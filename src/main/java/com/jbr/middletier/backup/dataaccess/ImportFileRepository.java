package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.ImportFile;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

public interface ImportFileRepository extends CrudRepository<ImportFile, Integer>, JpaSpecificationExecutor {
    List<ImportFile> findAllByOrderByDate();

    // Clear imports.
    @Transactional
    @Modifying
    @Query("DELETE FROM ImportFile")
    void clearImports();
}
