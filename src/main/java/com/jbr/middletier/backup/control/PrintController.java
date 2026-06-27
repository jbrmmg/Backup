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
@RequestMapping("/api/v1")
public class PrintController {
    private static final Logger LOG = LoggerFactory.getLogger(PrintController.class);

    private final PrintManager printManager;

    public PrintController(PrintManager printManager) {
        this.printManager = printManager;
    }

    @GetMapping(path="/prints/sizes")
    public List<PrintSizeDTO> printSizes() {
        LOG.info("Get print sizes");
        return printManager.getPrintSizes();
    }

    @PostMapping(path="/prints")
    public Integer print(@RequestBody SelectedPrintDTO print) {
        return printManager.select(print);
    }

    @PutMapping(path="/prints")
    public Integer updatePrint(@RequestBody SelectedPrintDTO selected) {
        return printManager.updatePrint(selected);
    }

    @PostMapping(path="/prints/unselect")
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

    @PostMapping(path="/prints/generate")
    public OkStatus doSomething() {
        LOG.info("Get a list of the P files");

        printManager.gatherList();

        return OkStatus.getOkStatus();
    }
}
