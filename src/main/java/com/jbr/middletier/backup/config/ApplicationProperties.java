package com.jbr.middletier.backup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="backup")
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
    private String webLogUrl;
    private String schedule;
    private boolean enabled;
    private String gatherSchedule;
    private boolean gatherEnabled;
    private String synchronizeSchedule;
    private boolean synchonizeEnabled;
    private String reviewDirectory;

    public Directory getDirectory() { return this.directory; }

    public void setServiceName(String serviceName) { this.serviceName = serviceName; }

    public String getServiceName() { return this.serviceName; }

    public void setReviewDirectory(String reviewDirectory) { this.reviewDirectory = reviewDirectory; }

    public String getReviewDirectory() { return this.reviewDirectory; }

    public void setDbUrl(String dbUrl) { this.dbUrl = dbUrl; }

    public String getDbUrl() { return this.dbUrl; }

    public void setDbPassword(String dbPassword) { this.dbPassword = dbPassword; }

    public String getDbPassword() { return this.dbPassword; }

    public void setDbUsername(String dbUsername) { this.dbUsername = dbUsername; }

    public String getDbUsername() { return this.dbUsername; }

    public void setZipDirectory(String zipDirectory) { this.zipDirectory = zipDirectory; }

    public String getZipDirectory() { return this.zipDirectory; }

    public void setWebLogUrl(String webLogUrl) { this.webLogUrl = webLogUrl; }

    public String getWebLogUrl() { return this.webLogUrl; }

    public void setSchedule(String schedule) { this.schedule = schedule; }

    public String getSchedule() { return this.schedule; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean getEnabled() { return this.enabled; }

    public void setGatherSchedule(String schedule) { this.gatherSchedule = schedule; }

    public String getGatherSchedule() { return this.gatherSchedule; }

    public void setGatherEnabled(boolean enabled) { this.gatherEnabled = enabled; }

    public boolean getGatherEnabled() { return this.gatherEnabled; }
}
