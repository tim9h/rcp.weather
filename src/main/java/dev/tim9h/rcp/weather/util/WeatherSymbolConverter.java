package dev.tim9h.rcp.weather.util;

import java.util.Locale;

public class WeatherSymbolConverter {

	private WeatherSymbolConverter() {
		//
	}

	public static String getIconResource(String condition) {
		if (condition == null) {
			return null;
		}
		return switch (condition.toLowerCase(Locale.ROOT)) {
		case "clear", "clear sky" -> "/images/weather/clear.svg";
		case "clouds", "few clouds", "scattered clouds", "broken clouds", "overcast clouds" ->
			"/images/weather/cloud.svg";
		case "rain", "light rain", "moderate rain", "heavy intensity rain", "very heavy rain", "extreme rain",
				"freezing rain", "light intensity shower rain", "shower rain", "heavy intensity shower rain",
				"ragged shower rain" ->
			"/images/weather/rain.svg";
		case "snow", "light snow", "heavy snow", "sleet", "light shower sleet", "shower sleet", "light rain and snow",
				"rain and snow", "light shower snow", "shower snow", "heavy shower snow" ->
			"/images/weather/snow.svg";
		case "thunderstorm", "thunderstorm with light rain", "thunderstorm with rain", "thunderstorm with heavy rain",
				"light thunderstorm", "heavy thunderstorm", "ragged thunderstorm", "thunderstorm with light drizzle",
				"thunderstorm with drizzle", "thunderstorm with heavy drizzle" ->
			"/images/weather/thunderstorm.svg";
		case "drizzle", "light intensity drizzle", "heavy intensity drizzle", "light intensity drizzle rain",
				"drizzle rain", "heavy intensity drizzle rain", "shower rain and drizzle",
				"heavy shower rain and drizzle", "shower drizzle" ->
			"/images/weather/drizzle.svg";
		case "atmosphere", "mist", "smoke", "haze", "sand/dust whirls", "fog", "sand", "dust", "volcanic ash",
				"squalls", "tornado" ->
			"/images/weather/fog.svg";
		default -> null;
		};
	}

}
