package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.config.ApplicationProperties;
import com.jbr.middletier.backup.data.DbLog;
import com.jbr.middletier.backup.data.DbLogType;
import com.jbr.middletier.backup.dataaccess.DbLogRepository;
import com.jbr.middletier.backup.dto.DbLogDTO;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DbLoggingManager {
    private static final Logger LOG = LoggerFactory.getLogger(DbLoggingManager.class);

    private final ApplicationProperties applicationProperties;
    private final DbLogRepository dbLogRepository;
    private final EnumMap<DbLogType, List<String>> messageCache;
    private final ModelMapper modelMapper;

    @PostConstruct
    public void initialise() {
        info("Starting up");
    }

    public DbLoggingManager(ApplicationProperties applicationProperties, DbLogRepository dbLogRepository, ModelMapper modelMapper) {
        this.applicationProperties = applicationProperties;
        this.dbLogRepository = dbLogRepository;
        this.modelMapper = modelMapper;
        this.messageCache =  new EnumMap<>(DbLogType.class);
    }

    public List<String> getMessageCache(DbLogType type) {
        if(!this.messageCache.containsKey(type)) {
            return new ArrayList<>();
        }

        return this.messageCache.get(type);
    }

    public void clearMessageCache() {
        for(Map.Entry<DbLogType, List<String>> nextEntry : this.messageCache.entrySet()) {
            nextEntry.getValue().clear();
        }
    }

    private void cacheMessageIfRequired(DbLogType type, String message) {
        if(!this.applicationProperties.getCacheWebLog()) {
            return;
        }

        List<String> messages;
        if (!messageCache.containsKey(type)) {
            messages = new ArrayList<>();
            messageCache.put(type, messages);
        } else {
            messages = messageCache.get(type);
        }
        messages.add(message);
    }

    public void debug(String message) {
        LOG.debug(message);
        cacheMessageIfRequired(DbLogType.DLT_DEBUG,message);
        dbLogRepository.save(new DbLog(DbLogType.DLT_DEBUG,message));
    }

    public void info(String message) {
        LOG.info(message);
        cacheMessageIfRequired(DbLogType.DLT_INFO,message);
        dbLogRepository.save(new DbLog(DbLogType.DLT_INFO,message));
    }

    public void warn(String message) {
        LOG.warn(message);
        cacheMessageIfRequired(DbLogType.DLT_WARNING,message);
        dbLogRepository.save(new DbLog(DbLogType.DLT_WARNING,message));
    }

    public void error(String message) {
        LOG.error(message);
        cacheMessageIfRequired(DbLogType.DLT_ERROR,message);
        dbLogRepository.save(new DbLog(DbLogType.DLT_ERROR,message));
    }

    public List<DbLog> findDbLogs() {
        List<DbLog> result = new ArrayList<>();

        dbLogRepository.findAllByOrderByDateAsc().forEach(result::add);

        LOG.info("Messages returned {}", result.size());
        return result;
    }

    public void removeOldLogs() {
        LOG.info("Remove old logs");

        LocalDateTime oldest = LocalDateTime.now();
        oldest = oldest.minusDays(2);

        int count = 0;
        for(DbLog next : findDbLogs()) {
            if(next.getDate().isBefore(oldest)) {
                dbLogRepository.delete(next);
                count++;
            }
        }

        LOG.info("Removed {} logs", count);
    }

    public DbLogDTO convertToDTO(DbLog dbLog) {
        return this.modelMapper.map(dbLog,DbLogDTO.class);
    }
}
