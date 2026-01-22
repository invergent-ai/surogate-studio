package net.statemesh.promql.converter.series;


import net.statemesh.promql.converter.Result;

import java.util.ArrayList;
import java.util.List;

public class DefaultSeriesResult extends Result<SeriesResultItem> {
	List<SeriesResultItem> result = new ArrayList<SeriesResultItem>();
	public void addData(SeriesResultItem data) {
		result.add(data);
	}

	@Override
	public List<SeriesResultItem> getResult() {
		return result;
	}

	@Override
	public String toString() {
		return "SeriesResultItem [result=" + result + "]";
	}

}
