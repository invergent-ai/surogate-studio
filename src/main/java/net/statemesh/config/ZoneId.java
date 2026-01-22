package net.statemesh.config;

public enum ZoneId {
    DENSEMAX("densemax"),
    EU_CENTRAL("eu-central"),
    SINGAPORE("sg"),
    HONG_KONG("hk"),
    INDIA("india"),
    CHINA_NORTH("cn-north"),
    AFRICA("africa"),
    US_EAST("us-east"),
    US_WEST("us-west");

    private final String zoneId;

    ZoneId(String zoneId) {
        this.zoneId = zoneId;
    }

    public String getValue() {
        return zoneId;
    }

    public static ZoneId fromValue(String value) {
        for (ZoneId zoneId : ZoneId.values()) {
            if (zoneId.getValue().equals(value)) {
                return zoneId;
            }
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}
