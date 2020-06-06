package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.ActionConfirm;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

public interface ActionConfirmRepository extends CrudRepository<ActionConfirm, Integer>, JpaSpecificationExecutor {
    List<ActionConfirm> findByPathAndAction(String path, String action);

    List<ActionConfirm> findByConfirmed(Boolean confirmed);

    // Mark everything as removed.
    @Transactional
    @Modifying
    @Query("DELETE FROM ActionConfirm WHERE action='DELETE_DUP' AND confirmed=0")
    void clearDuplicateDelete();

    // Clear imports.
    @Transactional
    @Modifying
    @Query("DELETE FROM ActionConfirm WHERE action='IMPORT' AND confirmed=0")
    void clearImports();
}
