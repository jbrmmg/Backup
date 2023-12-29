package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.*;
import com.jbr.middletier.backup.dataaccess.PrintRepository;
import com.jbr.middletier.backup.dataaccess.PrintSizeRepository;
import com.jbr.middletier.backup.dto.PrintSizeDTO;
import com.jbr.middletier.backup.dto.SelectedPrintDTO;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class PrintManager {
    private static final Logger LOG = LoggerFactory.getLogger(PrintManager.class);

    private static final int DEFAULT_PRINT_SIZE_ID = 12;
    private static final String PRINT_FLAG = "P";

    private final PrintRepository printRepository;
    private final PrintSizeRepository printSizeRepository;
    private final FileSystemObjectManager fileSystemObjectManager;
    private final ModelMapper modelMapper;
    private final List<PrintSizeDTO> printSizes;

    public PrintManager(PrintRepository printRepository, PrintSizeRepository printSizeRepository, FileSystemObjectManager fileSystemObjectManager, ModelMapper modelMapper) {
        this.printRepository = printRepository;
        this.printSizeRepository = printSizeRepository;
        this.fileSystemObjectManager = fileSystemObjectManager;
        this.modelMapper = modelMapper;
        this.printSizes = new ArrayList<>();
    }

    public List<PrintSizeDTO> getPrintSizes() {
        // If they already loaded then returnn the list.
        if(!printSizes.isEmpty()) {
            return printSizes;
        }

        // Populate the list and return it.
        for(PrintSize next : printSizeRepository.findAll()) {
            printSizes.add(modelMapper.map(next,PrintSizeDTO.class));
        }

        return printSizes;
    }

    public PrintSizeDTO getPrintSize(Integer id) {
        // Find the size by id.
        for(PrintSizeDTO next : getPrintSizes()) {
            if(next.getId().equals(id)) {
                return next;
            }
        }

        return null;
    }

    public Integer select(Integer id) {
        // Find the file.
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        // Get the default size
        PrintSizeDTO defaultSize = getPrintSize(DEFAULT_PRINT_SIZE_ID);
        if(defaultSize == null) {
            LOG.error("Default print size is missing (select for print)");
        }

        if(file.isPresent() && defaultSize != null) {
            // Create a print
            Print newPrint = new Print();
            PrintId newPrintId = new PrintId();
            newPrintId.setFileId(id);
            newPrintId.setSizeId(defaultSize.getId());
            newPrint.setId(newPrintId);
            newPrint.setBorder(false);
            newPrint.setBlackWhite(false);

            printRepository.save(newPrint);

            return id;
        }

        return null;
    }

    private void removePrintRow(int fileId) {
        for(Print next : printRepository.findAll()) {
            if(next.getId().getFileId() == fileId) {
                printRepository.delete(next);
                break;
            }
        }
    }

    public Integer unselect(Integer id) {
        // Find the file.
        Optional<FileSystemObject> file = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(id,FileSystemObjectType.FSO_FILE));

        if(file.isPresent()) {
            // Remove the legacy flag.
            fileSystemObjectManager.clearFlags(file.get());

            // If any print row exists remove it.
            removePrintRow(id);

            return id;
        }

        return null;
    }

    public List<SelectedPrintDTO> getPrints() {
        List<SelectedPrintDTO> result = new ArrayList<>();

        // Get the print rows.
        for(Print next : printRepository.findAll()) {
            SelectedPrintDTO nextPrint = new SelectedPrintDTO();
            nextPrint.setFileId(next.getId().getFileId());
            nextPrint.setSizeId(next.getId().getSizeId());
            nextPrint.setBorder(next.getBorder());
            nextPrint.setBlackWhite(next.getBlackWhite());

            // Set the print size name.
            for(PrintSizeDTO nextSize : getPrintSizes()) {
                if(nextPrint.getSizeId() == nextSize.getId()) {
                    nextPrint.setSizeName(nextSize.getName());
                }
            }

            // Set the file name.
            Optional<FileSystemObject> fso = fileSystemObjectManager.findFileSystemObject(new FileSystemObjectId(next.getId().getFileId(),FileSystemObjectType.FSO_FILE));
            fso.ifPresent(fileSystemObject -> nextPrint.setFileName(fileSystemObject.getName()));

            result.add(nextPrint);
        }

        // Get the default size (id = 12)
        PrintSizeDTO defaultSize = getPrintSize(DEFAULT_PRINT_SIZE_ID);

        // Check to see if any files have a P flag (legacy).
        for(FileInfo next : fileSystemObjectManager.getFilesByFlag(PRINT_FLAG)) {
            // Is this already in the list?
            boolean already = false;
            for(SelectedPrintDTO existing : result) {
                if(existing.getFileId() == next.getIdAndType().getId()) {
                    already = true;
                    break;
                }
            }

            if(defaultSize == null) {
                LOG.error("Default print size is missing (legacy print)");
            }

            if(!already && defaultSize != null) {
                SelectedPrintDTO nextPrint = new SelectedPrintDTO();
                nextPrint.setFileId(next.getIdAndType().getId());
                nextPrint.setSizeName(defaultSize.getName());
                nextPrint.setSizeId(defaultSize.getId());
                nextPrint.setFileName(next.getName());
                nextPrint.setBorder(false);
                nextPrint.setBlackWhite(false);

                result.add(nextPrint);
            }
        }

        return result;
    }

    public Integer updatePrint(SelectedPrintDTO print) {
        // Update the print
        LOG.info("Update print details - {} {} {} {} {}", print.getFileId(), print.getSizeId(), print.getSizeName(), print.getBlackWhite(), print.getBorder());

        // Delete if exists.
        removePrintRow(print.getFileId());

        // Create a print.
        Print newPrint = new Print();
        PrintId newPrintId = new PrintId();
        newPrintId.setFileId(print.getFileId());
        newPrintId.setSizeId(print.getSizeId());
        newPrint.setId(newPrintId);
        newPrint.setBlackWhite(print.getBlackWhite());
        newPrint.setBorder(print.getBorder());

        printRepository.save(newPrint);

        return print.getFileId();
    }

    public List<Integer> deletePrints() {
        List<Integer> result = new ArrayList<>();

        // Remove the print row.
        for(Print next : printRepository.findAll()) {
            if(!result.contains(next.getId().getFileId())) {
                result.add(next.getId().getFileId());
            }
        }

        printRepository.deleteAll();

        // Legacy
        for(FileInfo next : fileSystemObjectManager.getFilesByFlag(PRINT_FLAG)) {
            next.setFlags(null);

            if(!result.contains(next.getIdAndType().getId())) {
                result.add(next.getIdAndType().getId());
            }

            fileSystemObjectManager.save(next);
        }

        return result;
    }

    public void gatherList() {
        // Find the P files.
        for(FileInfo next : fileSystemObjectManager.getFilesByFlag(PRINT_FLAG)) {
            File printFile = fileSystemObjectManager.getFile(next);
            LOG.info("cp " + printFile.getAbsolutePath() + " /media/jason/6263-3935/" + next.getName());
        }
    }
}
