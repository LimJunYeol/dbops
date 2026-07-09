package com.dbops.dto;

import com.dbops.entity.DbTask;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public class AccountDtos {

    /**
     * 계정 생성 요청.
     * username 패턴 검증이 1차 방어선 (DDL은 파라미터 바인딩 불가).
     */
    public record CreateAccountRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,31}$",
                    message = "계정명은 영문으로 시작, 영문/숫자/_ 조합 3~32자")
            String username,
            @NotBlank @Size(min = 8, max = 64) String password
    ) {}

    /** 권한 부여 요청. privilege는 화이트리스트 enum으로만 받는다. */
    public record GrantRequest(
            @NotBlank @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{2,31}$") String username,
            @NotBlank @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]{0,63}$",
                    message = "데이터베이스명 형식이 올바르지 않습니다") String database,
            @NotNull Privilege privilege
    ) {}

    /** 권한을 자유 문자열이 아닌 enum으로 제한 -> GRANT 문 인젝션 원천 차단 */
    public enum Privilege {
        READ_ONLY("SELECT"),
        READ_WRITE("SELECT, INSERT, UPDATE, DELETE");

        private final String sql;
        Privilege(String sql) { this.sql = sql; }
        public String toSql() { return sql; }
    }

    public record TaskResponse(
            Long id, String type, String status, String detail,
            String errorMessage, LocalDateTime createdAt, LocalDateTime finishedAt
    ) {
        public static TaskResponse from(DbTask t) {
            return new TaskResponse(t.getId(), t.getType().name(), t.getStatus().name(),
                    t.getDetail(), t.getErrorMessage(), t.getCreatedAt(), t.getFinishedAt());
        }
    }
}