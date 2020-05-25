package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Directory;
import com.jbr.middletier.backup.data.Source;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DirectoryRepository extends CrudRepository<Directory, Integer>, JpaSpecificationExecutor {
    Optional<Directory> findBySourceAndPath(Source source, String path);
}
