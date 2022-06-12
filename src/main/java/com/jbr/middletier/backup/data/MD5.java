package com.jbr.middletier.backup.data;

public class MD5 {
    private String md5;

    public MD5() {
        this.md5 = null;
    }

    public MD5(String md5) {
        if((md5 != null) && (md5.length() > 0)) {
            this.md5 = md5;
            return;
        }

        this.md5 = null;
    }

    public MD5(MD5 source) {
        this(source.md5);
    }

    public boolean compare(MD5 rhs, boolean blankOK) {
        if(!blankOK) {
            return toString().equals(rhs.toString());
        }

        if(this.md5 == null)
            return true;

        if(rhs.md5 == null)
            return true;

        return this.md5.equals(rhs.md5);
    }

    public boolean isSet() {
        if(this.md5 == null) {
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        return this.md5 == null ? "" : this.md5;
    }
}
