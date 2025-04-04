package dev.tim9h.rcp.weather;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.google.inject.Inject;

import dev.tim9h.rcp.spi.CCard;
import dev.tim9h.rcp.spi.CCardFactory;

public class WeatherViewFactory implements CCardFactory {

	static final String SETTING_UNITS = "weather.temperature.units";

	public static final String SETTING_LOCATION = "weather.location.name";

	public static final String SETTING_OPENWEATHERMAP_APIKEY = "weather.provider.openweathermap.apikey";
	
	public static final String SETTING_WEATHER_MODE = "weather.mode";
	
	public static final String SETTING_TEMPORARY_WEATHER_DURATION = "weather.temporary.duration";

	@Inject
	private WeatherView view;

	@Override
	public String getId() {
		return "weather";
	}

	@Override
	public CCard createCCard() {
		return view;
	}

	@Override
	public Map<String, String> getSettingsContributions() {
		Map<String, String> map = new HashMap<>();
		map.put(SETTING_UNITS, "metric");
		map.put(SETTING_LOCATION, "Kempten");
		map.put(SETTING_OPENWEATHERMAP_APIKEY, StringUtils.EMPTY);
		map.put(SETTING_WEATHER_MODE, "current");
		map.put(SETTING_TEMPORARY_WEATHER_DURATION, "7000");
		return map;
	}

}
