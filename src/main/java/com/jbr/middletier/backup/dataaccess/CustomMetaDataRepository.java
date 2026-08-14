package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.CustomMetaData;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CustomMetaDataRepository extends CrudRepository<CustomMetaData, Integer> {
    List<CustomMetaData> findByOriginalMd5(String originalMd5);
}
