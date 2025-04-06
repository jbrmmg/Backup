package com.jbr.middletier.backup.data;

public enum TrafficLightType {
    TL_UNKNOWN("UNKNOWN"),
    TL_RED("RED"),
    TL_AMBER("AMBER"),
    TL_GREEN("GREEN");

    private final String value;

    TrafficLightType(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }

    public static TrafficLightType getFromName(String name) {
        for (TrafficLightType nextTL : TrafficLightType.values()) {
            if(nextTL.getValue().equalsIgnoreCase(name)) {
                return nextTL;
            }
        }

        return null;
    }

    public static String getTextValue(TrafficLightType trafficLight) {
        for (TrafficLightType nextTL : TrafficLightType.values()) {
            if(nextTL.equals(trafficLight)) {
                return nextTL.getValue();
            }
        }

        throw new IllegalArgumentException(trafficLight.toString() + " is not a valid TrafficLightType");
    }
}
