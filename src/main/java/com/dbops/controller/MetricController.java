package com.dbops.controller;

import com.dbops.entity.DbMetric;
import com.dbops.repository.DbInstanceRepository;
import com.dbops.repository.DbMetricRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/instances/{instanceId}/metrics")
public class MetricController {

    private final DbMetricRepository metricRepository;
    private final DbInstanceRepository instanceRepository;

    public MetricController(DbMetricRepository metricRepository,
                            DbInstanceRepository instanceRepository) {
        this.metricRepository = metricRepository;
        this.instanceRepository = instanceRepository;
    }

    public record MetricRequest(
            @NotNull Integer threadsConnected,
            @NotNull Long slowQueries,
            @NotNull Long uptimeSec,
            @NotNull Long questions
    ) {}

    /** 에이전트 push 수신 */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DbMetric receive(@PathVariable Long instanceId,
                            @Valid @RequestBody MetricRequest req) {
        if (!instanceRepository.existsById(instanceId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "존재하지 않는 인스턴스입니다: " + instanceId);
        }
        return metricRepository.save(new DbMetric(instanceId,
                req.threadsConnected(), req.slowQueries(), req.uptimeSec(), req.questions()));
    }

    /** 최근 지표 20건 */
    @GetMapping
    public List<DbMetric> recent(@PathVariable Long instanceId) {
        return metricRepository.findTop20ByInstanceIdOrderByCollectedAtDesc(instanceId);
    }
}