# JBR-712 - additional flags

Currently files are flagged as being video or image, i would like to have a new flag for hyper link which indicates if a file can be viewed in a browser.

## Changes

Add a flag to the classification object that is true/false (same as other flags) - this indicates if the files of this classification can be viewed in a browser - call it browser.  Initialise to false for all classifications.

This means the flag will also need to be on FileDTO and copied from the Classification if present.