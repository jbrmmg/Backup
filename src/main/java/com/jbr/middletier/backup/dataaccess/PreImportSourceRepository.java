package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.PreImportSource;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface PreImportSourceRepository extends CrudRepository<PreImportSource, Integer>, JpaSpecificationExecutor<PreImportSource> {
    Iterable<PreImportSource> findAllByOrderByIdAsc();
}
