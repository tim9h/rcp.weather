package dev.tim9h.rcp.weather.bean.ows;

import javax.annotation.processing.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("jsonschema2pojo")
public class LocalNames {

	@SerializedName("de")
	@Expose
	private String de;

	@SerializedName("eo")
	@Expose
	private String eo;

	@SerializedName("es")
	@Expose
	private String es;

	@SerializedName("la")
	@Expose
	private String la;

	@SerializedName("ru")
	@Expose
	private String ru;

	@SerializedName("sr")
	@Expose
	private String sr;

	public String getDe() {
		return de;
	}

	public void setDe(String de) {
		this.de = de;
	}

	public String getEo() {
		return eo;
	}

	public void setEo(String eo) {
		this.eo = eo;
	}

	public String getEs() {
		return es;
	}

	public void setEs(String es) {
		this.es = es;
	}

	public String getLa() {
		return la;
	}

	public void setLa(String la) {
		this.la = la;
	}

	public String getRu() {
		return ru;
	}

	public void setRu(String ru) {
		this.ru = ru;
	}

	public String getSr() {
		return sr;
	}

	public void setSr(String sr) {
		this.sr = sr;
	}

}