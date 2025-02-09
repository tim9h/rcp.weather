package dev.tim9h.rcp.weather;

import java.io.IOException;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;

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
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class WeatherView implements CCard {

	private static final String SHOWING_WEATHER_FORECAST_FOR = "Showing weather forecast for";

	private static final String CURRENT = "current";

	private static final String FORECAST = "forecast";

	private static final String SHOWING_WEATHER_FOR = "Showing weather for";

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

	private String tempLocation;

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
				updateWeatherData();
			}
		}, 0, 300000);
	}

	@Override
	public Optional<Node> getNode() throws IOException {
		wrapper = new StackPane();
		var mode = settings.getString(WeatherViewFactory.SETTING_WEATHER_MODE);
		if (FORECAST.equals(mode)) {
			showWeatherForecastPanel();
		} else {
			showCurrentWeatherPanel();
		}
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
				// update weather location
				var location = StringUtils.capitalize((String) data[1]);
				settings.persist(WeatherViewFactory.SETTING_LOCATION, location);
				eventManager.echo("Weather location set to", location);
				coord = null;
				updateWeatherData();
			} else {
				displayWeatherLocation();
			}
		} else if (FORECAST.equals(data[0])) {
			settings.persist(WeatherViewFactory.SETTING_WEATHER_MODE, FORECAST);
			var location = StringUtils.defaultIfBlank(tempLocation,
					settings.getString(WeatherViewFactory.SETTING_LOCATION));
			eventManager.echo(SHOWING_WEATHER_FORECAST_FOR, StringUtils.capitalize(location));
			showWeatherForecastPanel();

		} else if (CURRENT.equals(data[0])) {
			settings.persist(WeatherViewFactory.SETTING_WEATHER_MODE, CURRENT);
			var location = StringUtils.defaultIfBlank(tempLocation,
					settings.getString(WeatherViewFactory.SETTING_LOCATION));
			eventManager.echo(SHOWING_WEATHER_FOR, StringUtils.capitalize(location));
			showCurrentWeatherPanel();
		} else {
			// display weather for temporary location
			var location = (String) data[0];
			if (settings.getString(WeatherViewFactory.SETTING_WEATHER_MODE).equals(FORECAST)) {
				eventManager.echoAsync(SHOWING_WEATHER_FORECAST_FOR, location);
			} else {
				eventManager.echo(SHOWING_WEATHER_FOR, location);
			}
			updateWeatherDataTemporary(location);
		}
	}

	private void displayWeatherLocation() {
		var location = settings.getString(WeatherViewFactory.SETTING_LOCATION);
		if (StringUtils.isNotBlank(location)) {
			eventManager.echoAsync("Weather location set to", location);
		} else {
			eventManager.echo("Unable to show weather location");
			logger.warn(() -> "Weather location missing");
		}
	}

	@Override
	public Optional<TreeNode<String>> getModelessCommands() {
		var node = new TreeNode<>(WEATHER);
		node.add("location", FORECAST, CURRENT);
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

	public void foo(String test) {
		logger.debug(() -> test);
	}

	private void showCurrentWeatherPanel() {
		if (wrapper.getChildren().contains(forecastPane)) {
			wrapper.getChildren().remove(forecastPane);
		}
		wrapper.getChildren().add(currentWeatherPane);
	}

	private void showWeatherForecastPanel() {
		if (wrapper.getChildren().contains(currentWeatherPane)) {
			wrapper.getChildren().remove(currentWeatherPane);
		}
		wrapper.getChildren().add(forecastPane);
	}

	private void updateWeatherData() {
		var units = settings.getString(WeatherViewFactory.SETTING_UNITS);
		var coordinate = getCoord();
		if (StringUtils.isNotBlank(units) && coordinate != null) {
			var weather = weatherService.getCurrentWeather(coordinate.lat(), coordinate.lon(), units);
			currentWeatherPane.update(weather, units);
			var forecast = weatherService.getForecast(coordinate.lat(), coordinate.lon(), units);
			forecastPane.update(forecast);
		} else {
			eventManager.echoAsync("Unable to refresh weather");
			logger.warn(() -> "Weather settings missing");
		}
	}

	private void updateWeatherDataTemporary(String temporaryLocation) {
		tempLocation = temporaryLocation;
		var units = settings.getString(WeatherViewFactory.SETTING_UNITS);
		if (StringUtils.isNotBlank(units) && StringUtils.isNotBlank(temporaryLocation)) {
			var coordinate = weatherService.getCoordinate(temporaryLocation);
			if (coordinate != null) {
				setTemporaryHighlight(true);
				var mode = settings.getString(WeatherViewFactory.SETTING_WEATHER_MODE);
				if (FORECAST.equals(mode)) {
					eventManager.echoAsync(SHOWING_WEATHER_FORECAST_FOR, StringUtils.capitalize(temporaryLocation));
				} else {
					eventManager.echoAsync(SHOWING_WEATHER_FOR, StringUtils.capitalize(temporaryLocation));
				}
				var weather = weatherService.getCurrentWeather(coordinate.lat(), coordinate.lon(), units);
				currentWeatherPane.update(weather, units);
				var forecast = weatherService.getForecast(coordinate.lat(), coordinate.lon(), units);
				forecastPane.update(forecast);

				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						updateWeatherData();
						setTemporaryHighlight(false);
						tempLocation = null;
					}
				}, settings.getInt(WeatherViewFactory.SETTING_TEMPORARY_WEATHER_DURATION));
			} else {
				eventManager.echoAsync("Location not found", temporaryLocation);
			}
		} else {
			eventManager.echo("Units or location missing");
		}
	}

	private void setTemporaryHighlight(boolean enabled) {
		if (enabled) {
			if (!currentWeatherPane.getStyleClass().contains(CSS_CLASS_ACCENT_CARD)) {
				currentWeatherPane.getStyleClass().add(CSS_CLASS_ACCENT_CARD);
			}
			if (!forecastPane.getStyleClass().contains(CSS_CLASS_ACCENT_CARD)) {
				forecastPane.getStyleClass().add(CSS_CLASS_ACCENT_CARD);
			}
		} else {
			if (currentWeatherPane.getStyleClass().contains(CSS_CLASS_ACCENT_CARD)) {
				currentWeatherPane.getStyleClass().remove(CSS_CLASS_ACCENT_CARD);
			}
			if (forecastPane.getStyleClass().contains(CSS_CLASS_ACCENT_CARD)) {
				forecastPane.getStyleClass().remove(CSS_CLASS_ACCENT_CARD);
			}
			eventManager.clearAsync();
		}
	}

}
