package dev.tim9h.rcp.weather.pane;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;

import com.google.inject.Inject;
import com.google.inject.Injector;

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

	private static final DateTimeFormatter WEEKDAY_FORMATTER = DateTimeFormatter.ofPattern("EE");

	@Inject
	public ForecastPane(Injector injector) {
		injector.injectMembers(this);

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
		assert Platform.isFxApplicationThread();
		logger.info(() -> "Updating weather forecast: " + forecast);

		for (var day : days) {
			day.clear();
		}

		var i = 0;
		for (var entry : forecast.days().entrySet()) {
			if (i >= days.size()) {
				break;
			}
			var pane = days.get(i);
			var weather = entry.getValue();
			var weekdayFormatted = entry.getKey().format(WEEKDAY_FORMATTER).substring(0, 2);
			pane.getWeekday().textProperty().setValue(weekdayFormatted);
			pane.getTempMin().textProperty()
					.setValue(String.format("%d°", Integer.valueOf(weather.getTempMin().intValue())));
			pane.getTempMax().textProperty()
					.setValue(String.format("%d°", Integer.valueOf(weather.getTempMax().intValue())));
			pane.getCondition().textProperty().setValue(StringUtils.abbreviate(weather.getCondition(), 7));
			i++;
		}
	}

}
