package com.jbr.middletier.backup.util;

public class LatLong {
    private final String latitude;
    private final String latitudeRef;
    private final String longitude;
    private final String longitudeRef;

    private double getDegrees(String degrees) {
        degrees = degrees.replace("°", "").trim();

        return Double.parseDouble(degrees);
    }

    private double getMinutes(String minutes) {
        minutes = minutes.replace("'", "").trim();

        return Double.parseDouble(minutes) / 60.0;
    }

    private double getSeconds(String seconds) {
        seconds = seconds.replace("\"", "").trim();

        return Double.parseDouble(seconds) / 3600.0;
    }

    private double getValue(String element) {
        element = element.trim();

        if(element.endsWith("°")) {
            return getDegrees(element);
        }

        if(element.endsWith("'")) {
            return getMinutes(element);
        }

        if(element.endsWith("\"")) {
            return getSeconds(element);
        }

        return 0.0;
    }

    private double getCoordinate(String source, String reference) {
        try {
            String[] elements = source.split(" ");

            double value = 0.0;
            for (String next : elements) {
                value += getValue(next);
            }

            if (reference.equalsIgnoreCase("s") || reference.equalsIgnoreCase("w")) {
                value *= -1;
            }

            return value;
        } catch (Exception ignored) {
        }

        return 0.0;
    }

    public LatLong(String latitude, String latitudeRef, String longitude, String longitudeRef) {
        this.latitude = latitude;
        this.latitudeRef = latitudeRef;
        this.longitude = longitude;
        this.longitudeRef = longitudeRef;
    }

    public double getLatitude() {
        return getCoordinate(latitude,latitudeRef);
    }

    public double getLongitude() {
        return getCoordinate(longitude,longitudeRef);
    }
}
