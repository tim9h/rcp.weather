package dev.tim9h.rcp.weather.util;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

import dev.tim9h.rcp.weather.bean.DayWeatherBean;
import dev.tim9h.rcp.weather.bean.ows.OwsWeatherBean;

public class WeatherCollector
		implements Collector<OwsWeatherBean, Map<LocalDate, DayWeatherBean>, Map<LocalDate, DayWeatherBean>> {

	enum Condition {
		//@formatter:off
		THUNDERSTORM("Thunderstorm", 6), 
		SNOW("Snow", 5),
		RAIN("Rain", 4),
		DRIZZLE("Drizzle", 3),
		CLOUDS("Clouds", 2),
		ATMOSPHERE("Atmosphere", 1), 
		CLEAR("Clear", 0);
		//@formatter:on

		private Condition(String label, int value) {
			this.value = value;
			this.label = label;
		}

		public int getValue() {
			return value;
		}

		public String getLabel() {
			return label;
		}

		public final String label;

		public final int value;
	}

	public static WeatherCollector toMap() {
		return new WeatherCollector();
	}

	@Override
	public Supplier<Map<LocalDate, DayWeatherBean>> supplier() {
		return TreeMap::new;
	}

	@Override
	public BiConsumer<Map<LocalDate, DayWeatherBean>, OwsWeatherBean> accumulator() {
		return (map, bean) -> {
			var date = LocalDate.ofInstant(Instant.ofEpochSecond(bean.getDt().longValue()),
					TimeZone.getDefault().toZoneId());
			if (map.containsKey(date)) {
				var entry = map.get(date);
				if (entry.getTempMin().doubleValue() > bean.getMain().getTemp().doubleValue()) {
					entry.setTempMin(bean.getMain().getTemp());
				}
				if (entry.getTempMax().doubleValue() < bean.getMain().getTemp().doubleValue()) {
					entry.setTempMax(bean.getMain().getTemp());
				}
				entry.addDescrption(bean.getWeather().get(0).getDescription());
				accumulateWeatherCondition(bean, entry);
			} else {
				if (map.size() < 5) {
					map.put(date, new DayWeatherBean(bean.getMain().getTemp(),
							bean.getWeather().get(0).getDescription(), bean.getWeather().get(0).getMain()));
				}
			}
		};
	}

	private static void accumulateWeatherCondition(OwsWeatherBean bean, DayWeatherBean entry) {
		var main = bean.getWeather().get(0).getMain();
		if (entry.getCondition() == null) {
			entry.setCondition(Condition.valueOf(main.toUpperCase()).getLabel());
		} else {
			var condition0 = Condition.valueOf(entry.getCondition().toUpperCase());
			var condition1 = Condition.valueOf(main.toUpperCase());
			if (condition0.getValue() > condition1.getValue()) {
				entry.setCondition(condition0.getLabel());
			}
		}
	}

	@Override
	public BinaryOperator<Map<LocalDate, DayWeatherBean>> combiner() {
		return (map0, map1) -> {
			map0.putAll(map1);
			return map0;
		};
	}

	@Override
	public Function<Map<LocalDate, DayWeatherBean>, Map<LocalDate, DayWeatherBean>> finisher() {
		return map -> map;
	}

	@Override
	public Set<Characteristics> characteristics() {
		return Set.of(Characteristics.UNORDERED);
	}

}
