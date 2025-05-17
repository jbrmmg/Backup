package com.jbr.middletier.backup.config;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dto.*;
import lombok.Getter;
import lombok.Setter;
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

        Converter<String,SourceStatusType> stringSourceStatusConverter = new AbstractConverter<>() {
            @Override
            protected SourceStatusType convert(String s) {
                return SourceStatusType.getSourceStatusType(s);
            }
        };

        Converter<SourceStatusType,String> sourceStatusStringConverter = new AbstractConverter<>() {
            @Override
            protected String convert(SourceStatusType s) {
                return s.getTypeName();
            }
        };

        Converter<ActionConfirmType,String> actionConfirmStringConverter = new AbstractConverter<>() {
            @Override
            protected String convert(ActionConfirmType ac) {
                return ac.getTypeName();
            }
        };

        PropertyMap<Source, SourceDTO> sourceMap = new PropertyMap<>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        PropertyMap<ImportSource, ImportSourceDTO> importSourceMap = new PropertyMap<>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
                map().setDestinationId(source.getDestination().getIdAndType().getId());
            }
        };

        PropertyMap<PreImportSource, PreImportSourceDTO> preImportSourceMap = new PropertyMap<>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        PropertyMap<PostImportSource, PostImportSourceDTO> postImportSourceMap = new PropertyMap<>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        PropertyMap<ImportSource, SourceDTO> importSource2Map = new PropertyMap<>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        PropertyMap<PreImportSource, SourceDTO> importSource3Map = new PropertyMap<>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        PropertyMap<PostImportSource, SourceDTO> importSource4Map = new PropertyMap<>() {
            @Override
            protected void configure() {
                map().setId(source.getIdAndType().getId());
            }
        };

        Converter<Optional<FileSystemObjectId>,String> fsoIdTypeConverter = new AbstractConverter<>() {
            @Override
            protected String convert(Optional<FileSystemObjectId> fileSystemObjectId) {
                return fileSystemObjectId.map(systemObjectId -> systemObjectId.getType().getTypeName()).orElse(null);
            }
        };

        Converter<Optional<FileSystemObjectId>,Integer> fsoIdIntegerConverter = new AbstractConverter<>() {
            @Override
            protected Integer convert(Optional<FileSystemObjectId> fileSystemObjectId) {
                return fileSystemObjectId.map(FileSystemObjectId::getId).orElse(null);
            }
        };

        Converter<FileSystemObjectId,Integer> fsoIdIntegerConverter2 = new AbstractConverter<>() {
            @Override
            protected Integer convert(FileSystemObjectId fileSystemObjectId) {
                return fileSystemObjectId.getId();
            }
        };

        Converter<FileInfo,Boolean> actionToIsImage = new AbstractConverter<>() {
            @Override
            protected Boolean convert(FileInfo file) {
                return getIsImageOrVideo(file,true);
            }
        };

        Converter<FileInfo,Boolean> actionToIsVideo = new AbstractConverter<>() {
            @Override
            protected Boolean convert(FileInfo file) {
                return getIsImageOrVideo(file,false);
            }
        };

        Converter<FileInfo,Integer> fileToIdConverter = new AbstractConverter<>() {
            @Override
            protected Integer convert(FileInfo file) {
                if(null == file) {
                    return null;
                }

                return file.getIdAndType().getId();
            }
        };

        Converter<FileInfo,String> fileToNameConverter = new AbstractConverter<>() {
            @Override
            protected String convert(FileInfo file) {
                if(null == file) {
                    return null;
                }

                return file.getName();
            }
        };

        Converter<FileInfo, LocalDateTime> fileToDateConverter = new AbstractConverter<>() {
            @Override
            protected LocalDateTime convert(FileInfo file) {
                if(null == file) {
                    return null;
                }

                return file.getDate();
            }
        };

        Converter<FileInfo,Long> fileToSizeConverter = new AbstractConverter<>() {
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
        modelMapper.createTypeMap(FileInfo.class,ImportFileBaseDTO.class).addMappings(mapper -> mapper.map(FileInfo::getName,ImportFileBaseDTO::setFilename));

        modelMapper.createTypeMap(ImportFile.class,ImportFileDTO.class).addMappings(mapper -> {
            mapper.map(ImportFile::getName,ImportFileDTO::setFilename);
            mapper.using(fsoIdIntegerConverter2).map(ImportFile::getIdAndType,ImportFileDTO::setId);
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
            mapper.using(actionToIsImage).map(ActionConfirm::getPath,ActionConfirmDTO::setImage);
            mapper.using(actionToIsVideo).map(ActionConfirm::getPath,ActionConfirmDTO::setVideo);
            mapper.using(fileToIdConverter).map(ActionConfirm::getPath,ActionConfirmDTO::setFileId);
            mapper.using(fileToNameConverter).map(ActionConfirm::getPath,ActionConfirmDTO::setFileName);
            mapper.using(fileToDateConverter).map(ActionConfirm::getPath,ActionConfirmDTO::setDate);
            mapper.using(fileToSizeConverter).map(ActionConfirm::getPath,ActionConfirmDTO::setSize);
            mapper.map(ActionConfirm::confirmed,ActionConfirmDTO::setConfirmed);
        });

        modelMapper.addMappings(sourceMap);
        modelMapper.addMappings(importSourceMap);
        modelMapper.addMappings(preImportSourceMap);
        modelMapper.addMappings(postImportSourceMap);
        modelMapper.addMappings(importSource2Map);
        modelMapper.addMappings(importSource3Map);
        modelMapper.addMappings(importSource4Map);

        return modelMapper;
    }

    @Getter
    public static class Directory {
        @Setter
        private String name;
        private long days;
        @Setter
        private String zip;
        @Setter
        private String dateFormat;

        public void setDays(int directoryDays) { this.days = directoryDays; }
    }

    @Setter
    @Getter
    public static class Email {
        private String host;
        private String user;
        private String password;
        private String from;
        private String to;
        private Boolean enabled;
        private Integer port;
        private Boolean authenticate;
    }

    @Getter
    private final Directory directory = new Directory();
    @Getter
    private final Email email = new Email();
    @Getter
    @Setter
    private String serviceName;
    @Getter
    @Setter
    private String dbUrl;
    @Getter
    @Setter
    private String dbPassword;
    @Getter
    @Setter
    private String dbUsername;
    @Getter
    @Setter
    private String zipDirectory;
    @Getter
    @Setter
    private String webLogUrl;
    @Setter
    private boolean cacheWebLog;
    @Getter
    @Setter
    private String schedule;
    @Setter
    private boolean enabled;
    @Setter
    @Getter
    private Boolean summaryEnabled;
    @Setter
    @Getter
    private Integer importThreads;
    @Getter
    @Setter
    private String gatherSchedule;
    @Setter
    private boolean gatherEnabled;
    @Getter
    @Setter
    private String reviewDirectory;
    @Getter
    @Setter
    private String dbBackupCommand;
    @Getter
    @Setter
    private Long dbBackupMaxTime;
    @Setter
    @Getter
    private String ffmpegCommand;
    @Setter
    @Getter
    private String vidToImageCommand;
    @Setter
    @Getter
    private String vidToImageLocation;

    public boolean getCacheWebLog() { return this.cacheWebLog; }

    public boolean getEnabled() { return this.enabled; }

    public boolean getGatherEnabled() { return this.gatherEnabled; }
}
