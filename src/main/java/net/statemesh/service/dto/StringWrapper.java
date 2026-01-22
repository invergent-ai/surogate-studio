package net.statemesh.service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StringWrapper {
    private String value;

    public static StringWrapper of(String value) {return new StringWrapper(value);}
}
