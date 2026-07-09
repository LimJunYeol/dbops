package com.dbops.repository;

import com.dbops.entity.DbMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DbMetricRepository extends JpaRepository<DbMetric, Long> {

    List<DbMetric> findTop20ByInstanceIdOrderByCollectedAtDesc(Long instanceId);
}