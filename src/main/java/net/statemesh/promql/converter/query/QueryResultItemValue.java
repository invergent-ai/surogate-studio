package net.statemesh.promql.converter.query;

import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class QueryResultItemValue {
    private double timestamp;
    private double value;
}
