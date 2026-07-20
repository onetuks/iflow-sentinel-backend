package com.onetuks.iflow_sentinel.connector;

import com.onetuks.iflow_sentinel.domain.artifact.Artifact;
import org.springframework.stereotype.Service;

/** ART-003: 선택한 iFlow의 design-time 아티팩트(ZIP)를 다운로드한다. Run(CheckRun)에서 사용. */
@Service
public class ArtifactDownloadService {

    private final SapODataClient odataClient;

    public ArtifactDownloadService(SapODataClient odataClient) {
        this.odataClient = odataClient;
    }

    public byte[] downloadZip(Artifact artifact) {
        String relativePath = "/IntegrationDesigntimeArtifacts(Id='" + artifact.getSapArtifactId()
                + "',Version='" + artifact.getVersion() + "')/$value";
        return odataClient.getBinary(artifact.getIntegrationPackage().getTenant(), relativePath);
    }
}
