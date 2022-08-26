package com.jbr.middletier.backup.config;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import org.modelmapper.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix="backup")
public class ApplicationProperties {
    private Boolean getIsImageOrVideo(FileInfo file, boolean imageCheck) {
        if(null == file) {
            return false;
        }

        if(file.getClassification() == null) {
            return false;
        }

        if(imageCheck) {
            return file.getClassification().getIsImage();
        }

        return file.getClassification().getIsVideo();
    }

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper modelMapper = new ModelMapper();

        Converter<String,SourceStatusType> stringSourceStatusConverter = new AbstractConverter<String, SourceStatusType>() {
            @Override
            protected SourceStatusType convert(String s) {
                return SourceStatusType.getSourceStatusType(s);
            }
        };

        Converter<SourceStatusType,String> sourceStatusStringConverter = new AbstractConverter<SourceStatusType, String>() {
            @Override
            protected String convert(SourceStatusType s) {
                return s.getTypeName();
            }
        };

        Converter<ActionConfirmType,String> actionConfirmStringConverter = new AbstractConverter<ActionConfirmType, String>() {
            @Override
            protected String convert(ActionConfirmType ac) {
                return ac.getTypeName();
            }
        };

        PropertyMap<Source, SourceDTO> sourceMap = new PropertyMap<Source, SourceDTO>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        PropertyMap<ImportSource, ImportSourceDTO> importSourceMap = new PropertyMap<ImportSource, ImportSourceDTO>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
                map().setDestinationId(source.getDestination().getIdAndType().getId());
            }
        };

        PropertyMap<PreImportSource, PreImportSourceDTO> preImportSourceMap = new PropertyMap<PreImportSource, PreImportSourceDTO>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        PropertyMap<ImportSource, SourceDTO> importSource2Map = new PropertyMap<ImportSource, SourceDTO>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        PropertyMap<PreImportSource, SourceDTO> importSource3Map = new PropertyMap<PreImportSource, SourceDTO>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        Converter<Optional<FileSystemObjectId>,String> fsoIdTypeConverter = new AbstractConverter<Optional<FileSystemObjectId>, String>() {
            @Override
            protected String convert(Optional<FileSystemObjectId> fileSystemObjectId) {
                return fileSystemObjectId.map(systemObjectId -> systemObjectId.getType().getTypeName()).orElse(null);
            }
        };

        Converter<Optional<FileSystemObjectId>,Integer> fsoIdIntegerConverter = new AbstractConverter<Optional<FileSystemObjectId>, Integer>() {
            @Override
            protected Integer convert(Optional<FileSystemObjectId> fileSystemObjectId) {
                return fileSystemObjectId.map(FileSystemObjectId::getId).orElse(null);
            }
        };

        Converter<FileInfo,Boolean> actionToIsImage = new AbstractConverter<FileInfo, Boolean>() {
            @Override
            protected Boolean convert(FileInfo file) {
                return getIsImageOrVideo(file,true);
            }
        };

        Converter<FileInfo,Boolean> actionToIsVideo = new AbstractConverter<FileInfo, Boolean>() {
            @Override
            protected Boolean convert(FileInfo file) {
                return getIsImageOrVideo(file,false);
            }
        };

        Converter<FileInfo,Integer> fileToIdConverter = new AbstractConverter<FileInfo, Integer>() {
            @Override
            protected Integer convert(FileInfo file) {
                if(null == file) {
                    return null;
                }

                return file.getIdAndType().getId();
            }
        };

        Converter<FileInfo,String> fileToNameConverter = new AbstractConverter<FileInfo, String>() {
            @Override
            protected String convert(FileInfo file) {
                if(null == file) {
                    return null;
                }

                return file.getName();
            }
        };

        Converter<FileInfo, LocalDateTime> fileToDateConverter = new AbstractConverter<FileInfo, LocalDateTime>() {
            @Override
            protected LocalDateTime convert(FileInfo file) {
                if(null == file) {
                    return null;
                }

                return file.getDate();
            }
        };

        Converter<FileInfo,Long> fileToSizeConverter = new AbstractConverter<FileInfo, Long>() {
            @Override
            protected Long convert(FileInfo file) {
                if(null == file) {
                    return null;
                }

                return file.getSize();
            }
        };

        modelMapper.addConverter(stringSourceStatusConverter);
        modelMapper.addConverter(sourceStatusStringConverter);
        modelMapper.addConverter(actionConfirmStringConverter);

        modelMapper.createTypeMap(Source.class, SourceDTO.class);
        modelMapper.createTypeMap(SourceDTO.class, Source.class);
        modelMapper.createTypeMap(ImportSource.class, ImportSourceDTO.class);
        modelMapper.createTypeMap(ImportSource.class, SourceDTO.class);
        modelMapper.createTypeMap(ImportSourceDTO.class, ImportSource.class);
        modelMapper.createTypeMap(PreImportSource.class, PreImportSourceDTO.class);
        modelMapper.createTypeMap(PreImportSource.class, SourceDTO.class);
        modelMapper.createTypeMap(PreImportSourceDTO.class, PreImportSource.class);
        modelMapper.createTypeMap(Location.class, LocationDTO.class);
        modelMapper.createTypeMap(LocationDTO.class, Location.class);
        modelMapper.createTypeMap(Classification.class, ClassificationDTO.class);
        modelMapper.createTypeMap(ClassificationDTO.class, Classification.class);
        modelMapper.createTypeMap(Hardware.class,HardwareDTO.class);
        modelMapper.createTypeMap(HardwareDTO.class,Hardware.class);
        modelMapper.createTypeMap(Backup.class,BackupDTO.class);
        modelMapper.createTypeMap(BackupDTO.class,Backup.class);
        modelMapper.createTypeMap(DbLog.class,DbLogDTO.class);

        modelMapper.createTypeMap(ImportFile.class,ImportFileDTO.class).addMappings(mapper -> {
            mapper.map(ImportFile::getName,ImportFileDTO::setFilename);
        });

        modelMapper.createTypeMap(FileInfo.class,FileInfoDTO.class).addMappings(mapper -> {
            mapper.using(fsoIdTypeConverter).map(FileInfo::getParentId,FileInfoDTO::setParentType);
            mapper.using(fsoIdIntegerConverter).map(FileInfo::getParentId,FileInfoDTO::setParentId);
            mapper.map(FileInfo::getName,FileInfoDTO::setFilename);
        });

        modelMapper.createTypeMap(IgnoreFile.class,FileInfoDTO.class).addMappings(mapper -> {
            mapper.using(fsoIdTypeConverter).map(FileInfo::getParentId,FileInfoDTO::setParentType);
            mapper.using(fsoIdIntegerConverter).map(FileInfo::getParentId,FileInfoDTO::setParentId);
            mapper.map(FileInfo::getName,FileInfoDTO::setFilename);
        });

        modelMapper.createTypeMap(ActionConfirm.class,ActionConfirmDTO.class).addMappings(mapper -> {
            mapper.using(actionToIsImage).map(ActionConfirm::getPath,ActionConfirmDTO::setIsImage);
            mapper.using(actionToIsVideo).map(ActionConfirm::getPath,ActionConfirmDTO::setIsVideo);
            mapper.using(fileToIdConverter).map(ActionConfirm::getPath,ActionConfirmDTO::setFileId);
            mapper.using(fileToNameConverter).map(ActionConfirm::getPath,ActionConfirmDTO::setFileName);
            mapper.using(fileToDateConverter).map(ActionConfirm::getPath,ActionConfirmDTO::setFileDate);
            mapper.using(fileToSizeConverter).map(ActionConfirm::getPath,ActionConfirmDTO::setFileSize);
            mapper.map(ActionConfirm::confirmed,ActionConfirmDTO::setConfirmed);
        });

        modelMapper.addMappings(sourceMap);
        modelMapper.addMappings(importSourceMap);
        modelMapper.addMappings(preImportSourceMap);
        modelMapper.addMappings(importSource2Map);
        modelMapper.addMappings(importSource3Map);

        return modelMapper;
    }

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
    private String ffmpegCommand;

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

    public String getFfmpegCommand() {
        return ffmpegCommand;
    }

    public void setFfmpegCommand(String ffmpegCommand) {
        this.ffmpegCommand = ffmpegCommand;
    }
}
