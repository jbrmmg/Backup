package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Hardware;
import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.dataaccess.HardwareRepository;
import com.jbr.middletier.backup.dto.HardwareDTO;
import com.jbr.middletier.backup.exception.HardwareAlreadyExistsException;
import com.jbr.middletier.backup.exception.InvalidHardwareIdException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Optional;

@RestController
@RequestMapping("/jbr/ext/hardware")
public class HardwareController {
    private static final Logger LOG = LoggerFactory.getLogger(HardwareController.class);

    private final HardwareRepository hardwareRepository;
    private final ModelMapper modelMapper;

    @Contract(pure = true)
    @Autowired
    HardwareController(HardwareRepository hardwareRepository, ModelMapper modelMapper) {
        this.hardwareRepository = hardwareRepository;
        this.modelMapper = modelMapper;
    }

    @GetMapping(path="/byId")
    public HardwareDTO specificHardware(@RequestParam(value="macAddress", defaultValue="00:00:00:00:00:00") String macAddress) throws InvalidHardwareIdException {
        LOG.info("List hardware.");
        // Check that the item exists.
        Optional<Hardware> storedHardware = hardwareRepository.findById(macAddress);

        if(storedHardware.isEmpty()) {
            throw new InvalidHardwareIdException(macAddress);
        }

        return modelMapper.map(storedHardware.get(),HardwareDTO.class);
    }

    @GetMapping()
    public Iterable<Hardware> hardware() {
        LOG.info("List hardware.");
        return hardwareRepository.findAllByOrderByMacAddressAsc();
    }

    @PutMapping()
    public OkStatus update(@NotNull @RequestBody HardwareDTO hardware) throws InvalidHardwareIdException {
        // Check that the item exists.
        Optional<Hardware> storedHardware = hardwareRepository.findById(hardware.getMacAddress());

        if(storedHardware.isEmpty()) {
            LOG.warn("Cannot find hardware specified.");
            throw new InvalidHardwareIdException(hardware.getMacAddress());
        }

        LOG.info("Update hardware {}", storedHardware.get().getMacAddress());
        hardwareRepository.save(modelMapper.map(hardware, Hardware.class));
        return OkStatus.getOkStatus();
    }

    @PostMapping()
    public OkStatus create(@NotNull @RequestBody HardwareDTO hardware) throws HardwareAlreadyExistsException {
        // Check that the item exists.
        Optional<Hardware> storedHardware = hardwareRepository.findById(hardware.getMacAddress());
        if(storedHardware.isPresent()) {
            LOG.warn("Specified hardware already exists {}", storedHardware.get().getMacAddress());
            throw new HardwareAlreadyExistsException(hardware.getMacAddress());
        }

        Hardware created = hardwareRepository.save(modelMapper.map(hardware, Hardware.class));
        LOG.info("Created new hardware {}", created.getMacAddress());

        return OkStatus.getOkStatus();
    }

    @DeleteMapping()
    public OkStatus delete(@NotNull @RequestBody HardwareDTO hardware) throws InvalidHardwareIdException {
        // Check that the item exists.
        Optional<Hardware> storedHardware = hardwareRepository.findById(hardware.getMacAddress());

        if(storedHardware.isEmpty()) {
            LOG.info("Specified hardware is not in the database.");
            throw new InvalidHardwareIdException(hardware.getMacAddress());
        }

        LOG.info("Delete hardware {}", storedHardware.get().getMacAddress());
        hardwareRepository.delete(storedHardware.get());

        return OkStatus.getOkStatus();
    }
}
