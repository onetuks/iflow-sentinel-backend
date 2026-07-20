package com.onetuks.iflow_sentinel.parser;

import com.onetuks.iflow_sentinel.parser.model.ParsedModel;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * 수동 검증용 임시 엔드포인트. Connector(SAP IS 연동)가 아직 없으므로,
 * ZIP 파일을 직접 업로드해 Parser의 출력을 눈으로 확인하기 위한 용도다.
 * Connector 완성 후에는 이 엔드포인트의 필요성을 재검토한다.
 */
@RestController
public class ParserController {

    private final ParserFacade parserFacade;

    public ParserController(ParserFacade parserFacade) {
        this.parserFacade = parserFacade;
    }

    @PostMapping("/api/parser/test")
    public ParsedModel parseUploadedArtifact(@RequestParam("file") MultipartFile file) {
        try {
            return parserFacade.parse(file.getInputStream());
        } catch (IOException e) {
            throw new UncheckedIOException("업로드된 파일을 읽을 수 없습니다.", e);
        }
    }
}
