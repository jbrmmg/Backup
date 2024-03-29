package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.dto.PrintSizeDTO;
import com.jbr.middletier.backup.dto.SelectedPrintDTO;
import com.jbr.middletier.backup.manager.PrintManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

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
    public List<PrintSizeDTO> printSizes() {
        LOG.info("Get print sizes");
        return printManager.getPrintSizes();
    }

    @PostMapping(path="/print")
    public Integer print(@RequestBody SelectedPrintDTO print) {
        return printManager.select(print);
    }

    @PutMapping(path="/print")
    public Integer updatePrint(@RequestBody SelectedPrintDTO selected) {
        return printManager.updatePrint(selected);
    }

    @PostMapping(path="/unprint")
    public Integer unprint(@RequestBody Integer id) {
        return printManager.unselect(id);
    }

    @GetMapping(path="/prints")
    public List<SelectedPrintDTO> prints() {
        return printManager.getPrints();
    }

    @DeleteMapping(path="/prints")
    public List<Integer> deletePrints() {
        return printManager.deletePrints();
    }

    @PostMapping(path="/generate")
    public OkStatus doSomething() {
        LOG.info("Get a list of the P files");

        printManager.gatherList();

        return OkStatus.getOkStatus();
    }
}
