package com.jbr.middletier.backup.manager;

import lombok.Getter;

import java.util.Map;

@Getter
public class FileSystemCustomData {
    private final String originalFile;
    private final String originalMd5;
    private final Long originalSize;

    public FileSystemCustomData(Map<String, String> metaData) {
        this.originalFile = findValue(metaData, "jbr_original_file", "jbr original file");
        this.originalMd5 = findValue(metaData, "jbr_original_file_md5", "jbr original file md 5", "jbr original file md5");
        String sizeStr = findValue(metaData, "jbr_original_file_size", "jbr original file size");
        this.originalSize = parseSize(sizeStr);
    }

    private static String findValue(Map<String, String> map, String... keys) {
        for (String key : keys) {
            String value = map.get(key);
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private static Long parseSize(String value) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean hasData() {
        return originalFile != null || originalMd5 != null || originalSize != null;
    }
}
