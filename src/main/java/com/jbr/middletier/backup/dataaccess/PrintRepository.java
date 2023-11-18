package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Print;
import com.jbr.middletier.backup.data.PrintId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface PrintRepository extends CrudRepository<Print, PrintId>, JpaSpecificationExecutor<Print> {
}
