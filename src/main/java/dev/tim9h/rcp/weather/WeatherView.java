package dev.tim9h.rcp.weather;

import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Injector;

import dev.tim9h.rcp.event.EventManager;
import dev.tim9h.rcp.logging.InjectLogger;
import dev.tim9h.rcp.settings.Settings;
import dev.tim9h.rcp.spi.CCard;
import dev.tim9h.rcp.spi.Gravity;
import dev.tim9h.rcp.spi.Position;
import dev.tim9h.rcp.spi.TreeNode;
import dev.tim9h.rcp.weather.bean.Coordinate;
import dev.tim9h.rcp.weather.pane.CurrentWeatherPane;
import dev.tim9h.rcp.weather.pane.ForecastPane;
import dev.tim9h.rcp.weather.service.WeatherService;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class WeatherView implements CCard {

	private static final String CSS_CLASS_ACCENT_CARD = "accent-cccard";

	private static final String WEATHER = "weather";

	@InjectLogger
	private Logger logger;

	@Inject
	private EventManager eventManager;

	@Inject
	private Settings settings;

	@Inject
	private WeatherService weatherService;

	private Coordinate coord;

	private Pane wrapper;

	@Inject
	private ForecastPane forecastPane;

	@Inject
	private CurrentWeatherPane currentWeatherPane;

	@Override
	public String getName() {
		return "Weather";
	}

	@Inject
	public WeatherView(Injector injector) {
		injector.injectMembers(this);

		var timer = new Timer("weatherRefresher", true);
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				updateWeatherPanel();
			}
		}, 0, 300000);
	}

	private void updateWeatherPanel() {
		var units = settings.getString(WeatherViewFactory.SETTING_UNITS);
		var coordinate = getCoord();
		if (StringUtils.isNotBlank(units) && coordinate != null) {
			Platform.runLater(() -> {
				if (wrapper.getChildren().contains(forecastPane)) {
					wrapper.getChildren().remove(forecastPane);
					wrapper.getChildren().add(currentWeatherPane);
				}
			});

			var weather = weatherService.getCurrentWeather(coordinate.lat(), coordinate.lon(), units);
			if (currentWeatherPane != null && currentWeatherPane.getStyleClass().contains(CSS_CLASS_ACCENT_CARD)) {
				currentWeatherPane.getStyleClass().remove(CSS_CLASS_ACCENT_CARD);
			}
			if (currentWeatherPane != null) {
				currentWeatherPane.update(weather, units);
			} else {
				logger.error(() -> "Unable to update current weather");
			}

		} else {
			eventManager.echoAsync("Unable to refresh weather");
			logger.warn(() -> "Weather settings missing");
		}
	}

	@Override
	public Optional<Node> getNode() throws IOException {
		wrapper = new StackPane(currentWeatherPane);
		return Optional.of(wrapper);
	}

	@Override
	public Gravity getGravity() {
		return new Gravity(10, Position.TOP);
	}

	@Override
	public void initBus(EventManager em) {
		CCard.super.initBus(eventManager);
		em.listen(WEATHER, this::handleWeatherCommands);
	}

	private void handleWeatherCommands(Object[] data) {
		if (data == null) {
			eventManager.echo("Where?");
		} else if ("location".equals(data[0])) {
			if (data.length > 1) {
				var location = StringUtils.capitalize((String) data[1]);
				settings.persist(WeatherViewFactory.SETTING_LOCATION, location);
				eventManager.echo("Weather location set to", location);
				coord = null;
				updateWeatherPanel();
			} else {
				displayWeatherLocation();
			}
		} else if ("forecast".equals(data[0])) {
			displayWeatherForecast(data);
		} else {
			displayWeatherForLocation(StringUtils.join(data, "+"));
		}
	}

	private void displayWeatherForecast(Object[] data) {
		eventManager.showWaitingIndicator();
		if (data.length > 1) {
			var location = StringUtils.join(Arrays.copyOfRange(data, 1, data.length), "+");
			eventManager.showWaitingIndicator();
			CompletableFuture.supplyAsync(() -> weatherService.getCoordinate(location)).thenAccept(coordinate -> {
				if (coordinate != null) {
					if (forecastPane != null && !forecastPane.getStyleClass().contains(CSS_CLASS_ACCENT_CARD)) {
						forecastPane.getStyleClass().add(CSS_CLASS_ACCENT_CARD);
					}
					displayWeatherForecast(coordinate.lat(), coordinate.lon(), location);
				} else {
					eventManager.echoAsync("Location not found", location);
				}
			});
		} else {
			var coordinate = getCoord();
			if (coordinate != null) {
				var location = settings.getString(WeatherViewFactory.SETTING_LOCATION);
				displayWeatherForecast(coordinate.lat(), coordinate.lon(), location);
			}
		}
	}

	private void displayWeatherForecast(Double lat, Double lon, String location) {
		CompletableFuture.runAsync(() -> {
			var units = settings.getString(WeatherViewFactory.SETTING_UNITS);
			var forecast = weatherService.getForecast(lat, lon, units);
			forecastPane.update(forecast);
			eventManager.echoAsync("Showing weather forecast for", StringUtils.capitalize(location));
			Platform.runLater(() -> {
				if (wrapper.getChildren().contains(currentWeatherPane)) {
					wrapper.getChildren().remove(currentWeatherPane);
				}
				if (!wrapper.getChildren().contains(forecastPane)) {
					wrapper.getChildren().add(forecastPane);
				}
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						updateWeatherPanel();
						if (forecastPane != null && forecastPane.getStyleClass().contains(CSS_CLASS_ACCENT_CARD)) {
							forecastPane.getStyleClass().remove(CSS_CLASS_ACCENT_CARD);
						}
					}
				}, 5000);
			});
		});
	}

	private void displayWeatherLocation() {
		var location = settings.getString(WeatherViewFactory.SETTING_LOCATION);
		if (StringUtils.isNotBlank(location)) {
			eventManager.echoAsync("Showing weather for", location);
		} else {
			eventManager.echo("Unable to show weather location");
			logger.warn(() -> "Weather location missing");
		}
	}

	private void displayWeatherForLocation(String location) {
		var units = settings.getString(WeatherViewFactory.SETTING_UNITS);
		if (StringUtils.isNotBlank(units) && StringUtils.isNotBlank(location)) {
			eventManager.showWaitingIndicator();
			CompletableFuture.supplyAsync(() -> weatherService.getCoordinate(location)).thenAccept(coordinate -> {
				if (coordinate != null) {
					var weather = weatherService.getCurrentWeather(coordinate.lat(), coordinate.lon(), units);
					currentWeatherPane.getStyleClass().add(CSS_CLASS_ACCENT_CARD);
					eventManager.echoAsync("Showing weather in", StringUtils.capitalize(location));
					currentWeatherPane.update(weather, units);
					new Timer().schedule(new TimerTask() {
						@Override
						public void run() {
							updateWeatherPanel();
						}
					}, 5000);
				} else {
					eventManager.echoAsync("Location not found", location);
				}
			});
		} else {
			eventManager.echo("Units or location missing");
		}
	}

	@Override
	public Optional<TreeNode<String>> getModelessCommands() {
		var node = new TreeNode<>(WEATHER);
		node.add("location", "forecast");
		return Optional.of(node);
	}

	private Coordinate getCoord() {
		if (coord == null) {
			var location = settings.getString(WeatherViewFactory.SETTING_LOCATION);
			if (StringUtils.isNotBlank(location)) {
				coord = weatherService.getCoordinate(location);
			} else {
				logger.warn(() -> "Unable to update weather: location not set");
			}
		}
		return coord;
	}

}
