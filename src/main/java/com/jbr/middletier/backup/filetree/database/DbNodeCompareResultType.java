package com.jbr.middletier.backup.filetree.database;

import com.jbr.middletier.backup.data.ClassificationActionType;

public enum DbNodeCompareResultType {
    DBC_EQUAL,
    DBC_EQUAL_EXCEPT_DATE,
    DBC_NOT_EQUAL;
}
