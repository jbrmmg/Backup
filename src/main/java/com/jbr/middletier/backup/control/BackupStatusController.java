package com.jbr.middletier.backup.control;

import com.jbr.middletier.backup.data.BackupJobRun;
import com.jbr.middletier.backup.dataaccess.BackupJobRunRepository;
import com.jbr.middletier.backup.dataaccess.BackupRepository;
import com.jbr.middletier.backup.dto.BackupJobRunDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/jbr/ext/backup/status")
@Tag(name = "Backup Status", description = "Backup job run status and history")
public class BackupStatusController {

    private final BackupRepository backupRepository;
    private final BackupJobRunRepository backupJobRunRepository;

    @Autowired
    public BackupStatusController(BackupRepository backupRepository,
                                  BackupJobRunRepository backupJobRunRepository) {
        this.backupRepository = backupRepository;
        this.backupJobRunRepository = backupJobRunRepository;
    }

    private BackupJobRunDTO toDTO(BackupJobRun run) {
        BackupJobRunDTO dto = new BackupJobRunDTO();
        dto.setBackupId(run.getBackupId());
        dto.setStartedAt(run.getStartedAt());
        dto.setFinishedAt(run.getFinishedAt());
        dto.setStatus(run.getStatus());
        dto.setMessage(run.getMessage());
        return dto;
    }

    @GetMapping("/jobs")
    public List<BackupJobRunDTO> latestJobRuns() {
        List<BackupJobRunDTO> result = new ArrayList<>();
        backupRepository.findAllByOrderByTimeAsc().forEach(backup -> {
            List<BackupJobRun> runs = backupJobRunRepository.findByBackupIdOrderByStartedAtDesc(
                    backup.getId(), PageRequest.of(0, 1));
            if (!runs.isEmpty()) {
                result.add(toDTO(runs.get(0)));
            }
        });
        return result;
    }

    @GetMapping("/jobs/{backupId}")
    public List<BackupJobRunDTO> jobHistory(@PathVariable String backupId,
                                            @RequestParam(defaultValue = "10") int limit) {
        return backupJobRunRepository
                .findByBackupIdOrderByStartedAtDesc(backupId, PageRequest.of(0, limit))
                .stream()
                .map(this::toDTO)
                .toList();
    }
}
