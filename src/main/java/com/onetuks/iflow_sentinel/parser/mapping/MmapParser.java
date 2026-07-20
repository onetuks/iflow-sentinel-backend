package com.onetuks.iflow_sentinel.parser.mapping;

import com.onetuks.iflow_sentinel.parser.model.FieldCount;
import com.onetuks.iflow_sentinel.parser.model.FunctionLibraryRef;
import com.onetuks.iflow_sentinel.parser.model.MappingArtifact;
import com.onetuks.iflow_sentinel.parser.model.MappingFunction;
import com.onetuks.iflow_sentinel.parser.model.MessageRef;
import com.onetuks.iflow_sentinel.parser.zip.ArtifactZipEntry;
import com.onetuks.iflow_sentinel.parser.xml.XmlDom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * *.mmap 파일(메시지 매핑 정의)을 파싱한다. lnks의 lnkRole(SOURCE_IFR_MESS/TARGET_IFR_MESS/UsedFuncLib)에서
 * 원본/대상 메시지와 사용된 함수 라이브러리를, brick 요소(트리 깊이 무관 전체 스캔)에서 함수 사용 내역과
 * 필드 개수를 추출한다.
 */
public final class MmapParser {

    private MmapParser() {
    }

    public static MappingArtifact parse(ArtifactZipEntry entry) {
        Document document = XmlDom.parse(entry.content());
        Element root = document.getDocumentElement();

        MessageRef sourceMessage = null;
        MessageRef targetMessage = null;
        List<FunctionLibraryRef> functionLibraries = new ArrayList<>();

        for (Element lnkRole : XmlDom.allByLocalName(root, "lnkRole")) {
            String role = XmlDom.attr(lnkRole, "role");
            List<Element> keys = XmlDom.allByLocalName(lnkRole, "key");
            if (keys.isEmpty()) {
                continue;
            }
            List<String> elems = XmlDom.directChildElements(keys.get(0), "elem").stream()
                    .map(Element::getTextContent)
                    .toList();

            switch (role) {
                case "SOURCE_IFR_MESS" -> sourceMessage = toMessageRef(elems);
                case "TARGET_IFR_MESS" -> targetMessage = toMessageRef(elems);
                case "UsedFuncLib" -> {
                    if (elems.size() >= 2) {
                        functionLibraries.add(new FunctionLibraryRef(elems.get(0), elems.get(1)));
                    }
                }
                default -> {
                    // 스키마에 없는 lnkRole은 무시
                }
            }
        }

        List<MappingFunction> functions = new ArrayList<>();
        int sourceCount = 0;
        int targetCount = 0;
        for (Element brick : XmlDom.allByLocalName(root, "brick")) {
            String type = XmlDom.attr(brick, "type");
            switch (type) {
                case "Func" -> {
                    String fname = XmlDom.attr(brick, "fname");
                    String fns = XmlDom.attr(brick, "fns");
                    boolean standard = "dflt".equals(fns);
                    functions.add(new MappingFunction(fname, standard, standard ? null : fns));
                }
                case "Src" -> sourceCount++;
                case "Dst" -> targetCount++;
                default -> {
                    // 트랜스폼 트리의 다른 brick 타입은 스키마에 없으므로 무시
                }
            }
        }

        String name = deriveName(entry.fileName());
        return new MappingArtifact(
                name, entry.path(), entry.path(),
                sourceMessage, targetMessage,
                functionLibraries, functions,
                new FieldCount(sourceCount, targetCount)
        );
    }

    private static MessageRef toMessageRef(List<String> elems) {
        if (elems.size() < 4) {
            return null;
        }
        return new MessageRef(elems.get(0), elems.get(2), elems.get(3));
    }

    private static String deriveName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }
}
