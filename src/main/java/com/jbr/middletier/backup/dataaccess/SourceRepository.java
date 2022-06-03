package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Location;
import com.jbr.middletier.backup.data.Source;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface SourceRepository extends CrudRepository<Source, Integer>, JpaSpecificationExecutor<Source> {
    Iterable<Source> findAllByOrderByIdAsc();
}
