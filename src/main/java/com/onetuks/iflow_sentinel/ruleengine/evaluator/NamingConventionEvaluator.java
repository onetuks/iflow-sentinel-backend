package com.onetuks.iflow_sentinel.ruleengine.evaluator;

import com.onetuks.iflow_sentinel.domain.rule.RuleType;
import com.onetuks.iflow_sentinel.parser.model.ParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.ArtifactParsedModel;
import com.onetuks.iflow_sentinel.ruleengine.EffectiveRule;
import com.onetuks.iflow_sentinel.ruleengine.FindingResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * target.element(participant/channel/step) [+target.role]로 대상을 골라, 이름이
 * params.prefix[](접두어 중 하나로 시작) 또는 params.pattern(정규식)을 만족하지 않으면 위반으로 본다.
 */
@Component
public class NamingConventionEvaluator implements RuleTypeEvaluator {

    @Override
    public RuleType supports() {
        return RuleType.NAMING_CONVENTION;
    }

    @Override
    public List<FindingResult> evaluate(EffectiveRule effectiveRule, List<ArtifactParsedModel> models) {
        Map<String, Object> target = effectiveRule.rule().getTarget();
        Map<String, Object> params = effectiveRule.rule().getParams();
        String element = RuleParams.string(target, "element");
        String role = RuleParams.string(target, "role");
        List<String> prefixes = RuleParams.stringList(params, "prefix");
        String pattern = RuleParams.string(params, "pattern");

        List<FindingResult> findings = new ArrayList<>();
        for (ArtifactParsedModel model : models) {
            for (NamedElement candidate : collectCandidates(model.parsedModel(), element, role)) {
                if (!satisfiesNaming(candidate.name(), prefixes, pattern)) {
                    findings.add(new FindingResult(
                            model.artifact(), effectiveRule.rule(), effectiveRule.severity(),
                            element + ":" + candidate.id(), effectiveRule.rule().getMessage()
                    ));
                }
            }
        }
        return findings;
    }

    private static boolean satisfiesNaming(String name, List<String> prefixes, String pattern) {
        if (name == null) {
            return true;
        }
        boolean prefixOk = prefixes.isEmpty() || prefixes.stream().anyMatch(name::startsWith);
        boolean patternOk = pattern == null || pattern.isBlank() || name.matches(pattern);
        return prefixOk && patternOk;
    }

    private static List<NamedElement> collectCandidates(ParsedModel parsedModel, String element, String role) {
        if ("participant".equals(element)) {
            return parsedModel.iflow().participants().stream()
                    .filter(p -> role == null || role.equals(p.role()))
                    .map(p -> new NamedElement(p.id(), p.name()))
                    .toList();
        }
        if ("channel".equals(element)) {
            return parsedModel.iflow().channels().stream()
                    .map(c -> new NamedElement(c.id(), c.name()))
                    .toList();
        }
        if ("step".equals(element)) {
            return parsedModel.iflow().steps().stream()
                    .map(s -> new NamedElement(s.id(), s.name()))
                    .toList();
        }
        return List.of();
    }

    private record NamedElement(String id, String name) {
    }
}
