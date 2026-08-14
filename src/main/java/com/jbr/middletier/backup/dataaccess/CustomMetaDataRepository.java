package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.CustomMetaData;
import org.springframework.data.repository.CrudRepository;

public interface CustomMetaDataRepository extends CrudRepository<CustomMetaData, Integer> {
}
