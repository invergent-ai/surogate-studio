package net.statemesh.promql.converter.query;

import net.statemesh.promql.converter.MetricData;

public class ScalarData extends QueryResultItemValue implements MetricData {

	public ScalarData(double timestamp, double value) {
		super(timestamp, value);
	}

}
