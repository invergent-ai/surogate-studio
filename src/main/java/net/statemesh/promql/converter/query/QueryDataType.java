package net.statemesh.promql.converter.query;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import net.statemesh.promql.converter.MetricData;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public enum QueryDataType {
    Matrix{

        @SuppressWarnings("unchecked")
        @Override
        public MatrixData convert(JsonReader reader) throws IOException {
            MatrixData resultDataItem = new MatrixData();
            reader.beginObject();
            while(reader.hasNext()) {
                String name = reader.nextName();
                if("metric".equalsIgnoreCase(name)) {
                    Map<String,String> metric = new HashMap<String,String>();
                    reader.beginObject();
                    while(reader.hasNext()) {
                        metric.put(reader.nextName(), reader.nextString());
                    }
                    reader.endObject();
                    resultDataItem.setMetric(metric);
                } else if("values".equalsIgnoreCase(name)) {
                    ArrayList<QueryResultItemValue> resultDataItemValue = new ArrayList<QueryResultItemValue>();
                    reader.beginArray();
                    while(reader.hasNext()) {
                        reader.beginArray();
                        resultDataItemValue.add(new QueryResultItemValue(readDouble(reader), readDouble(reader)));
                        reader.endArray();
                    }
                    reader.endArray();
                    resultDataItem.setDataValues(resultDataItemValue.toArray(new QueryResultItemValue[] {}));
                }
            }
            reader.endObject();
            return resultDataItem;
        }

    },
    Vector{

        @SuppressWarnings("unchecked")
        @Override
        public VectorData convert(JsonReader reader) throws IOException {
            VectorData resultDataItem = new VectorData();
            reader.beginObject();
            while(reader.hasNext()) {
                String name = reader.nextName();
                if("metric".equalsIgnoreCase(name)) {
                    Map<String,String> metric = new HashMap<String,String>();
                    reader.beginObject();
                    while(reader.hasNext()) {
                        metric.put(reader.nextName(), reader.nextString());
                    }
                    reader.endObject();
                    resultDataItem.setMetric(metric);
                } else if("value".equalsIgnoreCase(name)) {
                    reader.beginArray();
                    resultDataItem.setDataValue(new QueryResultItemValue(readDouble(reader), readDouble(reader)));
                    reader.endArray();
                }
            }
            reader.endObject();
            return resultDataItem;
        }

    },
    Scalar{

        @SuppressWarnings("unchecked")
        @Override
        public ScalarData convert(JsonReader reader) throws IOException {
            ScalarData resultDataItem = null;
            resultDataItem = new ScalarData(readDouble(reader), readDouble(reader));
            return resultDataItem;
        }

    };

    // Helper method to handle Prometheus special values
    private static double readDouble(JsonReader reader) throws IOException {
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull();
            return Double.NaN;
        }

        if (reader.peek() == JsonToken.STRING) {
            String value = reader.nextString();
            switch (value) {
                case "+Inf":
                case "Inf":
                    return Double.POSITIVE_INFINITY;
                case "-Inf":
                    return Double.NEGATIVE_INFINITY;
                case "NaN":
                    return Double.NaN;
                default:
                    try {
                        return Double.parseDouble(value);
                    } catch (NumberFormatException e) {
                        return Double.NaN;
                    }
            }
        }

        // Normal numeric value
        return reader.nextDouble();
    }

    public abstract <T extends MetricData> T convert(JsonReader reader) throws IOException ;
}
