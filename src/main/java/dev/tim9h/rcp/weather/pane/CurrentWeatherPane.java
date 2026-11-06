package dev.tim9h.rcp.weather.pane;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Injector;

import dev.tim9h.rcp.logging.InjectLogger;
import dev.tim9h.rcp.settings.Settings;
import dev.tim9h.rcp.weather.WeatherView;
import dev.tim9h.rcp.weather.bean.WeatherBean;
import dev.tim9h.rcp.weather.util.WeatherSymbolConverter;
import dev.tim9h.rcp.weather.util.WeatherUtils;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.geometry.HPos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class CurrentWeatherPane extends GridPane {

	private static final String CSS_CLASS_SECONDARY = "secondary";

	private static final String CSS_CLASS_DETAIL = "detail-label";

	private Hyperlink temperature;

	private Label description;

	private Label precipitation;

	private Label humidity;

	private Label wind;

	private volatile String temporaryLocation;

	@Inject
	private Settings settings;

	@InjectLogger
	private Logger logger;

	@Inject
	public CurrentWeatherPane(Injector injector) {
		injector.injectMembers(this);

		getStyleClass().add("ccCard");
		var col = new ColumnConstraints();
		col.setPercentWidth(23.3);
		col.setHalignment(HPos.LEFT);
		var wide = new ColumnConstraints();
		wide.setPercentWidth(30);
		wide.setHalignment(HPos.LEFT);
		getColumnConstraints().addAll(wide, col, col, col);

		temperature = new Hyperlink();
		temperature.setOnAction(_ -> {
			var tempLocation = temporaryLocation;
			var location = tempLocation != null && !tempLocation.isBlank() ? tempLocation
					: settings.getString(WeatherView.SETTING_LOCATION);
			openUrl("https://www.google.com/search?q=weather+" + encodeValue(location));
		});

		temperature.getStyleClass().add("accent-label");
		description = new Label();
		var lblPrecipitation = new Label("☔ Precipitation");
		lblPrecipitation.getStyleClass().addAll(CSS_CLASS_SECONDARY, CSS_CLASS_DETAIL);
		precipitation = new Label();
		var lblHumidiy = new Label("⛆ Humidity");
		lblHumidiy.getStyleClass().addAll(CSS_CLASS_SECONDARY, CSS_CLASS_DETAIL);
		humidity = new Label();
		var lblWind = new Label("⇶ Wind");
		lblWind.getStyleClass().addAll(CSS_CLASS_SECONDARY, CSS_CLASS_DETAIL);
		wind = new Label();

		var symbol = new Label();
		symbol.getStyleClass().add(CSS_CLASS_SECONDARY);
		symbol.textProperty().bind(Bindings.createStringBinding(
				() -> WeatherSymbolConverter.getSymbol(description.getText()), description.textProperty()));
		var descriptionBox = new HBox(symbol, description);

		add(temperature, 0, 0);
		add(descriptionBox, 0, 1);

		add(lblPrecipitation, 1, 0);
		add(precipitation, 1, 1);

		add(lblHumidiy, 2, 0);
		add(humidity, 2, 1);

		add(lblWind, 3, 0);
		add(wind, 3, 1);
	}

	public void setTemporaryLocation(String temporaryLocation) {
		this.temporaryLocation = temporaryLocation;
	}

	private static String encodeValue(String value) {
		var encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
		if (encoded.startsWith("%EF%BB%BF")) {
			return encoded.substring(9); // remove BOM
		}
		return encoded;
	}

	private void openUrl(String url) {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			logger.warn(() -> "Desktop browsing is not supported");
			return;
		}
		try {
			Desktop.getDesktop().browse(new URI(url));
		} catch (URISyntaxException | IOException e) {
			logger.error(() -> "Unable to open url", e);
		}
	}

	public void update(WeatherBean weather, String units) {
		assert Platform.isFxApplicationThread();
		logger.info(() -> "Updating weather: " + weather);
		temperature.textProperty()
				.set(String.valueOf(weather.temperature().intValue()) + WeatherUtils.getUnitStringTemperature(units));
		description.textProperty().set(weather.description());
		precipitation.textProperty().set(weather.precipitation() + " mm");
		humidity.textProperty().set(weather.humidity() + "%");
		var windSpeed = "imperial".equals(units) ? weather.windSpeed() : weather.windSpeed() * 3.6; // km/h
		wind.textProperty().set(String.format("%.1f %s", windSpeed, WeatherUtils.getUnitStringWindSpeed(units)));
	}

}