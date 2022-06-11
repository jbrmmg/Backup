package com.jbr.middletier.backup.data;

public class MD5 {
    private String md5;

    public MD5() {
        this.md5 = null;
    }

    public MD5(String md5) {
        if((md5 != null) && (md5.length() > 0)) {
            this.md5 = md5;
        }

        this.md5 = null;
    }

    @Override
    public String toString() {
        return this.md5;
    }
}
