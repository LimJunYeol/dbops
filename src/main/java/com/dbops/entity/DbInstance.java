package com.dbops.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 관리 대상 DB 인스턴스.
 * - (host, port) 유니크 제약: 중복 등록을 DB 레벨에서 차단
 * - 비밀번호는 AES 암호화 값만 저장 (평문 금지)
 */
@Entity
@Table(name = "db_instance",
        uniqueConstraints = @UniqueConstraint(name = "uk_host_port", columnNames = {"host", "port"}))
public class DbInstance {

    public enum Status { ACTIVE, UNREACHABLE }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(nullable = false, length = 100)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false, length = 50)
    private String adminUser;

    @Column(nullable = false, length = 255)
    private String adminPwEnc;        // AES 암호화된 비밀번호

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected DbInstance() {}         // JPA 기본 생성자

    public DbInstance(String name, String host, Integer port, String adminUser, String adminPwEnc) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.adminUser = adminUser;
        this.adminPwEnc = adminPwEnc;
        this.status = Status.ACTIVE;
        this.createdAt = LocalDateTime.now();
    }

    public void markUnreachable() { this.status = Status.UNREACHABLE; }
    public void markActive()      { this.status = Status.ACTIVE; }

    public Long getId()            { return id; }
    public String getName()        { return name; }
    public String getHost()        { return host; }
    public Integer getPort()       { return port; }
    public String getAdminUser()   { return adminUser; }
    public String getAdminPwEnc()  { return adminPwEnc; }
    public Status getStatus()      { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}