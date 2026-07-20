package com.onetuks.iflow_sentinel.rulemgmt;

import com.onetuks.iflow_sentinel.rulemgmt.dto.BindingOverrideRequest;
import com.onetuks.iflow_sentinel.rulemgmt.dto.BindingOverrideResponse;
import com.onetuks.iflow_sentinel.rulemgmt.dto.BindingRequest;
import com.onetuks.iflow_sentinel.rulemgmt.dto.BindingResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BindingController {

    private final BindingService bindingService;

    public BindingController(BindingService bindingService) {
        this.bindingService = bindingService;
    }

    @PostMapping("/api/projects/{projectId}/bindings")
    public BindingResponse create(@PathVariable Long projectId, @RequestBody BindingRequest request) {
        return bindingService.create(projectId, request);
    }

    @GetMapping("/api/projects/{projectId}/bindings")
    public List<BindingResponse> list(@PathVariable Long projectId) {
        return bindingService.list(projectId);
    }

    @PostMapping("/api/bindings/{id}/overrides")
    public BindingOverrideResponse addOverride(@PathVariable Long id, @RequestBody BindingOverrideRequest request) {
        return bindingService.addOverride(id, request);
    }
}
