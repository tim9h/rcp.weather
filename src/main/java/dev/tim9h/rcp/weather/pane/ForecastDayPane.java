package dev.tim9h.rcp.weather.pane;

import dev.tim9h.rcp.weather.util.WeatherIconUtil;
import dev.tim9h.rcp.weather.util.WeatherSymbolConverter;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

public class ForecastDayPane {

	private static final String CSS_CLASS_SECONDARY = "secondary";

	private static final String CSS_CLASS_ACCENT = "accent-label";

	private static final String CSS_CLASS_DETAIL = "detail-label";

	private Label weekday;

	private Label tempMin;

	private Label tempMax;

	private Label condition;

	private HBox top;

	private HBox bottom;

	public ForecastDayPane() {
		weekday = new Label();
		weekday.getStyleClass().addAll(CSS_CLASS_SECONDARY, CSS_CLASS_DETAIL);
		tempMin = new Label();
		tempMax = new Label();
		tempMax.getStyleClass().add(CSS_CLASS_ACCENT);
		condition = new Label();
		top = new HBox(weekday, tempMax, tempMin);
		top.setSpacing(0.0);
		var symbol = new Label();
		symbol.getStyleClass().add(CSS_CLASS_SECONDARY);
		symbol.graphicProperty().bind(Bindings.createObjectBinding(() -> {
			var resource = WeatherSymbolConverter.getIconResource(condition.getText());
			return resource == null ? null : WeatherIconUtil.loadIcon(resource, symbol, 16);
		}, condition.textProperty()));
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

	public void clear() {
		weekday.setText("");
		tempMin.setText("");
		tempMax.setText("");
		condition.setText("");
	}

}
