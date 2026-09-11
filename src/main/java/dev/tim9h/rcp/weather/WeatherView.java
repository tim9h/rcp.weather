package dev.tim9h.rcp.weather;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;

import dev.tim9h.rcp.event.EventManager;
import dev.tim9h.rcp.logging.InjectLogger;
import dev.tim9h.rcp.settings.Settings;
import dev.tim9h.rcp.spi.CommandBuilder;
import dev.tim9h.rcp.spi.CommandNode;
import dev.tim9h.rcp.spi.Gravity;
import dev.tim9h.rcp.spi.Plugin;
import dev.tim9h.rcp.spi.Position;
import dev.tim9h.rcp.weather.bean.Coordinate;
import dev.tim9h.rcp.weather.bean.Forecast;
import dev.tim9h.rcp.weather.bean.WeatherBean;
import dev.tim9h.rcp.weather.pane.CurrentWeatherPane;
import dev.tim9h.rcp.weather.pane.ForecastPane;
import dev.tim9h.rcp.weather.service.WeatherService;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;

public class WeatherView implements Plugin {

	static final String SETTING_UNITS = "weather.temperature.units";

	public static final String SETTING_LOCATION = "weather.location.name";

	public static final String SETTING_OPENWEATHERMAP_APIKEY = "weather.provider.openweathermap.apikey";

	public static final String SETTING_WEATHER_MODE = "weather.mode";

	public static final String SETTING_TEMPORARY_WEATHER_DURATION = "weather.temporary.duration";

	private static final String SHOWING_WEATHER_FORECAST_FOR = "Showing weather forecast for";

	private static final String CURRENT = "current";

	private static final String FORECAST = "forecast";

	private static final String SHOWING_WEATHER_FOR = "Showing weather for";

	private static final String CSS_CLASS_ACCENT_CARD = "accent-cccard";

	private static final String WEATHER = "weather";

	private static final long REFRESH_INTERVAL_MINUTES = 5;

	@InjectLogger
	private Logger logger;

	@Inject
	private EventManager eventManager;

	@Inject
	private Settings settings;

	@Inject
	private WeatherService weatherService;

	@Inject
	private ForecastPane forecastPane;

	@Inject
	private CurrentWeatherPane currentWeatherPane;

	private final AtomicBoolean refreshInProgress = new AtomicBoolean();

	private final AtomicLong requestGeneration = new AtomicLong();

	private volatile Coordinate coord;

	private volatile String tempLocation;

	private Pane wrapper;

	private ScheduledExecutorService scheduler;

	private ExecutorService weatherExecutor;

	private ScheduledFuture<?> temporaryWeatherTask;

	private volatile boolean shuttingDown;

	@Override
	public String getName() {
		return "Weather";
	}

