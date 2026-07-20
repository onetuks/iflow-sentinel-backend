package com.onetuks.iflow_sentinel.parser.iflow;

import com.onetuks.iflow_sentinel.parser.model.ExceptionSubprocess;
import com.onetuks.iflow_sentinel.parser.xml.XmlDom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * errorEventDefinition을 가진 bpmn2:subProcess를 문서 전체에서 찾는다.
 * 배열이 비어 있으면 예외 처리 서브프로세스가 없다는 뜻이며(설계서 5.3.7), Parser는 이 사실만 산출한다.
 */
public final class ExceptionSubprocessParser {

    private ExceptionSubprocessParser() {
    }

    public static List<ExceptionSubprocess> parse(Document document) {
        List<ExceptionSubprocess> result = new ArrayList<>();
        for (Element subProcess : XmlDom.allByLocalName(document.getDocumentElement(), "subProcess")) {
            if (!XmlDom.allByLocalName(subProcess, "errorEventDefinition").isEmpty()) {
                String id = XmlDom.attr(subProcess, "id");
                String name = XmlDom.attr(subProcess, "name");
                String processId = XmlDom.ancestorIdByLocalName(subProcess, "process");
                result.add(new ExceptionSubprocess(id, name, processId));
            }
        }
        return result;
    }
}
