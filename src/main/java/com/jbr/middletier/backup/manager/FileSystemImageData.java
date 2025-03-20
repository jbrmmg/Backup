package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.util.ImageSize;
import com.jbr.middletier.backup.util.LatLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@SuppressWarnings("unused")
public class FileSystemImageData {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemImageData.class);

    private static final String META_DATETIME_FORMAT = "yyyy:MM:dd HH:mm:ss";

    private LocalDateTime dateTime;
    private ImageSize size;
    private LatLong latLong;
    private String mimeType;
    private boolean valid;

    private LocalDateTime getMimeDateTime(String dateTime) {
        if(dateTime == null || dateTime.length() < META_DATETIME_FORMAT.length())
            return null;

        //2025:03:01 13:56:44
        //2025:03:01 13:56:43+00:00
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(META_DATETIME_FORMAT);
        return LocalDateTime.parse(dateTime.substring(0,META_DATETIME_FORMAT.length()), formatter);
    }

    private ImageSize getMimeImageSize(String size) {
        try {
            // Format of the string is width x height
            String[] split = size.split("x");

            int width = Integer.parseInt(split[0].trim());
            int height = Integer.parseInt(split[1].trim());

            return new ImageSize(width, height);
        } catch (NumberFormatException e) {
            return null;
        }
    }



    private LatLong getMimeLatLong(String gps) {
        try {
            if(gps == null || gps.isEmpty()) {
                return null;
            }

            // 51 deg 27' 22.41" N, 2 deg 37' 32.56" W
            // 51 deg 27' 22.32" N, 2 deg 37' 32.52" W
            // Format of the string is:     a deg b' c.cc" d, a deg b' c.cc" d
            // Where a = degrees, b = minutes, c.cc = seconds and d = reference (N/S) or (E/W)

            String[] split = gps.split(",");

            String lat = split[0].trim();
            String lng = split[1].trim();

            split = lat.split("\"");

            String latCoord = split[0].trim() + "\"";
            String latRef = split[1].trim();

            split = lng.split("\"");

            String lngCoord = split[0].trim() + "\"";
            String lngRef = split[1].trim();

            return new  LatLong(latCoord, latRef, lngCoord, lngRef);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void processImageMetaData(Map<String,String> metaData) {
        this.dateTime = getMimeDateTime(metaData.get("date/time original"));
        this.size = getMimeImageSize(metaData.get("image size"));
        this.latLong = getMimeLatLong(metaData.get("gps position"));
    }

    private void processVideoMetaData(Map<String,String> metaData) {
        this.dateTime = getMimeDateTime(metaData.get("creation date"));
        if(this.dateTime == null) {
            this.dateTime = getMimeDateTime(metaData.get("media create date"));
        }
        this.size = getMimeImageSize(metaData.get("image size"));
        this.latLong = getMimeLatLong(metaData.get("gps position"));
    }

    public FileSystemImageData(Map<String,String> metaData) {
        try {
            this.dateTime = null;
            this.size = null;
            this.latLong = null;
            this.mimeType = "";

            if(metaData != null) {
                this.valid = false;

                // Get the mime type
                if(!metaData.containsKey("mime type")) {
                    throw new IllegalArgumentException("Missing mime type, cannot determine the file meta data type");
                }

                this.mimeType = metaData.get("mime type");

                // Only expect there to be an image or video.
                String[] mimeTypeElements = mimeType.split("/");
                switch (mimeTypeElements[0].trim()) {
                    case "image":
                        processImageMetaData(metaData);
                        break;
                    case "video":
                        processVideoMetaData(metaData);
                        break;
                    default:
                        throw new  IllegalArgumentException("Expect the mime type to be image or video.");
                }
            }
        } catch(Exception e) {
            this.valid = false;
        }

        this.valid = true;
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public ImageSize getImageSize() {
        return this.size;
    }

    public LatLong getLatLong() {
        return this.latLong;
    }

    public String getMimeType() {
        return this.mimeType;
    }

    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        if(this.valid) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMMM-uuuu hh:mm");
            return this.dateTime.format(formatter) + " " + this.mimeType;
        }

        return "(no valid meta date)";
    }
}
