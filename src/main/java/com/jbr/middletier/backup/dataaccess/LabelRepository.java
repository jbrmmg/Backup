package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Label;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

public interface LabelRepository extends CrudRepository<Label, Integer>, JpaSpecificationExecutor<Label> {
}
