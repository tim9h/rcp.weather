package dev.tim9h.rcp.weather.bean.ows;

import javax.annotation.processing.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class Snow {

	@SerializedName("1h")
	@Expose
	private Double _1h;

	@SerializedName("3h")
	@Expose
	private Double _3h;

	public Double get1h() {
		return _1h;
	}

	public void set1h(Double _1h) {
		this._1h = _1h;
	}

	public Double get3h() {
		return _3h;
	}

	public void set3h(Double _3h) {
		this._3h = _3h;
	}

}