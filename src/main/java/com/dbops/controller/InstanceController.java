package com.dbops.controller;

import com.dbops.dto.InstanceDtos.*;
import com.dbops.service.InstanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/instances")
public class InstanceController {

    private final InstanceService instanceService;

    public InstanceController(InstanceService instanceService) {
        this.instanceService = instanceService;
    }

    /** 등록 (저장 전 접속 검증 포함) */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InstanceResponse register(@Valid @RequestBody RegisterRequest request) {
        return instanceService.register(request);
    }

    /** 전체 목록 + 현재 상태 */
    @GetMapping
    public List<InstanceResponse> list() {
        return instanceService.findAll();
    }

    /** 상세 (최근 헬스체크 10건 포함) */
    @GetMapping("/{id}")
    public InstanceDetailResponse detail(@PathVariable Long id) {
        return instanceService.findDetail(id);
    }

    /** 등록 해제 (이력은 보존) */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        instanceService.delete(id);
    }

    /** 즉시 헬스체크 */
    @PostMapping("/{id}/health")
    public HealthLogResponse checkNow(@PathVariable Long id) {
        return instanceService.checkNow(id);
    }
}