package net.statemesh.promql.converter.status;

import net.statemesh.promql.converter.Result;

import java.util.ArrayList;
import java.util.List;

public class DefaultConfigResult extends Result<String> {
	List<String> result = new ArrayList<String>();
	public void addData(String data) {
		result.add(data);
	}

	@Override
	public List<String> getResult() {
		return result;
	}

	@Override
	public String toString() {
		return "DefaultConfigResult [result=" + result + "]";
	}

}
