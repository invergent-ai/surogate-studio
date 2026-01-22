package net.statemesh.domain.enumeration;


import lombok.Getter;

@Getter
public enum ColorScheme {
    LIGHT("light"),
    DARK("dark");

    private final String value;

    ColorScheme(String value) {
        this.value = value;
    }
}
