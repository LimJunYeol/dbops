package com.dbops.repository;

import com.dbops.entity.DbTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DbTaskRepository extends JpaRepository<DbTask, Long> {
    List<DbTask> findByInstanceIdOrderByCreatedAtDesc(Long instanceId);
}