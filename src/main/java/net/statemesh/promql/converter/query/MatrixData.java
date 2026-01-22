package net.statemesh.promql.converter.query;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.statemesh.promql.converter.MetricData;

import java.util.HashMap;
import java.util.Map;


@Getter
@Setter
@ToString
public class MatrixData implements MetricData {
	private Map<String,String> metric = new HashMap<>();
	private QueryResultItemValue[] dataValues = new QueryResultItemValue[0];

	public double[] getValues() {
		double[] values = new double[dataValues.length];
		int index = 0;
		for(QueryResultItemValue dataValue : dataValues) {
			values[index++] = dataValue.getValue();
		}
		return values;
	}

	public double[] getTimestamps() {
		double[] timestamps = new double[dataValues.length];
		int index = 0;
		for(QueryResultItemValue dataValue : dataValues) {
			timestamps[index++] = dataValue.getTimestamp();
		}
		return timestamps;
	}
}
