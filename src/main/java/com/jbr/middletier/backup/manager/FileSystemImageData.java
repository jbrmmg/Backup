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
    private ImageDataDirectoryType dateSource;
    private int width;
    private int height;
    private boolean valid;

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
            default:
                // Ignore any other types.
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
            default:
                // Ignore any other types.
        }
    }

    private void setDateTime(String value, String format, ImageDataDirectoryType source) {
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
            setDateTime(value,EXIF_DATE_FORMAT,ImageDataDirectoryType.IDD_ICC_PROFILE);
        }
    }

    private void extractFromExifSubIfd(String tag, String value) {
        if(!tag.equals(TAG_EXIF_SUBIFD)) {
            return;
        }

        if(this.dateTime == null || this.dateSource.equals(ImageDataDirectoryType.IDD_ICC_PROFILE)) {
            setDateTime(value,EXIF_DATE_FORMAT,ImageDataDirectoryType.IDD_EXIF_SUBIFD);
        }
    }

    private void extractFromMp4(String tag, String value) {
        if(!tag.equals(TAG_CREATION_TIME)) {
            return;
        }

        setDateTime(value,MP4_DATE_FORMAT,ImageDataDirectoryType.IDD_EXIF_SUBIFD);
    }

    private void extractFromQuickTime(String tag, String value) {
        if(!tag.equals(TAG_CREATION_TIME)) {
            return;
        }

        setDateTime(value,QUICKTIME_DATE_FORMAT,ImageDataDirectoryType.IDD_QUICKTIME);
    }

    private void extractFrom(String directory, String tag, String value) {
        ImageDataDirectoryType directoryType = ImageDataDirectoryType.getImageDataDirectoryType(directory);
        
        switch (directoryType) {
            case IDD_PNG_IHDR:
                extractFromPngIhdr(tag,value);
                break;
            case IDD_ICC_PROFILE:
                extractFromIccProfile(tag,value);
                break;
            case IDD_EXIF_SUBIFD:
                extractFromExifSubIfd(tag,value);
                break;
            case IDD_JPEG:
                extractFromJpeg(tag,value);
                break;
            case IDD_MP4:
                extractFromMp4(tag,value);
                break;
            case IDD_QUICKTIME:
                extractFromQuickTime(tag,value);
                break;
            case IDD_PNG_ICCP,
                IDD_EXIF_IFD0,
                IDD_XMP,
                IDD_FILE_TYPE,
                IDD_FILE,
                IDD_JFIF,
                IDD_APPLE_MAKERNOTE,
                IDD_APPLE_RUN_TIME,
                IDD_GPS,
                IDD_HUFFMAN,
                IDD_MP4_SOUND,
                IDD_MP4_VIDEO,
                IDD_QUICKTIME_SOUND,
                IDD_QUICKTIME_VIDEO,
                IDD_QUICKTIME_METADATA,
                IDD_PNG_SRGB,
                IDD_EXIF_THUMBNAIL,
                IDD_PHOTOSHOP,
                IDD_IPTC:
                // Ignore these headers.
                break;
            case IDD_UNKNOWN:
                LOG.warn("Directory - {} not handled.", directory);
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

    public ImageDataDirectoryType getDateSourceType() {
        return this.dateSource;
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
