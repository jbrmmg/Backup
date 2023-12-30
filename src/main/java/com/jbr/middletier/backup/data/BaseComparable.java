package com.jbr.middletier.backup.data;

import java.io.Serializable;

public class BaseComparable implements Serializable {
    @Override
    public boolean equals(Object o) {
        if(o == this) return true;

        if(o == null || !o.getClass().equals(this.getClass())) {
            return false;
        }

        return this.toString().equalsIgnoreCase(o.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }
}
