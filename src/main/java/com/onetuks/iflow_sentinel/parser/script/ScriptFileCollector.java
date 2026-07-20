package com.onetuks.iflow_sentinel.parser.script;

import com.onetuks.iflow_sentinel.parser.model.ScriptArtifact;
import com.onetuks.iflow_sentinel.parser.util.ScriptLanguage;
import com.onetuks.iflow_sentinel.parser.zip.ArtifactZipEntry;

import java.nio.charset.StandardCharsets;

/** ZIP 안에서 실제로 발견된 *.groovy/*.js 파일을 읽어 ScriptArtifact로 만든다(isResolved=true, source 포함). */
public final class ScriptFileCollector {

    private ScriptFileCollector() {
    }

    public static ScriptArtifact parse(ArtifactZipEntry entry) {
        String fileName = entry.fileName();
        String source = new String(entry.content(), StandardCharsets.UTF_8);
        return new ScriptArtifact(fileName, ScriptLanguage.fromFileName(fileName), source, true);
    }
}
