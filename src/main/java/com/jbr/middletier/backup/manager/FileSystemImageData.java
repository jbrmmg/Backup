package com.jbr.middletier.backup.manager;

import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.constants.TiffTagConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class FileSystemImageData {
    private LocalDateTime dateTime;
    private boolean valid;

    public FileSystemImageData(JpegImageMetadata jpegImageMetadata) {
        try {
            TiffField field = jpegImageMetadata.findEXIFValueWithExactMatch(TiffTagConstants.TIFF_TAG_DATE_TIME);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("''uuuu:MM:dd HH:mm:ss''");
            this.dateTime = LocalDateTime.parse(field.getValueDescription(),formatter);

            this.valid = true;
        } catch(Exception e) {
            this.valid = false;
        }
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public boolean isValid() {
        return valid;
    }
}
