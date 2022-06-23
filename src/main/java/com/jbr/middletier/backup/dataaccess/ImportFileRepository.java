package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.ImportFile;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface ImportFileRepository extends CrudRepository<ImportFile, Integer>, JpaSpecificationExecutor<ImportFile> {
    Iterable<ImportFile> findAllByOrderByIdAsc();
}
