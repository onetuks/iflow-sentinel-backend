package com.onetuks.iflow_sentinel.parser.wsdl;

import com.onetuks.iflow_sentinel.parser.model.SchemaArtifact;
import com.onetuks.iflow_sentinel.parser.zip.ArtifactZipEntry;
import com.onetuks.iflow_sentinel.parser.xml.XmlDom;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

/** *.wsdl 파일에서 인터페이스 스키마(메시지 타입·네임스페이스)를 추출한다. */
public final class WsdlParser {

    private WsdlParser() {
    }

    public static SchemaArtifact parse(ArtifactZipEntry entry) {
        Document document = XmlDom.parse(entry.content());
        Element root = document.getDocumentElement();

        String name = XmlDom.attr(root, "name");
        String targetNamespace = XmlDom.attr(root, "targetNamespace");
        List<String> messageTypes = XmlDom.directChildElements(root, "message").stream()
                .map(message -> XmlDom.attr(message, "name"))
                .toList();

        return new SchemaArtifact(entry.fileName(), name, targetNamespace, messageTypes);
    }
}
