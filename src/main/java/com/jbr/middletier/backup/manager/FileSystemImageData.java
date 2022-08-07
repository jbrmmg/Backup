package com.jbr.middletier.backup.manager;

import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("unused")
public class FileSystemImageData {
    private static final Logger LOG = LoggerFactory.getLogger(FileSystemImageData.class);

    private LocalDateTime dateTime;
    private String dateSource;
    private int width;
    private int height;
    private boolean valid;

    private static final String DIR_PNG_IHDR = "PNG-IHDR";
    private static final String DIR_ICC_PROFILE = "ICC Profile";
    private static final String DIR_EXIF_SUBIFD = "Exif SubIFD";
    private static final String DIR_JPEG = "JPEG";
    private static final String DIR_MP4 = "MP4";
    private static final String DIR_PNG_ICCP = "PNG-iCCP";
    private static final String DIR_EXIF_IFD0 = "Exif IFD0";
    private static final String DIR_XMP = "XMP";
    private static final String DIR_FILE_TYPE = "File Type";
    private static final String DIR_FILE = "File";
    private static final String DIR_JFIF = "JFIF";
    private static final String DIR_APPLE_MAKERNOTE = "Apple Makernote";
    private static final String DIR_APPLE_RUN_TIME = "Apple Run Time";
    private static final String DIR_GPS = "GPS";
    private static final String DIR_HUFFMAN = "Huffman";
    private static final String DIR_MP4_SOUND = "MP4 Sound";
    private static final String DIR_MP4_VIDEO = "MP4 Video";
    private static final String DIR_QUICKTIME_SOUND = "QuickTime Sound";
    private static final String DIR_QUICKTIME_VIDEO = "QuickTime Video";
    private static final String DIR_QUICKTIME_METADATA = "QuickTime Metadata";
    private static final String DIR_PNG_SRGB = "PNG-sRGB";
    private static final String DIR_EXIF_THUMBNAIL = "Exif Thumbnail";
    private static final String DIR_PHOTOSHOP = "Photoshop";
    private static final String DIR_IPTC = "IPTC";
    private static final String DIR_QUICKTIME = "QuickTime";
    private static final String TAG_IMAGE_WIDTH = "Image Width";
    private static final String TAG_IMAGE_HEIGHT = "Image Height";
    private static final String TAG_ICC_PROFILE_DATETIME = "Profile Date/Time";
    private static final String TAG_EXIF_SUBIFD = "Date/Time Original";
    private static final String TAG_CREATION_TIME = "Creation Time";
    private static final String EXIF_DATE_FORMAT = "uuuu:MM:dd HH:mm:ss";
    private static final String MP4_DATE_FORMAT = "EEE MMM dd HH:mm:ss z uuuu";
    private static final String QUICKTIME_DATE_FORMAT = "EEE MMM dd HH:mm:ss xxx uuuu";

    private void extractFromPngIhdr(String tag, String value) {
        switch (tag) {
            case TAG_IMAGE_WIDTH:
                this.width = Integer.parseInt(value);
                break;
            case TAG_IMAGE_HEIGHT:
                this.height = Integer.parseInt(value);
                break;
        }
    }

    private void extractFromJpeg(String tag, String value) {
        switch (tag) {
            case TAG_IMAGE_WIDTH:
                this.width = Integer.parseInt(value.replace(" pixels", ""));
                break;
            case TAG_IMAGE_HEIGHT:
                this.height = Integer.parseInt(value.replace(" pixels", ""));
                break;
        }
    }

    private void setDateTime(String value, String format, String source) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
        LocalDateTime newDateTime = LocalDateTime.parse(value,formatter);

        LocalDateTime minDateTime = LocalDateTime.of(1990,1, 1, 0, 0);
        if(minDateTime.isBefore(newDateTime)) {
            this.dateTime = newDateTime;
            this.dateSource = source;
            this.valid = true;
        }
    }

    private void extractFromIccProfile(String tag, String value) {
        if(!tag.equals(TAG_ICC_PROFILE_DATETIME)) {
            return;
        }

        if(this.dateTime == null) {
            setDateTime(value,EXIF_DATE_FORMAT,DIR_ICC_PROFILE);
        }
    }

    private void extractFromExifSubIfd(String tag, String value) {
        if(!tag.equals(TAG_EXIF_SUBIFD)) {
            return;
        }

        if(this.dateTime == null || this.dateSource.equals(DIR_ICC_PROFILE)) {
            setDateTime(value,EXIF_DATE_FORMAT,DIR_EXIF_SUBIFD);
        }
    }

    private void extractFromMp4(String tag, String value) {
        if(!tag.equals(TAG_CREATION_TIME)) {
            return;
        }

        setDateTime(value,MP4_DATE_FORMAT,DIR_EXIF_SUBIFD);
    }

    private void extractFromQuickTime(String tag, String value) {
        if(!tag.equals(TAG_CREATION_TIME)) {
            return;
        }

        setDateTime(value,QUICKTIME_DATE_FORMAT,DIR_QUICKTIME);
    }

    private void extractFrom(String directory, String tag, String value) {
        switch (directory) {
            case DIR_PNG_IHDR:
                extractFromPngIhdr(tag,value);
                break;
            case DIR_ICC_PROFILE:
                extractFromIccProfile(tag,value);
                break;
            case DIR_EXIF_SUBIFD:
                extractFromExifSubIfd(tag,value);
                break;
            case DIR_JPEG:
                extractFromJpeg(tag,value);
                break;
            case DIR_MP4:
                extractFromMp4(tag,value);
                break;
            case DIR_QUICKTIME:
                extractFromQuickTime(tag,value);
                break;
            case DIR_PNG_ICCP:
            case DIR_EXIF_IFD0:
            case DIR_XMP:
            case DIR_FILE_TYPE:
            case DIR_FILE:
            case DIR_JFIF:
            case DIR_APPLE_MAKERNOTE:
            case DIR_APPLE_RUN_TIME:
            case DIR_GPS:
            case DIR_HUFFMAN:
            case DIR_MP4_SOUND:
            case DIR_MP4_VIDEO:
            case DIR_QUICKTIME_SOUND:
            case DIR_QUICKTIME_VIDEO:
            case DIR_QUICKTIME_METADATA:
            case DIR_PNG_SRGB:
            case DIR_EXIF_THUMBNAIL:
            case DIR_PHOTOSHOP:
            case DIR_IPTC:
                // Ignore these headers.
                break;
            default:
                LOG.warn("Directory - " + directory + " not handled.");
        }
    }

    public FileSystemImageData(Metadata metaData) {
        try {
            this.dateTime = null;
            this.dateSource = null;
            this.height = 0;
            this.width = 0;

            if(metaData != null) {
                this.valid = false;
                for (Directory directory : metaData.getDirectories()) {
                    for (Tag tag : directory.getTags()) {
                        extractFrom(directory.getName(), tag.getTagName(), tag.getDescription());
                    }
                }
            }
        } catch(Exception e) {
            this.valid = false;
        }
    }

    public LocalDateTime getDateTime() {
        return dateTime;
    }

    public int getWidth() { return this.width; }

    public int getHeight() { return this.height; }

    public boolean isValid() {
        return valid;
    }

    @Override
    public String toString() {
        if(this.valid) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMMM-uuuu hh:mm");
            return this.dateTime.format(formatter) + " " + this.dateSource;
        }

        return "(no valid meta date)";
    }
}
