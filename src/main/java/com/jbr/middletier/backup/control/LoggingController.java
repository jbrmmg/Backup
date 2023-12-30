package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.DbLogDTO;
import com.jbr.middletier.backup.manager.DbLoggingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/jbr/int/backup")
public class LoggingController {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingController.class);

    private final DbLoggingManager dbLoggingManager;

    @Autowired
    public LoggingController(DbLoggingManager dbLoggingManager) {
        this.dbLoggingManager = dbLoggingManager;
    }

    @GetMapping(path="/log")
    public List<DbLogDTO> getLog() {
        LOG.info("Get the log data");

        List<DbLogDTO> result = new ArrayList<>();

        dbLoggingManager.findDbLogs().forEach(dbLog -> result.add(dbLoggingManager.convertToDTO(dbLog)));

        return result;
    }
}
