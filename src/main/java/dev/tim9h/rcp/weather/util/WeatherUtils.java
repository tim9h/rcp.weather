package dev.tim9h.rcp.weather.util;

public class WeatherUtils {

	private WeatherUtils() {
		// hide implicit public constructor
	}

	public static String getUnitStringTemperature(String units) {
		return switch (units) {
		case "imperial" -> " °F";
		case "metric" -> " °C";
		default -> " °K";
		};
	}

	public static String getUnitStringWindSpeed(String units) {
		return switch (units) {
		case "imperial" -> " m/h";
		default -> " m/s";
		};
	}

}
