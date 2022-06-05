package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.ImportSource;
import com.jbr.middletier.backup.data.Source;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface ImportSourceRepository extends CrudRepository<ImportSource, Integer>, JpaSpecificationExecutor<ImportSource> {
    Iterable<ImportSource> findAllByOrderByIdAsc();
}
