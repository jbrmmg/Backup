package com.jbr.middletier.backup.util;

import java.util.Objects;

public class LatLong {
    private final String latitude;
    private final NorthSouth latitudeRef;
    private final String longitude;
    private final EastWest longitudeRef;
    private final Double decimalLatitude;
    private final Double decimalLongitude;

    private static class ReferenceType {
        private final int multiplier;

        public ReferenceType(boolean positive) {
            this.multiplier = positive ? 1 : -1;
        }

        public double multiply (double value) {
            return value * this.multiplier;
        }
    }

    private static class EastWest extends ReferenceType {
        private static boolean getMultiplier(String latitudeReference) {
            if(latitudeReference.equalsIgnoreCase("E")) {
                return true;
            }

            if(latitudeReference.equalsIgnoreCase("W")) {
                return false;
            }

            throw new IllegalArgumentException("East west reference must be 'E' or 'W'");
        }

        public EastWest(String latitudeReference) {
            super(EastWest.getMultiplier(latitudeReference));
        }
    }

    private static class NorthSouth extends ReferenceType {
        private static boolean getMultiplier(String latitudeReference) {
            if(latitudeReference.equalsIgnoreCase("N")) {
                return true;
            }

            if(latitudeReference.equalsIgnoreCase("S")) {
                return false;
            }

            throw new IllegalArgumentException("East west reference must be 'N' or 'S'");
        }

        public NorthSouth(String latitudeReference) {
            super(NorthSouth.getMultiplier(latitudeReference));
        }
    }

    private double getDegrees(String degrees) {
        degrees = degrees.replace("deg", "").trim();

        return Math.abs(Double.parseDouble(degrees));
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

        if(element.endsWith("deg")) {
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

    private double getCoordinate(String source, ReferenceType reference) {
        try {
            String[] elements = source.replace(" deg", "deg").split(" ");

            double value = 0.0;
            for (String next : elements) {
                value += getValue(next);
            }

            return reference.multiply(value);
        } catch (Exception ignored) {
        }

        return 0.0;
    }

    public LatLong(String latitude, String latitudeRef, String longitude, String longitudeRef) {
        this.latitude = latitude;
        this.latitudeRef = new NorthSouth(latitudeRef);
        this.decimalLatitude = null;
        this.longitude = longitude;
        this.longitudeRef = new EastWest(longitudeRef);
        this.decimalLongitude = null;
    }

    public LatLong(Double latitude, Double longitude) {
        this.latitude = null;
        this.latitudeRef = null;
        this.decimalLatitude = latitude;
        this.longitude = null;
        this.longitudeRef = null;
        this.decimalLongitude = longitude;
    }

    public double getLatitude() {
        return Objects.requireNonNullElseGet(this.decimalLatitude, () -> getCoordinate(latitude, latitudeRef));
    }

    public double getLongitude() {
        return Objects.requireNonNullElseGet(this.decimalLongitude, () -> getCoordinate(longitude, longitudeRef));
    }
}
