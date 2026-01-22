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
public class VectorData implements MetricData {
    private Map<String, String> metric = new HashMap<>();
    private QueryResultItemValue dataValue = new QueryResultItemValue();

    public double getValue() {
        return dataValue.getValue();
    }

    public double getTimestamp() {
        return dataValue.getTimestamp();
    }
}
