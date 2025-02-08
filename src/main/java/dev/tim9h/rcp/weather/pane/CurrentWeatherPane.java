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

import dev.tim9h.rcp.controls.IconButton;
import dev.tim9h.rcp.event.CcEvent;
import dev.tim9h.rcp.event.EventManager;
import dev.tim9h.rcp.logging.InjectLogger;
import dev.tim9h.rcp.settings.Settings;
import dev.tim9h.rcp.weather.WeatherViewFactory;
import dev.tim9h.rcp.weather.bean.WeatherBean;
import dev.tim9h.rcp.weather.util.WeatherUtils;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class CurrentWeatherPane extends GridPane {

	private static final String CSS_CLASS_SECONDARY = "secondary";

	private Hyperlink temperature;

	private Label description;

	private Label precipitation;

	private Label humidity;

	private Label wind;

	@Inject
	private Settings settings;

	@InjectLogger
	private Logger logger;
	
	@Inject
	private IconButton btnSwap;
	
	@Inject
	private EventManager eventManager;
	
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
			var location = settings.getString(WeatherViewFactory.SETTING_LOCATION);
			openUrl("https://www.google.com/search?q=weather+%s", encodeValue(location));
		});
		temperature.getStyleClass().add("accent-label");
		description = new Label();
		var lblPrecipitation = new Label("Precipitation");
		lblPrecipitation.getStyleClass().add(CSS_CLASS_SECONDARY);
		precipitation = new Label();
		var lblHumidiy = new Label("Humidity");
		lblHumidiy.getStyleClass().add(CSS_CLASS_SECONDARY);
		humidity = new Label();
		var lblWind = new Label("Wind");
		lblWind.getStyleClass().add(CSS_CLASS_SECONDARY);
		wind = new Label();

		add(temperature, 0, 0);
		add(description, 0, 1);

		add(lblPrecipitation, 1, 0);
		add(precipitation, 1, 1);

		add(lblHumidiy, 2, 0);
		add(humidity, 2, 1);

		var windGrid = createSwapButtonWrapper(lblWind);
		add(windGrid, 3, 0, 1, 2);
	}

	private GridPane createSwapButtonWrapper(Label lblWind) {
		var windGrid = new GridPane();
        var windCC = new ColumnConstraints();
        windCC.setHgrow(Priority.ALWAYS); 
        var btnCC = new ColumnConstraints();
        btnCC.setPrefWidth(Region.USE_COMPUTED_SIZE);
        windGrid.getColumnConstraints().addAll(windCC, btnCC);
		windGrid.add(lblWind, 0, 0);
		windGrid.add(wind, 0, 1);
		btnSwap.setLabel('⇄');
		btnSwap.setOnAction(_ -> eventManager.post(new CcEvent("weather", "forecast")));
		windGrid.add(btnSwap, 1, 0, 1, 2);
		return windGrid;
	}

	private static String encodeValue(String value) {
		var encoded = URLEncoder.encode(value, StandardCharsets.UTF_8);
		if (encoded.startsWith("%EF%BB%BF")) {
			return encoded.substring(9); // remove BOM
		}
		return encoded;
	}

	private void openUrl(String url, Object... params) {
		try {
			Desktop.getDesktop().browse(new URI(String.format(url, params)));
		} catch (URISyntaxException | IOException e) {
			logger.error(() -> "Unable to open url", e);
		}
	}

	public void update(WeatherBean weather, String units) {
		logger.info(() -> "Updating weather: " + weather);
		Platform.runLater(() -> {
			temperature.textProperty().set(
					String.valueOf(weather.temperature().intValue()) + WeatherUtils.getUnitStringTemperature(units));
			description.textProperty().set(weather.description());
			precipitation.textProperty().set(weather.precipitation() + " mm");
			humidity.textProperty().set(weather.humidity() + "%");
			var windSpeed = units.equals("imperial") ? weather.windSpeed() : weather.windSpeed() * 3.6; // km/h
			wind.textProperty().set(String.format("%.1f %s", windSpeed, WeatherUtils.getUnitStringWindSpeed(units)));
		});
	}

}
