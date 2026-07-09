package com.dbops.dto;

import com.dbops.entity.DbInstance;
import com.dbops.entity.HealthCheckLog;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.List;

/** 인스턴스 요청/응답 DTO 모음 */
public class InstanceDtos {

    /** 등록 요청 */
    public record RegisterRequest(
            @NotBlank @Size(max = 50) String name,
            @NotBlank @Size(max = 100) String host,
            @NotNull @Min(1) @Max(65535) Integer port,
            @NotBlank String adminUser,
            @NotBlank String adminPassword
    ) {}

    /** 목록/상세 공통 응답 — 비밀번호는 절대 응답에 포함하지 않는다 */
    public record InstanceResponse(
            Long id, String name, String host, Integer port,
            String adminUser, String status, LocalDateTime createdAt
    ) {
        public static InstanceResponse from(DbInstance i) {
            return new InstanceResponse(i.getId(), i.getName(), i.getHost(), i.getPort(),
                    i.getAdminUser(), i.getStatus().name(), i.getCreatedAt());
        }
    }

    /** 헬스체크 1건 응답 */
    public record HealthLogResponse(
            Boolean reachable, Integer connectionCnt, Integer responseMs, LocalDateTime checkedAt
    ) {
        public static HealthLogResponse from(HealthCheckLog log) {
            return new HealthLogResponse(log.getReachable(), log.getConnectionCnt(),
                    log.getResponseMs(), log.getCheckedAt());
        }
    }

    /** 상세 응답 = 인스턴스 정보 + 최근 헬스체크 이력 */
    public record InstanceDetailResponse(
            InstanceResponse instance, List<HealthLogResponse> recentChecks
    ) {}
}