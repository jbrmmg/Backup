package com.jbr.middletier.backup.data;

@SuppressWarnings("unused")
public class SynchronizeStatus {
    private FileInfo sourceFile;
    private DirectoryInfo sourceDirectory;
    private Classification classification;
    private Source source;
    private Source destination;
    private FileInfo destinationFile;
    private DirectoryInfo destinationDirectory;

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

    public FileInfo getSourceFile() { return this.sourceFile; }
    public void setSourceFile(FileInfo sourceFile) { this.sourceFile = sourceFile; }
    public DirectoryInfo getSourceDirectory() { return this.sourceDirectory; }
    public Classification getClassification() { return this.classification; }
    public Source getSource() { return this.source; }
    public Source getDestination() { return this.destination; }
    public FileInfo getDestinationFile() { return this.destinationFile; }
    public DirectoryInfo getDestinationDirectory() { return this.destinationDirectory; }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();

        if(this.sourceFile != null) {
            output.append(getSourceFile().toString());
            output.append(" ");
        }

        if(this.classification != null) {
            output.append(getClassification().toString());
            output.append(" ");
        }

        if(this.source != null) {
            output.append(getSource().toString());
            output.append(" ");
        }

        if(this.destinationDirectory != null) {
            output.append(getDestinationDirectory().toString());
            output.append(" ");
        }

        if(this.destination != null) {
            output.append(getDestination().toString());
        }

        return output.toString();
    }
}
