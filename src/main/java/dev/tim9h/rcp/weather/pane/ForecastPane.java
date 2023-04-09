package dev.tim9h.rcp.weather.pane;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import dev.tim9h.rcp.logging.InjectLogger;
import dev.tim9h.rcp.weather.bean.Forecast;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

public class ForecastPane extends GridPane {

	@InjectLogger
	private Logger logger;

	private List<ForecastDayPane> days;

	public ForecastPane() {
		getStyleClass().add("ccCard");
		var col = new ColumnConstraints();
		col.setPercentWidth(20);
		col.setHgrow(Priority.ALWAYS);
		col.setHalignment(HPos.LEFT);
		getColumnConstraints().addAll(col, col, col, col, col);

		days = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			var day = new ForecastDayPane();
			days.add(day);
			add(day.getTop(), i, 0);
			add(day.getBottom(), i, 1);
		}
	}

	public void update(Forecast forecast) {
		logger.info(() -> "Updating weather forecast: " + forecast);
		Platform.runLater(() -> {
			var i = 0;
			for (var day : forecast.days().entrySet()) {
				var pane = days.get(i);
				var weather = day.getValue();
				var weekdayFormatted = day.getKey().format(DateTimeFormatter.ofPattern("E")).substring(0, 2);
				pane.getWeekday().textProperty().setValue(weekdayFormatted);
				pane.getTempMin().textProperty()
						.setValue(String.format("%d°", Integer.valueOf(weather.getTempMin().intValue())));
				pane.getTempMax().textProperty()
						.setValue(String.format("%d°", Integer.valueOf(weather.getTempMax().intValue())));
				pane.getCondition().textProperty().setValue(StringUtils.abbreviate(weather.getCondition(), 7));

				i++;
				if (i > forecast.days().size() || i >= days.size()) {
					break;
				}
			}
		});
	}

}
