package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.dto.PrintSizeDTO;
import com.jbr.middletier.backup.dto.SelectedPrintDTO;
import com.jbr.middletier.backup.manager.PrintManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Prints", description = "Photo print selection and generation")
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

    @Operation(summary = "Remove a photo from the print selection list")
    @PostMapping(path="/prints/unselect")
    public Integer unprint(@RequestBody Integer id) {
        return printManager.unselect(id);
    }

    @Operation(summary = "Remove a specific size/type print for a photo")
    @DeleteMapping(path="/prints/{fileId}/{sizeId}")
    public Integer unprintOne(@PathVariable Integer fileId, @PathVariable Integer sizeId) {
        return printManager.unselectOne(fileId, sizeId);
    }

    @GetMapping(path="/prints")
    public List<SelectedPrintDTO> prints() {
        return printManager.getPrints();
    }

    @DeleteMapping(path="/prints")
    public List<Integer> deletePrints() {
        return printManager.deletePrints();
    }

    @Operation(summary = "Generate the print list from the current selected photos")
    @PostMapping(path="/prints/generate")
    public OkStatus doSomething() {
        LOG.info("Get a list of the P files");

        printManager.gatherList();

        return OkStatus.getOkStatus();
    }
}
