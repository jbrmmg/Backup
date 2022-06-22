package com.jbr.middletier.backup.data;

public class MD5 {
    private final String md5Value;

    public MD5() {
        this.md5Value = null;
    }

    public MD5(String md5) {
        if((md5 != null) && (md5.length() > 0)) {
            this.md5Value = md5;
            return;
        }

        this.md5Value = null;
    }

    public MD5(MD5 source) {
        this(source.md5Value);
    }

    public boolean compare(MD5 rhs, boolean blankOK) {
        if(!blankOK) {
            return toString().equals(rhs.toString());
        }

        if(this.md5Value == null)
            return true;

        if(rhs.md5Value == null)
            return true;

        return this.md5Value.equals(rhs.md5Value);
    }

    public boolean isSet() {
        return this.md5Value != null;
    }

    @Override
    public String toString() {
        return this.md5Value == null ? "" : this.md5Value;
    }
}
