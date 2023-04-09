module rcp.weather {
	exports dev.tim9h.rcp.weather;
	exports dev.tim9h.rcp.weather.bean;

	requires transitive rcp.api;
	requires transitive com.google.guice;
	requires transitive javafx.controls;
	requires org.apache.logging.log4j;
	requires org.apache.commons.lang3;
	requires com.google.gson;
	requires java.desktop;
	requires java.compiler;
	requires javafx.graphics;
}