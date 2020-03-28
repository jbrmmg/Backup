package com.jbr.middletier.backup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="backup",ignoreUnknownFields = true)
public class ApplicationProperties {
    private String serviceName;
    private String directoryName;
    private int directoryDays;
    private String directoryZip;
    private String directoryDateFormat;
    private String dbUrl;
    private String dbPassword;
    private String dbUsername;
    private String zipDirectory;

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getServiceName() { return this.serviceName; }

    public void setDirectoryName(String directoryName) { this.directoryName = directoryName; }

    public String getDirectoryName() { return this.directoryName; }

    public void setDirectoryDays(int directoryDays) { this.directoryDays = directoryDays; }

    public int getDirectoryDays() { return this.directoryDays; }

    public void setDirectoryZip(String directoryZip) { this.directoryZip = directoryZip; }

    public String getDirectoryZip() { return this.directoryZip; }

    public void setDirectoryDateFormat(String directoryDateFormat) { this.directoryDateFormat = directoryDateFormat; }

    public String getDirectoryDateFormat() { return this.directoryDateFormat; }

    public void setDbUrl(String dbUrl) { this.dbUrl = dbUrl; }

    public String getDbUrl() { return this.dbUrl; }

    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

    public String getDbPassword() { return this.dbPassword; }

    public void setDbUsername(String dbUsername) { this.dbUsername = dbUsername; }

    public String getDbUsername() { return this.dbUsername; }

    public void setZipDirectory(String zipDirectory) { this.zipDirectory = zipDirectory; }

    public String getZipDirectory() { return this.zipDirectory; }
}
