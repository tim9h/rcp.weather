package dev.tim9h.rcp.weather.util;

public class WeatherSymbolConverter {

	private WeatherSymbolConverter() {
		//
	}

	public static String getSymbol(String condition) {
		return switch (condition.toLowerCase()) {
		case "clear", "" -> "☀";
		case "clouds", "few clouds", "scattered clouds", "broken clouds", "overcast clouds" -> "☁";
		case "rain", "light rain", "moderate rain", "heavy intensity rain", "very heavy rain", "extreme rain",
				"freezing rain", "light intensity shower rain", "shower rain", "heavy intensity shower rain",
				"ragged shower rain" ->
			"🌧";
		case "snow", "light snow", "heavy snow", "sleet", "light shower sleet", "shower sleet",
				"light rain and snow", "rain and snow", "light shower snow", "shower snow", "heavy shower snow" ->
			"❄";
		case "thunderstorm", "thunderstorm with light rain", "thunderstorm with rain", "thunderstorm with heavy rain",
				"light thunderstorm", "heavy thunderstorm", "ragged thunderstorm",
				"thunderstorm with light drizzle", "thunderstorm with drizzle", "thunderstorm with heavy drizzle" ->
			"🌩";
		case "drizzle", "light intensity drizzle", "heavy intensity drizzle", "light intensity drizzle rain",
				"drizzle rain", "heavy intensity drizzle rain", "shower rain and drizzle",
				"heavy shower rain and drizzle", "shower drizzle" ->
			"☔";
		case "atmosphere", "mist", "smoke", "haze", "sand/dust whirls", "fog", "sand", "dust", "volcanic ash",
				"squalls", "tornado" ->
			"🌫";
		default -> "";
		};
	}

}
