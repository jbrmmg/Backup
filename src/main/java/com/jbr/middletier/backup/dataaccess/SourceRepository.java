package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Source;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import javax.transaction.Transactional;

public interface SourceRepository extends CrudRepository<Source, Integer>, JpaSpecificationExecutor {
}
