package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Hardware;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface HardwareRepository  extends CrudRepository<Hardware, String>, JpaSpecificationExecutor {
}
