package com.dbops.repository;

import com.dbops.entity.DbInstance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DbInstanceRepository extends JpaRepository<DbInstance, Long> {

    boolean existsByHostAndPort(String host, Integer port);

    boolean existsByName(String name);
}