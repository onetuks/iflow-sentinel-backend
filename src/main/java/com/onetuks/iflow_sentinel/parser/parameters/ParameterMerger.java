package com.onetuks.iflow_sentinel.parser.parameters;

import com.onetuks.iflow_sentinel.parser.model.Parameter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * parameters.prop(값)과 parameters.propdef(선언)를 이름 기준으로 병합하고, .iflw 순회 중 수집한
 * {{name}} 참조 위치(referencedBy)를 대조해 isUsed를 계산한다. 한쪽에만 존재하는 이름도 그대로 반영하며
 * (불일치 자체를 문제로 판단하지 않음 — 그 판단은 Rule Engine의 몫), 값/선언 어느 한쪽만으로도 Parameter를 만든다.
 */
public final class ParameterMerger {

    private ParameterMerger() {
    }

    public static List<Parameter> merge(
            Map<String, String> values,
            Map<String, ParametersPropDefParser.ParamDef> defs,
            Map<String, List<String>> referencedBy
    ) {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(values.keySet());
        names.addAll(defs.keySet());

        List<Parameter> parameters = new ArrayList<>();
        for (String name : names) {
            String value = values.get(name);
            ParametersPropDefParser.ParamDef def = defs.get(name);
            List<String> refs = referencedBy.getOrDefault(name, List.of());
            parameters.add(new Parameter(
                    name,
                    value,
                    def == null ? null : def.type(),
                    def != null && def.isRequired(),
                    def == null ? "" : def.description(),
                    refs,
                    !refs.isEmpty()
            ));
        }
        return parameters;
    }
}
