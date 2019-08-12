package com.jbr.middletier.backup.dataaccess;

import com.jbr.middletier.backup.data.Backup;
import org.springframework.data.jpa.domain.Specification;

/**
 * Created by jason on 11/02/17.
 */
public class BackupSpecifications {
    public static Specification<Backup> backupsBetweenTimes(int fromTime, int toTime) {
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.between(root.get("time"), fromTime, toTime);
    }
}
