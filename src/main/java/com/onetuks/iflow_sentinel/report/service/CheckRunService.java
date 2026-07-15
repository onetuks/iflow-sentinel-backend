package com.onetuks.iflow_sentinel.report.service;

import com.onetuks.iflow_sentinel.report.domain.checkrun.CheckRun;
import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.report.dto.CheckRunCreateRequest;
import com.onetuks.iflow_sentinel.report.dto.CheckRunUpdateRequest;
import com.onetuks.iflow_sentinel.report.persistence.CheckRunJpaRepository;
import com.onetuks.iflow_sentinel.connector.persistence.ProjectJpaRepository;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CheckRunService {

    private final CheckRunJpaRepository checkRunRepository;
    private final ProjectJpaRepository projectRepository;

    @Transactional
    public CheckRun createCheckRun(CheckRunCreateRequest request) {
        Project project = projectRepository.findById(request.projectId())
                .orElseThrow(NoSuchElementException::new);

        CheckRun newCheckRun = CheckRun.builder()
                .project(project)
                .startedAt(request.startedAt())
                .status(request.status())
                .build();

        return checkRunRepository.save(newCheckRun);
    }

    @Transactional
    public CheckRun updateCheckRun(Long id, CheckRunUpdateRequest request) {
        CheckRun checkRun = checkRunRepository.findById(id).orElseThrow(NoSuchElementException::new);
        checkRun.updateStatus(request.status(), request.summary());
        return checkRunRepository.save(checkRun);
    }

    @Transactional(readOnly = true)
    public CheckRun getCheckRunById(Long id) {
        return checkRunRepository.findById(id).orElseThrow(NoSuchElementException::new);
    }

    @Transactional
    public void removeCheckRun(Long id) {
        checkRunRepository.deleteById(id);
    }
}
