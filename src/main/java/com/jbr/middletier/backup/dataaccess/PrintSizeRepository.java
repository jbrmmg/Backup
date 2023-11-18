package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.PrintSize;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface PrintSizeRepository extends CrudRepository<PrintSize, Integer>, JpaSpecificationExecutor<PrintSize> {
}