	@Override
	public void init() {
		shuttingDown = false;
		scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
			var thread = new Thread(r, "weather-scheduler");
			thread.setDaemon(true);
			return thread;
		});
		weatherExecutor = Executors.newSingleThreadExecutor(r -> {
			var thread = new Thread(r, "weather-worker");
			thread.setDaemon(true);
			return thread;
		});
		scheduler.scheduleAtFixedRate(this::updateWeatherData, 0, REFRESH_INTERVAL_MINUTES, TimeUnit.MINUTES);
	}

	@Override
	public Optional<Node> getNode() throws IOException {
		wrapper = new StackPane();
		var mode = settings.getString(SETTING_WEATHER_MODE);
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
	public void onShutdown() {
		shuttingDown = true;
		requestGeneration.incrementAndGet();
		if (temporaryWeatherTask != null) {
			temporaryWeatherTask.cancel(false);
			temporaryWeatherTask = null;
		}
		if (scheduler != null) {
			scheduler.shutdownNow();
			scheduler = null;
		}
		if (weatherExecutor != null) {
			weatherExecutor.shutdownNow();
			weatherExecutor = null;
		}
	}

	private void displayWeatherLocation() {
		var location = settings.getString(SETTING_LOCATION);

		if (StringUtils.isNotBlank(location)) {
			eventManager.echo("Weather location set to", location);
		} else {
			eventManager.echo("Unable to show weather location");
			logger.warn(() -> "Weather location missing");
		}
	}

	@Override
	public Optional<CommandNode> getCommands() {
		return new CommandBuilder().command(WEATHER, _ -> eventManager.echo("Where?")).argumentAction(arg -> {
			var location = StringUtils.capitalize(arg);
			eventManager.echo(settings.getString(SETTING_WEATHER_MODE).equals(FORECAST) ? SHOWING_WEATHER_FORECAST_FOR
					: SHOWING_WEATHER_FOR, location);
			updateWeatherDataTemporary(location);
		}).child(FORECAST, _ -> {
			settings.persist(SETTING_WEATHER_MODE, FORECAST);
			var location = StringUtils.defaultIfBlank(tempLocation, settings.getString(SETTING_LOCATION));
			eventManager.echo(SHOWING_WEATHER_FORECAST_FOR, StringUtils.capitalize(location));
			showWeatherForecastPanel();
		}).up().child(CURRENT, _ -> {
			settings.persist(SETTING_WEATHER_MODE, CURRENT);
			var location = StringUtils.defaultIfBlank(tempLocation, settings.getString(SETTING_LOCATION));
			eventManager.echo(SHOWING_WEATHER_FOR, StringUtils.capitalize(location));
			showCurrentWeatherPanel();
		}).up().child("location", _ -> displayWeatherLocation()).argumentAction(arg -> {
			var location = StringUtils.capitalize(arg);
			settings.persist(SETTING_LOCATION, location);
			eventManager.echo("Weather location set to", location);
			coord = null;
			updateWeatherData();
		}).build();
	}

	private Coordinate getCoord() {
		var cachedCoordinate = coord;
		if (cachedCoordinate != null) {
			return cachedCoordinate;
		}
		var location = settings.getString(SETTING_LOCATION);
		if (StringUtils.isBlank(location)) {
			logger.warn(() -> "Unable to update weather: location not set");
			return null;
		}
		var newCoordinate = weatherService.getCoordinate(location);
		if (newCoordinate != null) {
			coord = newCoordinate;
		}
		return newCoordinate;
	}

	private void showCurrentWeatherPanel() {
		wrapper.getChildren().setAll(currentWeatherPane);
	}

	private void showWeatherForecastPanel() {
		wrapper.getChildren().setAll(forecastPane);
	}

	private void updateWeatherData() {
		if (shuttingDown) {
			return;
		}
		if (!refreshInProgress.compareAndSet(false, true)) {
			logger.debug(() -> "Weather refresh already in progress");
			return;
		}
		var generation = requestGeneration.get();
		CompletableFuture.supplyAsync(this::loadWeatherData, weatherExecutor).thenAccept(data -> {
			if (data == null || shuttingDown || generation != requestGeneration.get()) {
				return;
			}
			Platform.runLater(() -> {
				if (shuttingDown || generation != requestGeneration.get()) {
					return;
				}
				if (data.currentWeather() != null) {
					currentWeatherPane.update(data.currentWeather(), data.units());
				}
				if (data.forecast() != null) {
					forecastPane.update(data.forecast());
				}
			});
		}).whenComplete((_, error) -> {
			refreshInProgress.set(false);
			if (error != null && !shuttingDown) {
				logger.error("Unable to update weather", error);
			}
		});
	}

	private WeatherData loadWeatherData() {
		var units = settings.getString(SETTING_UNITS);
		if (StringUtils.isBlank(units)) {
			logger.warn(() -> "Unable to update weather: units missing");
			return null;
		}
		var coordinate = getCoord();
		if (coordinate == null) {
			logger.warn(() -> "Unable to update weather: coordinate unavailable");
			return null;
		}
		var weather = weatherService.getCurrentWeather(coordinate.lat(), coordinate.lon(), units);
		var forecast = weatherService.getForecast(coordinate.lat(), coordinate.lon(), units);
		return new WeatherData(weather, forecast, units);
	}

	private void updateWeatherDataTemporary(String temporaryLocation) {
		if (shuttingDown) {
			return;
		}
		var units = settings.getString(SETTING_UNITS);
		if (StringUtils.isBlank(units) || StringUtils.isBlank(temporaryLocation)) {
			eventManager.echo("Units or location missing");
			return;
		}
		var generation = requestGeneration.incrementAndGet();
		tempLocation = temporaryLocation;
		cancelTemporaryWeatherTask();
		CompletableFuture.supplyAsync(() -> loadTemporaryWeatherData(temporaryLocation, units), weatherExecutor)
				.thenAccept(data -> {
					if (data == null || shuttingDown || generation != requestGeneration.get()) {
						return;
					}
					Platform.runLater(() -> {
						if (shuttingDown || generation != requestGeneration.get()) {
							return;
						}
						setTemporaryHighlight(true);
						var mode = settings.getString(SETTING_WEATHER_MODE);
						if (FORECAST.equals(mode)) {
							eventManager.echo(SHOWING_WEATHER_FORECAST_FOR, StringUtils.capitalize(temporaryLocation));
						} else {
							eventManager.echo(SHOWING_WEATHER_FOR, StringUtils.capitalize(temporaryLocation));
						}
						if (data.currentWeather() != null) {
							currentWeatherPane.update(data.currentWeather(), data.units());
						}
						if (data.forecast() != null) {
							forecastPane.update(data.forecast());
						}
					});
					scheduleTemporaryWeatherExpiration(generation);
				}).whenComplete((_, error) -> {
					if (error != null && !shuttingDown && generation == requestGeneration.get()) {
						logger.error("Unable to update temporary weather", error);
						Platform.runLater(() -> {
							if (!shuttingDown && generation == requestGeneration.get()) {
								tempLocation = null;
								eventManager.echo("Unable to refresh temporary weather");
							}
						});
					}
				});
	}

	private WeatherData loadTemporaryWeatherData(String temporaryLocation, String units) {
		var coordinate = weatherService.getCoordinate(temporaryLocation);
		if (coordinate == null) {
			return null;
		}
		var weather = weatherService.getCurrentWeather(coordinate.lat(), coordinate.lon(), units);
		var forecast = weatherService.getForecast(coordinate.lat(), coordinate.lon(), units);
		return new WeatherData(weather, forecast, units);
	}

	private void scheduleTemporaryWeatherExpiration(long generation) {
		var duration = settings.getInt(SETTING_TEMPORARY_WEATHER_DURATION);
		if (duration < 0) {
			duration = 0;
		}
		var currentScheduler = scheduler;
		if (currentScheduler == null || shuttingDown) {
			return;
		}
		temporaryWeatherTask = currentScheduler.schedule(() -> {
			if (shuttingDown || generation != requestGeneration.get()) {
				return;
			}
			Platform.runLater(() -> {
				if (shuttingDown || generation != requestGeneration.get()) {
					return;
				}
				setTemporaryHighlight(false);
				tempLocation = null;
			});
			updateWeatherData();
		}, duration, TimeUnit.MILLISECONDS);
	}

	private void cancelTemporaryWeatherTask() {
		var task = temporaryWeatherTask;
		if (task != null) {
			task.cancel(false);
			temporaryWeatherTask = null;
		}
	}

	private void setTemporaryHighlight(boolean enabled) {
		if (!Platform.isFxApplicationThread()) {
			throw new IllegalStateException("setTemporaryHighlight must be called on the JavaFX Application Thread");
		}
		if (enabled) {
			if (!currentWeatherPane.getStyleClass().contains(CSS_CLASS_ACCENT_CARD)) {
				currentWeatherPane.getStyleClass().add(CSS_CLASS_ACCENT_CARD);
			}
			if (!forecastPane.getStyleClass().contains(CSS_CLASS_ACCENT_CARD)) {
				forecastPane.getStyleClass().add(CSS_CLASS_ACCENT_CARD);
			}
		} else {
			currentWeatherPane.getStyleClass().remove(CSS_CLASS_ACCENT_CARD);
			forecastPane.getStyleClass().remove(CSS_CLASS_ACCENT_CARD);
			eventManager.clearAsync();
		}
	}

	@Override
	public Map<String, String> getSettingsContributions() {
		Map<String, String> map = new HashMap<>();

		map.put(SETTING_UNITS, "metric");
		map.put(SETTING_LOCATION, "Kempten");
		map.put(SETTING_OPENWEATHERMAP_APIKEY, StringUtils.EMPTY);
		map.put(SETTING_WEATHER_MODE, CURRENT);
		map.put(SETTING_TEMPORARY_WEATHER_DURATION, "7000");

		return map;
	}

	private record WeatherData(WeatherBean currentWeather, Forecast forecast, String units) {
	}

}