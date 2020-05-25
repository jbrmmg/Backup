package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Classification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface ClassificationRepository extends CrudRepository<Classification, Integer>, JpaSpecificationExecutor {
}
