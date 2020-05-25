package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Synchronize;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface SynchronizeRepository extends CrudRepository<Synchronize, Integer>, JpaSpecificationExecutor {
}
