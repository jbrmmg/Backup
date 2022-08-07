package com.jbr.middletier.backup.manager;

public enum ImageDataDirectoryType {
    IDD_UNKNOWN("<unknown>"),
    IDD_PNG_IHDR("PNG-IHDR"),
    IDD_ICC_PROFILE("ICC Profile"),
    IDD_EXIF_SUBIFD("Exif SubIFD"),
    IDD_JPEG("JPEG"),
    IDD_MP4("MP4"),
    IDD_PNG_ICCP("PNG-iCCP"),
    IDD_EXIF_IFD0("Exif IFD0"),
    IDD_XMP("XMP"),
    IDD_FILE_TYPE("File Type"),
    IDD_FILE("File"),
    IDD_JFIF("JFIF"),
    IDD_APPLE_MAKERNOTE("Apple Makernote"),
    IDD_APPLE_RUN_TIME("Apple Run Time"),
    IDD_GPS("GPS"),
    IDD_HUFFMAN("Huffman"),
    IDD_MP4_SOUND("MP4 Sound"),
    IDD_MP4_VIDEO("MP4 Video"),
    IDD_QUICKTIME_SOUND("QuickTime Sound"),
    IDD_QUICKTIME_VIDEO("QuickTime Video"),
    IDD_QUICKTIME_METADATA("QuickTime Metadata"),
    IDD_PNG_SRGB("PNG-sRGB"),
    IDD_EXIF_THUMBNAIL("Exif Thumbnail"),
    IDD_PHOTOSHOP("Photoshop"),
    IDD_IPTC("IPTC"),
    IDD_QUICKTIME("QuickTime");

    private final String type;
    
    ImageDataDirectoryType(String type) {
        this.type = type;
    }

    public String getTypeName() {
        return this.type;
    }

    public static ImageDataDirectoryType getImageDataDirectoryType(String name) {
        for(ImageDataDirectoryType type : ImageDataDirectoryType.values()) {
            if(type.getTypeName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return IDD_UNKNOWN;
    }
}
