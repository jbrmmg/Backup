package com.jbr.middletier.backup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix="backup",ignoreUnknownFields = true)
public class ApplicationProperties {
    public class Directory {
        private String name;
        private int days;
        private String zip;
        private String dateFormat;

        public void setName(String directoryName) { this.name = directoryName; }

        public String getName() { return this.name; }

        public void setDays(int directoryDays) { this.days = directoryDays; }

        public int getDays() { return this.days; }

        public void setZip(String directoryZip) { this.zip = directoryZip; }

        public String getZip() { return this.zip; }

        public void setDateFormat(String directoryDateFormat) { this.dateFormat = directoryDateFormat; }

        public String getDateFormat() { return this.dateFormat; }
    }

    private Directory directory = new Directory();
    private String serviceName;
    private String dbUrl;
    private String dbPassword;
    private String dbUsername;
    private String zipDirectory;

    public Directory getDirectory() { return this.directory; }

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getServiceName() { return this.serviceName; }

    public void setDbUrl(String dbUrl) { this.dbUrl = dbUrl; }

    public String getDbUrl() { return this.dbUrl; }

    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

    public String getDbPassword() { return this.dbPassword; }

    public void setDbUsername(String dbUsername) { this.dbUsername = dbUsername; }

    public String getDbUsername() { return this.dbUsername; }

    public void setZipDirectory(String zipDirectory) { this.zipDirectory = zipDirectory; }

    public String getZipDirectory() { return this.zipDirectory; }
}
