package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.ActionConfirm;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ActionConfirmRepository extends CrudRepository<ActionConfirm, String>, JpaSpecificationExecutor {
    List<ActionConfirm> findByPathAndAction(String path, String action);
}
