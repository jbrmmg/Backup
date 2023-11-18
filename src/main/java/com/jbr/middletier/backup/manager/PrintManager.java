package com.jbr.middletier.backup.manager;

import com.jbr.middletier.backup.data.PrintSize;
import com.jbr.middletier.backup.dataaccess.PrintRepository;
import com.jbr.middletier.backup.dataaccess.PrintSizeRepository;
import com.jbr.middletier.backup.dto.PrintSizeDTO;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PrintManager {
    private static final Logger LOG = LoggerFactory.getLogger(PrintManager.class);

    private final PrintRepository printRepository;
    private final PrintSizeRepository printSizeRepository;
    private final ModelMapper modelMapper;

    public PrintManager(PrintRepository printRepository, PrintSizeRepository printSizeRepository, ModelMapper modelMapper) {
        this.printRepository = printRepository;
        this.printSizeRepository = printSizeRepository;
        this.modelMapper = modelMapper;
    }

    public List<PrintSizeDTO> getPrintSizes() {
        List<PrintSizeDTO> result = new ArrayList<>();

        for(PrintSize next : printSizeRepository.findAll()) {
            result.add(modelMapper.map(next,PrintSizeDTO.class));
        }

        return result;
    }
}
