package com.dbops.repository;

import com.dbops.entity.HealthCheckLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HealthCheckLogRepository extends JpaRepository<HealthCheckLog, Long> {
    List<HealthCheckLog> findTop10ByInstanceIdOrderByCheckedAtDesc(Long instanceId);
}