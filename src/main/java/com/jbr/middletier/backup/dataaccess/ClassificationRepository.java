package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Classification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ClassificationRepository extends CrudRepository<Classification, Integer>, JpaSpecificationExecutor<Classification> {
    Iterable<Classification> findAllByOrderByIdAsc();

    List<Classification> findAllByOrderByOrderAsc();
}
