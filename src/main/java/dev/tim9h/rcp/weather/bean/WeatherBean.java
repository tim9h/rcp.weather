package dev.tim9h.rcp.weather.bean;

import org.apache.commons.lang3.StringUtils;

public record WeatherBean(Double temperature, String description, Double precipitation, Double windSpeed,
		Integer humidity, String location) {

	public WeatherBean() {
		this(Double.valueOf(0), StringUtils.EMPTY, Double.valueOf(0), Double.valueOf(0), Integer.valueOf(0),
				StringUtils.EMPTY);
	}

}
