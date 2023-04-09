package dev.tim9h.rcp.weather.bean;

import java.time.LocalDate;
import java.util.Map;

public record Forecast(String location, Map<LocalDate, DayWeatherBean> days) {

}
