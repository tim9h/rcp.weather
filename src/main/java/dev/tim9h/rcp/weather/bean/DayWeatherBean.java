package dev.tim9h.rcp.weather.bean;

import java.util.HashSet;
import java.util.Set;

public class DayWeatherBean {

	private Double tempMin;

	private Double tempMax;

	private Set<String> descriptions;

	private String condition;

	public DayWeatherBean(Double temp, String description) {
		super();
		this.tempMin = temp;
		this.tempMax = temp;
		getDescriptions().add(description);
	}

	public DayWeatherBean(Double temp, String description, String condition) {
		super();
		this.tempMin = temp;
		this.tempMax = temp;
		this.condition = condition;
		getDescriptions().add(description);
	}

	public DayWeatherBean(Double tempMin, Double tempMax, Set<String> descriptions) {
		super();
		this.tempMin = tempMin;
		this.tempMax = tempMax;
		this.descriptions = descriptions;
	}

	public Double getTempMin() {
		return tempMin;
	}

	public void setTempMin(Double tempMin) {
		this.tempMin = tempMin;
	}

	public Double getTempMax() {
		return tempMax;
	}

	public void setTempMax(Double tempMax) {
		this.tempMax = tempMax;
	}

	public Set<String> getDescriptions() {
		if (descriptions == null) {
			descriptions = new HashSet<>();
		}
		return descriptions;
	}

	public void setDescriptions(Set<String> descriptions) {
		this.descriptions = descriptions;
	}

	public void addDescrption(String description) {
		getDescriptions().add(description);
	}

	public String getCondition() {
		return condition;
	}

	public void setCondition(String condition) {
		this.condition = condition;
	}

	@Override
	public String toString() {
		return "DayWeatherBean [tempMin=" + tempMin + ", tempMax=" + tempMax + ", descriptions=" + descriptions
				+ ", condition=" + condition + "]";
	}

}
