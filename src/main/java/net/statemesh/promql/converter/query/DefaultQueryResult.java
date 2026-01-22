package net.statemesh.promql.converter.query;

import net.statemesh.promql.converter.MetricData;
import net.statemesh.promql.converter.Result;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;


public class DefaultQueryResult<T extends MetricData> extends Result<T> {

	List<T> result = new ArrayList<T>();
	public void addData(T data) {
		result.add(data);
	}

	@Override
	public List<T> getResult() {
		return result;
	}

	@Override
	public String toString() {
		return "DefaultQueryResult [result=" + result + "]";
	}

}
