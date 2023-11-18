package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.FileLabel;
import com.jbr.middletier.backup.data.FileLabelId;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface FileLabelRepository extends CrudRepository<FileLabel, FileLabelId>, JpaSpecificationExecutor<FileLabel> {
}
