package com.onetuks.iflow_sentinel.rulemgmt;

import com.onetuks.iflow_sentinel.domain.ruleset.Ruleset;
import com.onetuks.iflow_sentinel.domain.ruleset.RulesetRepository;
import com.onetuks.iflow_sentinel.rulemgmt.dto.ImportRequest;
import com.onetuks.iflow_sentinel.rulemgmt.dto.RulesetRequest;
import com.onetuks.iflow_sentinel.rulemgmt.dto.RulesetResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class RulesetService {

    private final RulesetRepository rulesetRepository;

    public RulesetService(RulesetRepository rulesetRepository) {
        this.rulesetRepository = rulesetRepository;
    }

    public RulesetResponse create(RulesetRequest request) {
        Ruleset ruleset = Ruleset.builder()
                .rulesetKey(request.rulesetKey())
                .version(request.version())
                .description(request.description())
                .build();
        return RulesetResponse.from(rulesetRepository.save(ruleset));
    }

    public List<RulesetResponse> list() {
        return rulesetRepository.findAll().stream().map(RulesetResponse::from).toList();
    }

    public RulesetResponse get(Long id) {
        return RulesetResponse.from(findRuleset(id));
    }

    public RulesetResponse addImport(Long id, ImportRequest request) {
        Ruleset ruleset = findRuleset(id);
        Ruleset imported = findRuleset(request.importedRulesetId());
        ruleset.addImport(imported);
        return RulesetResponse.from(rulesetRepository.save(ruleset));
    }

    private Ruleset findRuleset(Long id) {
        return rulesetRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("룰셋을 찾을 수 없습니다: " + id));
    }
}
