package net.statemesh.promql.converter.query;

import net.statemesh.promql.converter.MetricData;

import java.util.ArrayList;
import java.util.Collection;

public class ListVectorData extends ArrayList<VectorData> implements MetricData {
    public ListVectorData(Collection<VectorData> c) {
        super(c);
    }
}
