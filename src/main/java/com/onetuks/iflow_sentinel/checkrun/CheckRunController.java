package com.onetuks.iflow_sentinel.checkrun;

import com.onetuks.iflow_sentinel.checkrun.dto.CheckRunRequest;
import com.onetuks.iflow_sentinel.checkrun.dto.CheckRunResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/checkruns")
public class CheckRunController {

    private final CheckRunService checkRunService;

    public CheckRunController(CheckRunService checkRunService) {
        this.checkRunService = checkRunService;
    }

    @PostMapping
    public CheckRunResponse run(@RequestBody CheckRunRequest request) {
        return checkRunService.run(request.bindingId(), request.artifactId());
    }

    @GetMapping("/{id}")
    public CheckRunResponse get(@PathVariable Long id) {
        return checkRunService.get(id);
    }

    @GetMapping
    public List<CheckRunResponse> list(@RequestParam Long projectId) {
        return checkRunService.list(projectId);
    }
}
