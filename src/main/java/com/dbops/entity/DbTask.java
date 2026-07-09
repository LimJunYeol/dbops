package com.dbops.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 대상 DB에 실행하는 작업의 이력.
 * 메타DB 트랜잭션과 대상 DB 실행은 원자적으로 묶을 수 없으므로
 * PENDING -> SUCCESS/FAILED 상태 머신으로 관리한다.
 */
@Entity
@Table(name = "db_task",
        indexes = @Index(name = "idx_task_instance", columnList = "instanceId, createdAt"))
public class DbTask {

    public enum Type { CREATE_ACCOUNT, GRANT_PRIVILEGE }
    public enum Status { PENDING, SUCCESS, FAILED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long instanceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, length = 500)
    private String detail;            // 실행 내용 요약 (비밀번호 제외)

    @Column(length = 1000)
    private String errorMessage;      // 실패 시 원인

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime finishedAt;

    protected DbTask() {}

    public DbTask(Long instanceId, Type type, String detail) {
        this.instanceId = instanceId;
        this.type = type;
        this.status = Status.PENDING;
        this.detail = detail;
        this.createdAt = LocalDateTime.now();
    }

    public void succeed() {
        this.status = Status.SUCCESS;
        this.finishedAt = LocalDateTime.now();
    }

    public void fail(String errorMessage) {
        this.status = Status.FAILED;
        this.errorMessage = errorMessage;
        this.finishedAt = LocalDateTime.now();
    }

    public Long getId()              { return id; }
    public Long getInstanceId()      { return instanceId; }
    public Type getType()            { return type; }
    public Status getStatus()        { return status; }
    public String getDetail()        { return detail; }
    public String getErrorMessage()  { return errorMessage; }
    public LocalDateTime getCreatedAt()  { return createdAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}