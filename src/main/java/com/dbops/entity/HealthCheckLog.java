package com.dbops.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 헬스체크 이력.
 * 인덱스 (instanceId, checkedAt): 주 조회 패턴이
 * "특정 인스턴스의 최근 이력"이므로 복합 인덱스로 설계.
 */
@Entity
@Table(name = "health_check_log",
        indexes = @Index(name = "idx_instance_checked", columnList = "instanceId, checkedAt"))
public class HealthCheckLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long instanceId;

    @Column(nullable = false)
    private Boolean reachable;

    private Integer connectionCnt;

    private Integer responseMs;

    @Column(nullable = false)
    private LocalDateTime checkedAt;

    protected HealthCheckLog() {}

    public HealthCheckLog(Long instanceId, Boolean reachable, Integer connectionCnt, Integer responseMs) {
        this.instanceId = instanceId;
        this.reachable = reachable;
        this.connectionCnt = connectionCnt;
        this.responseMs = responseMs;
        this.checkedAt = LocalDateTime.now();
    }

    public Long getId()             { return id; }
    public Long getInstanceId()     { return instanceId; }
    public Boolean getReachable()   { return reachable; }
    public Integer getConnectionCnt() { return connectionCnt; }
    public Integer getResponseMs()  { return responseMs; }
    public LocalDateTime getCheckedAt() { return checkedAt; }
}