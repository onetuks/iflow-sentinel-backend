package com.onetuks.iflow_sentinel.reprocess.controller;

import com.onetuks.iflow_sentinel.reprocess.domain.ReprocessSupportType;
import com.onetuks.iflow_sentinel.reprocess.domain.StorageType;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageBodyResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessRequest;
import com.onetuks.iflow_sentinel.reprocess.dto.MessageReprocessResult;
import com.onetuks.iflow_sentinel.reprocess.dto.MplFailureResponse;
import com.onetuks.iflow_sentinel.reprocess.dto.StorageMappingDto;
import com.onetuks.iflow_sentinel.reprocess.service.MessageReprocessService;
import com.onetuks.iflow_sentinel.reprocess.service.StorageMappingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reprocess")
public class MessageReprocessController {

    private final MessageReprocessService messageReprocessService;
    private final StorageMappingService storageMappingService;

    public MessageReprocessController(MessageReprocessService messageReprocessService,
                                       StorageMappingService storageMappingService) {
        this.messageReprocessService = messageReprocessService;
        this.storageMappingService = storageMappingService;
    }

    @GetMapping("/artifacts/{artifactId}/support-type")
    public ResponseEntity<ReprocessSupportType> getReprocessSupportType(@PathVariable Long artifactId) {
        ReprocessSupportType supportType = messageReprocessService.getReprocessSupportType(artifactId);
        return ResponseEntity.ok(supportType);
    }

    @GetMapping("/mpl-failures")
    public ResponseEntity<List<MplFailureResponse>> getMplFailures(
            @RequestParam Long tenantId,
            @RequestParam(required = false) Long artifactId,
            @RequestParam(defaultValue = "20") int top) {
        List<MplFailureResponse> failures = messageReprocessService.getMplFailures(tenantId, artifactId, top);
        return ResponseEntity.ok(failures);
    }

    @GetMapping("/messages/{messageId}/body")
    public ResponseEntity<MessageBodyResponse> getMessageBody(
            @PathVariable String messageId,
            @RequestParam Long tenantId,
            @RequestParam Long artifactId,
            @RequestParam StorageType storageType) {
        MessageBodyResponse response = messageReprocessService.getMessageBody(tenantId, artifactId, messageId, storageType);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/execute")
    public ResponseEntity<MessageReprocessResult> executeReprocess(@RequestBody MessageReprocessRequest request) {
        MessageReprocessResult result = messageReprocessService.reprocessMessage(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/storage-mappings")
    public ResponseEntity<List<StorageMappingDto>> getStorageMappings(
            @RequestParam Long tenantId,
            @RequestParam Long artifactId) {
        List<StorageMappingDto> mappings = storageMappingService.getStorageMappings(tenantId, artifactId);
        return ResponseEntity.ok(mappings);
    }

    @PutMapping("/storage-mappings")
    public ResponseEntity<StorageMappingDto> saveStorageMapping(@RequestBody StorageMappingDto dto) {
        StorageMappingDto updated = storageMappingService.saveOrUpdateManualMapping(
                dto.tenantId(),
                dto.artifactId(),
                dto.storageType(),
                dto.storageName(),
                dto.expireDays()
        );
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/storage-mappings")
    public ResponseEntity<Void> deleteStorageMapping(
            @RequestParam Long tenantId,
            @RequestParam Long artifactId) {
        storageMappingService.deleteMapping(tenantId, artifactId);
        return ResponseEntity.noContent().build();
    }
}
