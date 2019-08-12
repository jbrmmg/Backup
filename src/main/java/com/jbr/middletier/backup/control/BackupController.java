package com.jbr.middletier.backup.control;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by jason on 08/02/17.
 */

@Controller
@RequestMapping("/jbr/ext/backup")
public class BackupController {
    final static private Logger LOG = LoggerFactory.getLogger(BackupController.class);

    @SuppressWarnings("SameReturnValue")
    @RequestMapping(method= RequestMethod.GET)
    public @ResponseBody
    String doNothing() {
        LOG.info("Backup.");
        return "OK";
    }
}
