package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.PostImportSource;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface PostImportSourceRepository extends CrudRepository<PostImportSource, Integer>, JpaSpecificationExecutor<PostImportSource> {
    Iterable<PostImportSource> findAllByOrderByIdAsc();
}
