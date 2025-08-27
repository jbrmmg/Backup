package com.jbr.middletier.backup.data;

public class MD5 {
    private final String md5Value;

    public MD5(String md5) {
        if((md5 == null) || (md5.isEmpty())) {
            throw new IllegalArgumentException("Cannot create MD5 with null or empty string");
        }

        if(md5.length() != 32) {
            throw new IllegalArgumentException("You must create MD5 with 32 characters");
        }

        if(!md5.matches("[0-9a-fA-F]+")) {
            throw new IllegalArgumentException("MD5 must only contain HEX digits (0-9 or A-F)");
        }

        this.md5Value = md5.toUpperCase();
    }

    public MD5(MD5 source) {
        this(source.md5Value);
    }

    public String getValue() { return this.toString(); }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;

        if(!(o instanceof MD5 md5)) return false;

        return this.md5Value.equalsIgnoreCase(md5.md5Value);
    }

    @Override
    public int hashCode() {
        return this.md5Value.hashCode();
    }

    @Override
    public String toString() {
        return this.md5Value;
    }
}
