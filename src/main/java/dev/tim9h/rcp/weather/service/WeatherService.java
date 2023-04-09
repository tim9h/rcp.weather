package dev.tim9h.rcp.weather.service;

import com.google.inject.ImplementedBy;

import dev.tim9h.rcp.weather.bean.Coordinate;
import dev.tim9h.rcp.weather.bean.Forecast;
import dev.tim9h.rcp.weather.bean.WeatherBean;

@ImplementedBy(OpenWeatherMapService.class)
public interface WeatherService {

	public WeatherBean getCurrentWeather(Double lat, Double lon, String units);

	public Coordinate getCoordinate(String location);

	public String getLocation(Double lat, Double lon);

	public Forecast getForecast(Double lat, Double lon, String units);

}
