package com.jbr.middletier.backup.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Date;

public class SynchronizeStatus {
    final static private Logger LOG = LoggerFactory.getLogger(SynchronizeStatus.class);

    public FileInfo sourceFile;
    public DirectoryInfo sourceDirectory;
    public Classification classification;
    public Source source;
    public Source destination;
    public FileInfo destinationFile;
    public DirectoryInfo destinationDirectory;

    public SynchronizeStatus(FileInfo sourceFile,
                             DirectoryInfo sourceDirectory,
                             Classification classification,
                             Source source,
                             Source destination,
                             FileInfo destinationFile,
                             DirectoryInfo destinationDirectory) {
        this.sourceFile = sourceFile;
        this.sourceDirectory = sourceDirectory;
        this.classification = classification;
        this.source = source;
        this.destination = destination;
        this.destinationFile = destinationFile;
        this.destinationDirectory = destinationDirectory;
    }

    @Override
    public String toString() {
        return sourceFile.toString() + " " + classification.toString();
    }
}
