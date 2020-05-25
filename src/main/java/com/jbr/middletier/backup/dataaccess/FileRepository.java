package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Directory;
import com.jbr.middletier.backup.data.File;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FileRepository extends CrudRepository<File, Integer>, JpaSpecificationExecutor {
    Optional<File> findByDirectoryAndName(Directory directory, String name);
}
