package dev.tim9h.rcp.weather.service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.inject.Inject;

import dev.tim9h.rcp.logging.InjectLogger;
import dev.tim9h.rcp.settings.Settings;
import dev.tim9h.rcp.weather.WeatherView;
import dev.tim9h.rcp.weather.bean.Coordinate;
import dev.tim9h.rcp.weather.bean.Forecast;
import dev.tim9h.rcp.weather.bean.WeatherBean;
import dev.tim9h.rcp.weather.bean.ows.ForecastBean;
import dev.tim9h.rcp.weather.bean.ows.Geocode;
import dev.tim9h.rcp.weather.bean.ows.OwsWeatherBean;
import dev.tim9h.rcp.weather.util.WeatherCollector;

public class OpenWeatherMapService implements WeatherService {

	private static final String API_BASE_URL = "https://api.openweathermap.org";

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

	@Inject
	Gson gson;

	@InjectLogger
	Logger logger;

	@Inject
	Settings settings;

	private final HttpClient httpClient;

	public OpenWeatherMapService() {
		httpClient = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).version(HttpClient.Version.HTTP_2).build();
	}

	@Override
	public WeatherBean getCurrentWeather(Double lat, Double lon, String units) {
		var apiKey = getApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			logger.warn(() -> "OpenWeatherMap API key is not configured");
			return null;
		}
		var uri = buildUri("/data/2.5/weather",
				Map.of("lat", String.valueOf(lat), "lon", String.valueOf(lon), "appid", apiKey, "units", units));
		var response = get(uri);
		if (response == null) {
			return null;
		}
		try {
			var owsBean = gson.fromJson(response, OwsWeatherBean.class);
			if (owsBean == null || owsBean.getMain() == null || owsBean.getWeather() == null
					|| owsBean.getWeather().isEmpty()) {
				logger.warn(() -> "OpenWeatherMap returned incomplete weather data");
				return null;
			}
			return new WeatherBean(owsBean.getMain().getTemp(),
					StringUtils.capitalize(owsBean.getWeather().get(0).getDescription()), getPrecipitation(owsBean),
					owsBean.getWind().getSpeed(), owsBean.getMain().getHumidity(), owsBean.getName());

		} catch (RuntimeException e) {
			logger.error(() -> "Unable to parse OpenWeatherMap weather response: " + e.getMessage());
			return null;
		}
	}

	@Override
	public Coordinate getCoordinate(String location) {
		var apiKey = getApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			logger.warn(() -> "OpenWeatherMap API key is not configured");
			return null;
		}
		if (location == null || location.isBlank()) {
			return null;
		}
		var uri = buildUri("/geo/1.0/direct", Map.of("q", location, "appid", apiKey));
		var response = get(uri);
		if (response == null) {
			return null;
		}
		try {
			var geocodes = gson.fromJson(response, Geocode[].class);
			if (geocodes == null || geocodes.length == 0) {
				logger.warn(() -> "No coordinates found for location: " + location);
				return null;
			}
			var geocode = geocodes[0];
			return new Coordinate(geocode.getLat(), geocode.getLon());
		} catch (RuntimeException e) {
			logger.error(() -> "Unable to parse OpenWeatherMap geocoding response: " + e.getMessage());
			return null;
		}
	}

	@Override
	public String getLocation(Double lat, Double lon) {
		var apiKey = getApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			logger.warn(() -> "OpenWeatherMap API key is not configured");
			return null;
		}
		var uri = buildUri("/geo/1.0/reverse",
				Map.of("lat", String.valueOf(lat), "lon", String.valueOf(lon), "limit", "1", "appid", apiKey));
		var response = get(uri);
		if (response == null) {
			return null;
		}
		try {
			var geocodes = gson.fromJson(response, Geocode[].class);
			if (geocodes == null || geocodes.length == 0) {
				logger.warn(() -> "No location found for coordinates: " + lat + ", " + lon);
				return null;
			}
			return geocodes[0].getName();
		} catch (RuntimeException e) {
			logger.error(() -> "Unable to parse OpenWeatherMap reverse geocoding response: " + e.getMessage());
			return null;
		}
	}

	@Override
	public Forecast getForecast(Double lat, Double lon, String units) {
		var apiKey = getApiKey();
		if (apiKey == null || apiKey.isBlank()) {
			logger.warn(() -> "OpenWeatherMap API key is not configured");
			return null;
		}
		var uri = buildUri("/data/2.5/forecast",
				Map.of("lat", String.valueOf(lat), "lon", String.valueOf(lon), "appid", apiKey, "units", units));
		var response = get(uri);
		if (response == null) {
			return null;
		}
		try {
			var forecastBean = gson.fromJson(response, ForecastBean.class);
			if (forecastBean == null || forecastBean.getList() == null || forecastBean.getCity() == null) {
				logger.warn(() -> "OpenWeatherMap returned incomplete forecast data");
				return null;
			}
			var list = forecastBean.getList().stream().collect(WeatherCollector.toMap());
			return new Forecast(forecastBean.getCity().getName(), list);
		} catch (RuntimeException e) {
			logger.error(() -> "Unable to parse OpenWeatherMap forecast response: " + e.getMessage());
			return null;
		}
	}

	private String getApiKey() {
		return settings.getString(WeatherView.SETTING_OPENWEATHERMAP_APIKEY);
	}

	private String get(URI uri) {
		logger.debug(() -> "Requesting OpenWeatherMap resource: " + uri.getPath());
		var request = HttpRequest.newBuilder().uri(uri).timeout(REQUEST_TIMEOUT).header("User-Agent", "WeatherPlugin")
				.header("Accept", "application/json").GET().build();
		try {
			var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
			var statusCode = response.statusCode();
			if (statusCode >= 200 && statusCode < 300) {
				return response.body();
			}
			logHttpError(uri, statusCode, response.body());
			return null;
		} catch (IOException e) {
			logger.error(() -> "Unable to fetch OpenWeatherMap data: " + e.getMessage());
			return null;
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			logger.warn(() -> "OpenWeatherMap request was interrupted");
			return null;
		}
	}

	private void logHttpError(URI uri, int statusCode, String body) {
		switch (statusCode) {
		case 401 -> logger.error(() -> "OpenWeatherMap authentication failed (HTTP 401)");
		case 404 -> logger.warn(() -> "OpenWeatherMap resource not found: " + uri.getPath());
		case 429 -> logger.warn(() -> "OpenWeatherMap rate limit exceeded (HTTP 429)");
		default -> {
			if (statusCode >= 500) {
				logger.error(() -> "OpenWeatherMap server error (HTTP " + statusCode + ")");
			} else {
				logger.warn(() -> "OpenWeatherMap request failed (HTTP " + statusCode + ")");
			}
		}
		}
	}

	private static URI buildUri(String path, Map<String, String> parameters) {
		var query = parameters.entrySet().stream().map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
				.collect(Collectors.joining("&"));
		return URI.create(API_BASE_URL + path + "?" + query);
	}

	private static String encode(String value) {
		return URLEncoder.encode(Objects.requireNonNullElse(value, ""), StandardCharsets.UTF_8);
	}

	private static Double getPrecipitation(OwsWeatherBean bean) {
		if (bean.getRain() != null) {
			var rain = bean.getRain();
			if (rain.get3h() != null) {
				return rain.get3h();
			}
			if (rain.get1h() != null) {
				return rain.get1h();
			}
		}
		if (bean.getSnow() != null) {
			var snow = bean.getSnow();
			if (snow.get3h() != null) {
				return snow.get3h();
			}
			if (snow.get1h() != null) {
				return snow.get1h();
			}
		}
		return 0.0;
	}
}