package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.MetaData;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface MetaDataRepository extends CrudRepository<MetaData, Integer>, JpaSpecificationExecutor<MetaData> {
}
