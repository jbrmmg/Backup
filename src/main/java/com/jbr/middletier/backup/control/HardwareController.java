package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.Hardware;
import com.jbr.middletier.backup.data.OkStatus;
import com.jbr.middletier.backup.dataaccess.HardwareRepository;
import com.jbr.middletier.backup.exception.HardwareAlreadyExistsException;
import com.jbr.middletier.backup.exception.InvalidHardwareIdException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/jbr/ext/hardware")
public class HardwareController {
    final static private Logger LOG = LoggerFactory.getLogger(HardwareController.class);

    final private HardwareRepository hardwareRepository;

    @Autowired
    HardwareController(HardwareRepository hardwareRepository) {
        this.hardwareRepository = hardwareRepository;
    }

    @RequestMapping(path="/byId",method=RequestMethod.GET)
    public @ResponseBody Hardware specificHardware(@RequestParam(value="macAddress", defaultValue="00:00:00:00:00:00") String macAddress) throws InvalidHardwareIdException {
        LOG.info("List hardware.");
        // Check that the item exists.
        Optional<Hardware> storedHardware = hardwareRepository.findById(macAddress);

        if(!storedHardware.isPresent()) {
            throw new InvalidHardwareIdException(macAddress);
        }

        return storedHardware.get();
    }

    @RequestMapping(method=RequestMethod.GET)
    public @ResponseBody Iterable<Hardware> hardware() {
        LOG.info("List hardware.");
        return hardwareRepository.findAll();
    }

    @RequestMapping(method=RequestMethod.PUT)
    public @ResponseBody OkStatus update(@RequestBody Hardware hardware) throws InvalidHardwareIdException {
        LOG.info("Update hardware - " + hardware.getMacAddress());

        // Check that the item exists.
        Optional<Hardware> storedHardware = hardwareRepository.findById(hardware.getMacAddress());

        if(!storedHardware.isPresent()) {
            throw new InvalidHardwareIdException(hardware.getMacAddress());
        }

        storedHardware.get().setIP(hardware.getIP());
        storedHardware.get().setName(hardware.getName());
        storedHardware.get().setReservedIP(hardware.getReservedIP());

        hardwareRepository.save(storedHardware.get());

        return OkStatus.getOkStatus();
    }

    @RequestMapping(method=RequestMethod.POST)
    public @ResponseBody OkStatus create(@RequestBody Hardware hardware) throws HardwareAlreadyExistsException {
        LOG.info("Create hardware - " + hardware.getMacAddress());

        // Check that the item exists.
        Optional<Hardware> storedHardware = hardwareRepository.findById(hardware.getMacAddress());
        if(storedHardware.isPresent()) {
            throw new HardwareAlreadyExistsException(hardware.getMacAddress());
        }

        hardwareRepository.save(hardware);

        return OkStatus.getOkStatus();
    }

    @RequestMapping(method=RequestMethod.DELETE)
    public @ResponseBody OkStatus delete(@RequestBody Hardware hardware) throws InvalidHardwareIdException {
        LOG.info("Delete hardware - " + hardware.getMacAddress());

        // Check that the item exists.
        Optional<Hardware> storedHardware = hardwareRepository.findById(hardware.getMacAddress());

        if(!storedHardware.isPresent()) {
            throw new InvalidHardwareIdException(hardware.getMacAddress());
        }

        hardwareRepository.delete(storedHardware.get());

        return OkStatus.getOkStatus();
    }
}
