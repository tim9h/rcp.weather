package dev.tim9h.rcp.weather.util;

import javafx.util.converter.DefaultStringConverter;

public class WeatherGroupSymbolConverter extends DefaultStringConverter {

	@Override
	public String toString(String condition) {
		return switch (condition) {
		case "Clear" -> "☀";
		case "Clouds" -> "☁";
		case "Rain" -> "🌧";
		case "Snow" -> "❄";
		case "Thunderstorm" -> "🌩";
		case "Drizzle" -> "☔";
		case "Atmosphere" -> "🌫";
		default -> "";
		};
	}

}
