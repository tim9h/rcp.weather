package dev.tim9h.rcp.weather.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;

public final class WeatherIconUtil {

	private static final Logger LOGGER = LogManager.getLogger(WeatherIconUtil.class);

	private static final Pattern SVG_PATH = Pattern.compile("<path[^>]* d=\"([^\"]+)\"");

	private WeatherIconUtil() {
		// Utility class
	}

	public static StackPane loadIcon(String resourcePath, Label colorSource, double size) {
		try (var input = WeatherIconUtil.class.getResourceAsStream(resourcePath)) {
			if (input == null) {
				throw new IOException("Missing weather icon resource: " + resourcePath);
			}
			var svg = new String(input.readAllBytes(), StandardCharsets.UTF_8);
			var matcher = SVG_PATH.matcher(svg);
			if (!matcher.find()) {
				throw new IOException("Invalid weather icon resource: " + resourcePath);
			}
			var path = new SVGPath();
			path.setContent(matcher.group(1));
			path.setScaleX(size / 24);
			path.setScaleY(size / 24);
			path.fillProperty().bind(colorSource.textFillProperty());

			var graphic = new StackPane(path);
			graphic.setMinSize(size, size);
			graphic.setPrefSize(size, size);
			graphic.setMaxSize(size, size);
			return graphic;
		} catch (IOException | RuntimeException e) {
			LOGGER.error(() -> "Unable to load weather icon: " + resourcePath, e);
			return null;
		}
	}
}
