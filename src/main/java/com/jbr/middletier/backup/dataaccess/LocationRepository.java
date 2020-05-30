package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Location;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends CrudRepository<Location, Integer>, JpaSpecificationExecutor {
}
