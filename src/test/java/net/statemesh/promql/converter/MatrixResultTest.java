package net.statemesh.promql.converter;

import junit.framework.TestCase;
import net.statemesh.promql.converter.query.DefaultQueryResult;
import net.statemesh.promql.converter.query.ScalarData;

public class MatrixResultTest extends TestCase {

	private String testScalarData="{\"status\":\"success\",\"data\":{\"resultType\":\"scalar\",\"result\":[1536200364.286,\"1\"]}}";

	public void testParser() {
		DefaultQueryResult<ScalarData> result = ConvertUtil.convertQueryResultString(testScalarData);
		System.out.println("-----" +result.getResult().size());
		for(MetricData metricData : result.getResult()) {
			System.out.println("=======>" + metricData);
		}
	}
}
