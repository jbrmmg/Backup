package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.dto.PrintSizeDTO;
import com.jbr.middletier.backup.manager.PrintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/jbr/int/backup")
public class PrintController {
    private static final Logger LOG = LoggerFactory.getLogger(PrintController.class);

    private final PrintManager printManager;

    public PrintController(PrintManager printManager) {
        this.printManager = printManager;
    }

    @GetMapping(path="/print-size")
    public @ResponseBody List<PrintSizeDTO> printSizes() { return printManager.getPrintSizes(); }
}
