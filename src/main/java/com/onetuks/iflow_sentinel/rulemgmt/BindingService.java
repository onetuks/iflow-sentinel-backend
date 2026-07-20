package com.onetuks.iflow_sentinel.rulemgmt;

import com.onetuks.iflow_sentinel.connector.domain.project.Project;
import com.onetuks.iflow_sentinel.connector.domain.project.ProjectRepository;
import com.onetuks.iflow_sentinel.domain.binding.Binding;
import com.onetuks.iflow_sentinel.domain.binding.BindingOverride;
import com.onetuks.iflow_sentinel.domain.binding.BindingOverrideRepository;
import com.onetuks.iflow_sentinel.domain.binding.BindingRepository;
import com.onetuks.iflow_sentinel.domain.rule.Rule;
import com.onetuks.iflow_sentinel.domain.rule.RuleRepository;
import com.onetuks.iflow_sentinel.domain.ruleset.Ruleset;
import com.onetuks.iflow_sentinel.domain.ruleset.RulesetRepository;
import com.onetuks.iflow_sentinel.rulemgmt.dto.BindingOverrideRequest;
import com.onetuks.iflow_sentinel.rulemgmt.dto.BindingOverrideResponse;
import com.onetuks.iflow_sentinel.rulemgmt.dto.BindingRequest;
import com.onetuks.iflow_sentinel.rulemgmt.dto.BindingResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class BindingService {

        private final ProjectRepository projectRepository;
        private final RulesetRepository rulesetRepository;
        private final RuleRepository ruleRepository;
        private final BindingRepository bindingRepository;
        private final BindingOverrideRepository bindingOverrideRepository;

        public BindingService(
                        ProjectRepository projectRepository,
                        RulesetRepository rulesetRepository,
                        RuleRepository ruleRepository,
                        BindingRepository bindingRepository,
                        BindingOverrideRepository bindingOverrideRepository) {
                this.projectRepository = projectRepository;
                this.rulesetRepository = rulesetRepository;
                this.ruleRepository = ruleRepository;
                this.bindingRepository = bindingRepository;
                this.bindingOverrideRepository = bindingOverrideRepository;
        }

        public BindingResponse create(Long projectId, BindingRequest request) {
                Project project = projectRepository.findById(projectId)
                                .orElseThrow(() -> new NoSuchElementException("프로젝트를 찾을 수 없습니다: " + projectId));
                Ruleset ruleset = rulesetRepository.findById(request.rulesetId())
                                .orElseThrow(() -> new NoSuchElementException("룰셋을 찾을 수 없습니다: " + request.rulesetId()));

                Binding binding = Binding.builder().project(project).ruleset(ruleset).build();
                return BindingResponse.from(bindingRepository.save(binding));
        }

        public List<BindingResponse> list(Long projectId) {
                return bindingRepository.findByProjectId(projectId).stream().map(BindingResponse::from).toList();
        }

        public BindingOverrideResponse addOverride(Long bindingId, BindingOverrideRequest request) {
                Binding binding = bindingRepository.findById(bindingId)
                                .orElseThrow(() -> new NoSuchElementException("바인딩을 찾을 수 없습니다: " + bindingId));
                Rule rule = ruleRepository.findById(request.ruleId())
                                .orElseThrow(() -> new NoSuchElementException("규칙을 찾을 수 없습니다: " + request.ruleId()));

                BindingOverride override = BindingOverride.builder()
                                .binding(binding)
                                .rule(rule)
                                .overriddenSeverity(request.overriddenSeverity())
                                .overriddenEnabled(request.overriddenEnabled())
                                .build();
                return BindingOverrideResponse.from(bindingOverrideRepository.save(override));
        }
}
