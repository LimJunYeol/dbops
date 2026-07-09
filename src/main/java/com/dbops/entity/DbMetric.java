package com.dbops.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Python 에이전트가 push하는 DB 상태 지표 */
@Entity
@Table(name = "db_metric",
        indexes = @Index(name = "idx_metric_instance", columnList = "instanceId, collectedAt"))
public class DbMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long instanceId;

    @Column(nullable = false)
    private Integer threadsConnected;   // 현재 커넥션 수

    @Column(nullable = false)
    private Long slowQueries;           // 누적 슬로우 쿼리 수

    @Column(nullable = false)
    private Long uptimeSec;             // 기동 후 경과 초

    @Column(nullable = false)
    private Long questions;             // 누적 쿼리 수

    @Column(nullable = false)
    private LocalDateTime collectedAt;

    protected DbMetric() {}

    public DbMetric(Long instanceId, Integer threadsConnected,
                    Long slowQueries, Long uptimeSec, Long questions) {
        this.instanceId = instanceId;
        this.threadsConnected = threadsConnected;
        this.slowQueries = slowQueries;
        this.uptimeSec = uptimeSec;
        this.questions = questions;
        this.collectedAt = LocalDateTime.now();
    }

    public Long getId()                { return id; }
    public Long getInstanceId()        { return instanceId; }
    public Integer getThreadsConnected() { return threadsConnected; }
    public Long getSlowQueries()       { return slowQueries; }
    public Long getUptimeSec()         { return uptimeSec; }
    public Long getQuestions()         { return questions; }
    public LocalDateTime getCollectedAt() { return collectedAt; }
}