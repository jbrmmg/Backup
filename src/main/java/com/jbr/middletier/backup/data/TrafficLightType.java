package com.jbr.middletier.backup.data;

public enum TrafficLightType {
    TL_UNKNOWN("UNKNOWN"),
    TL_RED("RED"),
    TL_AMBER("AMBER"),
    TL_GREEN("GREEN");

    private final String type;

    TrafficLightType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return this.type;
    }

    public static TrafficLightType getTrafficLightType(String name) {
        for(TrafficLightType type : TrafficLightType.values()) {
            if(type.getTypeName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        throw new IllegalStateException(name + " is not a valid Traffic Light type");
    }
}
