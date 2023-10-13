package dev.tim9h.rcp.weather.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.inject.Inject;

import dev.tim9h.rcp.logging.InjectLogger;
import dev.tim9h.rcp.settings.Settings;
import dev.tim9h.rcp.weather.WeatherViewFactory;
import dev.tim9h.rcp.weather.bean.Coordinate;
import dev.tim9h.rcp.weather.bean.Forecast;
import dev.tim9h.rcp.weather.bean.WeatherBean;
import dev.tim9h.rcp.weather.bean.ows.ForecastBean;
import dev.tim9h.rcp.weather.bean.ows.Geocode;
import dev.tim9h.rcp.weather.bean.ows.LocationName;
import dev.tim9h.rcp.weather.bean.ows.OwsWeatherBean;
import dev.tim9h.rcp.weather.bean.ows.WeatherCollector;

public class OpenWeatherMapService implements WeatherService {

	private static final String UNABLE_TO_FETCH_WEATHER_DATA = "Unable to fetch weather data: ";

	private static final String USER_AGENT = "User-Agent";

	private static final String USER_AGENT_VALUE = "Mozilla/5.0";

	private static final String METHOD_GET = "GET";

	@Inject
	private Gson gson;

	@InjectLogger
	private Logger logger;

	@Inject
	private Settings settings;

	@Override
	public WeatherBean getCurrentWeather(Double lat, Double lon, String units) {
		var apiKey = settings.getString(WeatherViewFactory.SETTING_OPENWEATHERMAP_APIKEY);
		try (var in = new BufferedReader(new InputStreamReader(getConnection(
				String.format("https://api.openweathermap.org/data/2.5/weather?lat=%s&lon=%s&appid=%s&units=%s", lat,
						lon, apiKey, units))
				.getInputStream()))) {
			var owsBean = gson.fromJson(in, OwsWeatherBean.class);
			return new WeatherBean(owsBean.getMain().getTemp(),
					StringUtils.capitalize(owsBean.getWeather().get(0).getDescription()), getPrecipitation(owsBean),
					owsBean.getWind().getSpeed(), owsBean.getMain().getHumidity(), owsBean.getName());
		} catch (IOException e) {
			logger.error(() -> UNABLE_TO_FETCH_WEATHER_DATA + e.getMessage());
			return null;
		}
	}

	private static Double getPrecipitation(OwsWeatherBean bean) {
		if (bean.getSnow() != null) {
			return bean.getSnow().get1h();
		} else if (bean.getRain() != null) {
			return bean.getRain().get1h();
		}
		return Double.valueOf(0);
	}

	private HttpsURLConnection getConnection(String urlString) throws IOException {
		logger.debug(() -> urlString);
		var url = URI.create(urlString).toURL();
		var con = (HttpsURLConnection) url.openConnection();
		con.setRequestMethod(METHOD_GET);
		con.setRequestProperty(USER_AGENT, USER_AGENT_VALUE);
		return con;
	}

	@Override
	public Coordinate getCoordinate(String location) {
		var apiKey = settings.getString(WeatherViewFactory.SETTING_OPENWEATHERMAP_APIKEY);
		try (var in = new BufferedReader(new InputStreamReader(getConnection(
				String.format("https://api.openweathermap.org/geo/1.0/direct?q=%s&appid=%s", location, apiKey))
				.getInputStream()))) {
			var owsBean = gson.fromJson(in, Geocode[].class);
			if (owsBean != null && owsBean.length > 0) {
				return new Coordinate(owsBean[0].getLat(), owsBean[0].getLon());
			} else {
				logger.warn(() -> "No geocode found for " + location);
				return null;
			}
		} catch (IOException e) {
			logger.error(() -> "Unable to fetch geocode: " + e.getMessage());
			return null;
		}
	}

	@Override
	public String getLocation(Double lat, Double lon) {
		var apiKey = settings.getString(WeatherViewFactory.SETTING_OPENWEATHERMAP_APIKEY);
		try (var in = new BufferedReader(new InputStreamReader(getConnection(String.format(
				"https://api.openweathermap.org/geo/1.0/reverse?lat=%f&lon=%f&limit=1&appid=%s", lat, lon, apiKey))
				.getInputStream()))) {
			var owsBeans = gson.fromJson(in, LocationName[].class);
			return (owsBeans != null && owsBeans.length > 0) ? owsBeans[0].getName() : null;
		} catch (IOException e) {
			logger.error(() -> UNABLE_TO_FETCH_WEATHER_DATA + e.getMessage());
			return null;
		}
	}

	@Override
	public Forecast getForecast(Double lat, Double lon, String units) {
		var apiKey = settings.getString(WeatherViewFactory.SETTING_OPENWEATHERMAP_APIKEY);
		try (var in = new BufferedReader(new InputStreamReader(getConnection(
				String.format("https://api.openweathermap.org/data/2.5/forecast?lat=%f&lon=%f&appid=%s&units=%s", lat,
						lon, apiKey, units))
				.getInputStream()))) {
			var forecastBean = gson.fromJson(in, ForecastBean.class);
			var list = forecastBean.getList().stream().collect(WeatherCollector.toMap());
			return new Forecast(forecastBean.getCity().getName(), list);
		} catch (IOException e) {
			logger.error(() -> UNABLE_TO_FETCH_WEATHER_DATA + e.getMessage());
			return null;
		}
	}

}
