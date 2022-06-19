package com.jbr.middletier.backup.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix="backup")
public class ApplicationProperties {
    public static class Directory {
        private String name;
        private long days;
        private String zip;
        private String dateFormat;

        public void setName(String directoryName) { this.name = directoryName; }

        public String getName() { return this.name; }

        public void setDays(int directoryDays) { this.days = directoryDays; }

        public long getDays() { return this.days; }

        public void setZip(String directoryZip) { this.zip = directoryZip; }

        public String getZip() { return this.zip; }

        public void setDateFormat(String directoryDateFormat) { this.dateFormat = directoryDateFormat; }

        public String getDateFormat() { return this.dateFormat; }
    }

    public static class Email {
        private String host;
        private String user;
        private String password;
        private String from;
        private String to;
        private Boolean enabled;
        private Integer port;
        private Boolean authenticate;

        public void setHost(String host) { this.host = host;}

        public void setUser(String user) { this.user = user; }

        public void setPassword(String password) { this.password = password; }

        public void setFrom(String from) { this.from = from; }

        public void setTo(String to) { this.to = to; }

        public void setEnabled(Boolean enabled) { this.enabled = enabled; }

        public void setPort(Integer port) { this.port = port; }

        public void setAuthenticate(Boolean authenticate) { this.authenticate = authenticate; }

        public String getHost() { return this.host; }

        public String getUser() { return this.user; }

        public String getPassword() { return this.password; }

        public String getFrom() { return this.from; }

        public Boolean getEnabled() { return this.enabled; }

        public String getTo() { return this.to; }

        public Integer getPort() { return this.port; }

        public Boolean getAuthenticate() { return this.authenticate; }
    }

    private final Directory directory = new Directory();
    private final Email email = new Email();
    private String serviceName;
    private String dbUrl;
    private String dbPassword;
    private String dbUsername;
    private String zipDirectory;
    private String webLogUrl;
    private boolean cacheWebLog;
    private String schedule;
    private boolean enabled;
    private String gatherSchedule;
    private boolean gatherEnabled;
    private String reviewDirectory;
    private String dbBackupCommand;
    private Long dbBackupMaxTime;

    public Directory getDirectory() { return this.directory; }

    public Email getEmail() { return this.email; }

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

    public void setCacheWebLog(boolean cacheWebLog) { this.cacheWebLog = cacheWebLog; }

    public boolean getCacheWebLog() { return this.cacheWebLog; }

    public void setSchedule(String schedule) { this.schedule = schedule; }

    public String getSchedule() { return this.schedule; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean getEnabled() { return this.enabled; }

    public void setGatherSchedule(String schedule) { this.gatherSchedule = schedule; }

    public String getGatherSchedule() { return this.gatherSchedule; }

    public void setGatherEnabled(boolean enabled) { this.gatherEnabled = enabled; }

    public boolean getGatherEnabled() { return this.gatherEnabled; }

    public void setDbBackupCommand(String dbBackupCommand) { this.dbBackupCommand = dbBackupCommand; }

    public String getDbBackupCommand() { return this.dbBackupCommand; }

    public void setDbBackupMaxTime(Long dbBackupMaxTime) { this.dbBackupMaxTime = dbBackupMaxTime; }

    public Long getDbBackupMaxTime() { return this.dbBackupMaxTime; }
}
