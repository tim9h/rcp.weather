package dev.tim9h.rcp.weather.pane;

import dev.tim9h.rcp.weather.util.WeatherSymbolConverter;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ForecastDayPane {

	private static final String CSS_CLASS_SECONDARY = "secondary";

	private static final String CSS_CLASS_ACCENT = "accent-label";

	private Label weekday;

	private Label tempMin;

	private Label tempMax;

	private Label condition;

	private HBox top;

	private HBox bottom;

	public ForecastDayPane() {
		weekday = new Label();
		weekday.getStyleClass().add(CSS_CLASS_SECONDARY);
		tempMin = new Label();
		tempMax = new Label();
		tempMax.getStyleClass().add(CSS_CLASS_ACCENT);
		condition = new Label();
		top = new HBox(weekday, tempMax, tempMin);
		top.setSpacing(0.0);
		var symbol = new Label();
		symbol.textProperty().bind(Bindings.createStringBinding(
				() -> WeatherSymbolConverter.getSymbol(condition.getText()), condition.textProperty()));
		symbol.getStyleClass().add(CSS_CLASS_SECONDARY);
		bottom = new HBox(symbol, condition);
		HBox.setMargin(top, new Insets(0));
	}

	public Node getTop() {
		return top;
	}

	public Node getBottom() {
		return bottom;
	}

	public Label getWeekday() {
		return weekday;
	}

	public Label getTempMin() {
		return tempMin;
	}

	public Label getTempMax() {
		return tempMax;
	}

	public Label getCondition() {
		return condition;
	}

}
