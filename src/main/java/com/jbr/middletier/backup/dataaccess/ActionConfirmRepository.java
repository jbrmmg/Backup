package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.ActionConfirm;
import com.jbr.middletier.backup.data.FileInfo;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;
import java.util.List;

public interface ActionConfirmRepository extends CrudRepository<ActionConfirm, Integer>, JpaSpecificationExecutor<ActionConfirm> {
    List<ActionConfirm> findByFileInfoAndAction(FileInfo fileInfo, String action);

    List<ActionConfirm> findByConfirmed(Boolean confirmed);

    List<ActionConfirm> findByConfirmedAndAction(Boolean confirmed, String action);

    // Remove actions
    @Transactional
    @Modifying
    @Query("DELETE FROM ActionConfirm WHERE action=?1 AND confirmed=?2")
    void clearActions(String actionName, boolean confirmed);
}
