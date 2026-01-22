package net.statemesh.promql.converter;


import junit.framework.TestCase;
import net.statemesh.promql.converter.am.AlertManagerResultItem;
import net.statemesh.promql.converter.am.DefaultAlertManagerResult;

public class AlertManagerResultTest extends TestCase {
		private String testAlertManagerData="{\"status\":\"success\",\"data\":{\"activeAlertmanagers\":[]}}";

	public void testParser() {
		DefaultAlertManagerResult result = ConvertUtil.convertAlertManagerResultString(testAlertManagerData);
		System.out.println("-----" +result.getResult().size());

		for(AlertManagerResultItem data : result.getResult()) {
			System.out.println("=======>\n" + data);
		}
	}
}
