package com.dbops.scheduler;

import com.dbops.entity.DbInstance;
import com.dbops.repository.DbInstanceRepository;
import com.dbops.service.InstanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 전체 인스턴스 주기 점검.
 * [한계] 순차 점검이라 죽은 DB가 많으면 (타임아웃 × N)만큼 주기가 늘어진다.
 * -> 3주차 Go 병렬 헬스체크의 동기.
 */
@Component
public class HealthCheckScheduler {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckScheduler.class);

    private final DbInstanceRepository instanceRepository;
    private final InstanceService instanceService;

    public HealthCheckScheduler(DbInstanceRepository instanceRepository,
                                InstanceService instanceService) {
        this.instanceRepository = instanceRepository;
        this.instanceService = instanceService;
    }

    @Scheduled(fixedDelayString = "${dbops.health-check.interval-ms}")
    public void checkAllInstances() {
        List<DbInstance> instances = instanceRepository.findAll();
        if (instances.isEmpty()) return;

        log.info("헬스체크 시작 - 대상 {}대", instances.size());
        for (DbInstance instance : instances) {
            var result = instanceService.performCheck(instance);
            log.info("  [{}] {}:{} -> {} ({}ms, 커넥션 {})",
                    instance.getName(), instance.getHost(), instance.getPort(),
                    result.getReachable() ? "OK" : "UNREACHABLE",
                    result.getResponseMs(), result.getConnectionCnt());
        }
    }
}