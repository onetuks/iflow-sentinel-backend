package com.onetuks.iflow_sentinel.parser;

import com.onetuks.iflow_sentinel.parser.iflow.IflwParser;
import com.onetuks.iflow_sentinel.parser.manifest.ManifestParser;
import com.onetuks.iflow_sentinel.parser.mapping.MmapParser;
import com.onetuks.iflow_sentinel.parser.metainfo.MetaInfoPropParser;
import com.onetuks.iflow_sentinel.parser.model.ArtifactInfo;
import com.onetuks.iflow_sentinel.parser.model.MappingArtifact;
import com.onetuks.iflow_sentinel.parser.model.ParsedModel;
import com.onetuks.iflow_sentinel.parser.model.SchemaArtifact;
import com.onetuks.iflow_sentinel.parser.model.ScriptArtifact;
import com.onetuks.iflow_sentinel.parser.model.StepNode;
import com.onetuks.iflow_sentinel.parser.parameters.ParameterMerger;
import com.onetuks.iflow_sentinel.parser.parameters.ParametersPropDefParser;
import com.onetuks.iflow_sentinel.parser.parameters.ParametersPropParser;
import com.onetuks.iflow_sentinel.parser.script.ScriptFileCollector;
import com.onetuks.iflow_sentinel.parser.wsdl.WsdlParser;
import com.onetuks.iflow_sentinel.parser.zip.ArtifactZip;
import com.onetuks.iflow_sentinel.parser.zip.ArtifactZipEntry;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Parser의 공개 진입점. 아티팩트 ZIP 바이트 배열을 받아 {@link ParsedModel}로 변환하는 순수 함수다.
 * 어떤 내부 상태도 갖지 않으므로(같은 입력이면 항상 같은 출력), 캐싱은 이 클래스 바깥에 얹을 수 있다(ART-006, 향후).
 */
@Component
public class ParserFacade {

    public ParsedModel parse(byte[] artifactZipBytes) {
        ArtifactZip zip = ArtifactZip.from(artifactZipBytes);

        byte[] manifestBytes = zip.findByExactSuffix("META-INF/MANIFEST.MF")
                .orElseThrow(() -> new ParserException("META-INF/MANIFEST.MF를 찾을 수 없습니다."));
        ManifestParser.ManifestData manifestData = ManifestParser.parse(manifestBytes);

        String description = zip.findByExactSuffix("metainfo.prop")
                .map(MetaInfoPropParser::parseDescription)
                .orElse("");

        ArtifactZipEntry iflwEntry = zip.requireSingleByExtension(".iflw");
        IflwParser.IflwParseResult iflwResult = IflwParser.parse(iflwEntry.content());

        Map<String, String> parameterValues = zip.findByExactSuffix("parameters.prop")
                .map(ParametersPropParser::parse)
                .orElse(Map.of());
        Map<String, ParametersPropDefParser.ParamDef> parameterDefs = zip.findByExactSuffix("parameters.propdef")
                .map(ParametersPropDefParser::parse)
                .orElse(Map.of());

        List<MappingArtifact> mappings = zip.findAllByExtension(".mmap").stream()
                .map(MmapParser::parse)
                .toList();

        List<SchemaArtifact> schemas = zip.findAllByExtension(".wsdl").stream()
                .map(WsdlParser::parse)
                .toList();

        List<ScriptArtifact> scripts = buildScripts(zip, iflwResult.model().steps());

        ArtifactInfo artifact = new ArtifactInfo(
                manifestData.name(),
                manifestData.symbolicName(),
                manifestData.version(),
                manifestData.bundleType(),
                manifestData.runtimeProfile(),
                description,
                manifestData.modifiedAt(),
                manifestData.requiredCapabilities()
        );

        return new ParsedModel(
                1,
                artifact,
                iflwResult.model(),
                ParameterMerger.merge(parameterValues, parameterDefs, iflwResult.parameterReferences()),
                mappings,
                schemas,
                scripts
        );
    }

    public ParsedModel parse(InputStream artifactZipStream) {
        try {
            return parse(artifactZipStream.readAllBytes());
        } catch (IOException e) {
            throw new ParserException("아티팩트 ZIP 스트림을 읽을 수 없습니다.", e);
        }
    }

    /**
     * scripts[]는 (1) ZIP 안에서 실제로 발견된 스크립트 파일과 (2) steps[]가 참조하지만 ZIP에 없는
     * 미해결 참조의 합집합이다(설계서 5.3.10·5.4). 미해결 항목은 isResolved=false, source=null로 남긴다.
     */
    private List<ScriptArtifact> buildScripts(ArtifactZip zip, List<StepNode> steps) {
        List<ScriptArtifact> resolved = zip.findAllByExtension(".groovy", ".js").stream()
                .map(ScriptFileCollector::parse)
                .toList();

        Set<String> resolvedFileNames = new LinkedHashSet<>();
        for (ScriptArtifact script : resolved) {
            resolvedFileNames.add(script.file());
        }

        List<ScriptArtifact> unresolved = new ArrayList<>();
        Set<String> seenUnresolved = new LinkedHashSet<>();
        for (StepNode step : steps) {
            if (step.script() == null) {
                continue;
            }
            String file = step.script().file();
            if (file == null || file.isBlank() || resolvedFileNames.contains(file) || !seenUnresolved.add(file)) {
                continue;
            }
            unresolved.add(new ScriptArtifact(file, step.script().language(), null, false));
        }

        List<ScriptArtifact> all = new ArrayList<>(resolved);
        all.addAll(unresolved);
        return all;
    }
}
