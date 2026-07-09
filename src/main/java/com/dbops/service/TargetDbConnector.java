package com.dbops.service;

import com.dbops.entity.DbInstance;
import com.dbops.util.AesEncryptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Properties;

/**
 * 관리 "대상" DB에 동적으로 접속하는 커넥터.
 *
 * [설계 판단]
 * 일반 애플리케이션은 자기 DB 하나에만 붙으므로 설정 파일 기반
 * 단일 DataSource로 충분하다. 운영 플랫폼은 "런타임에 등록되는
 * 임의의 DB"에 접속해야 하므로 DriverManager로 직접 접속한다.
 * 헬스체크는 커넥션 풀을 거치지 않아야 실제 접속 가능 여부를
 * 정확히 판별할 수 있다. (풀의 유휴 커넥션은 살아있다고 착각하게 만듦)
 */
@Component
public class TargetDbConnector {

    private final AesEncryptor encryptor;
    private final int timeoutMs;

    public TargetDbConnector(AesEncryptor encryptor,
                             @Value("${dbops.health-check.timeout-ms}") int timeoutMs) {
        this.encryptor = encryptor;
        this.timeoutMs = timeoutMs;
    }

    /** 접속 테스트 + 현재 커넥션 수 조회 */
    public ProbeResult probe(DbInstance instance) {
        String url = "jdbc:mysql://%s:%d/?connectTimeout=%d&socketTimeout=%d"
                .formatted(instance.getHost(), instance.getPort(), timeoutMs, timeoutMs);

        Properties props = new Properties();
        props.setProperty("user", instance.getAdminUser());
        props.setProperty("password", encryptor.decrypt(instance.getAdminPwEnc()));

        long start = System.currentTimeMillis();
        try (Connection conn = DriverManager.getConnection(url, props);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SHOW STATUS LIKE 'Threads_connected'")) {

            Integer connectionCnt = rs.next() ? rs.getInt(2) : null;
            int elapsed = (int) (System.currentTimeMillis() - start);
            return new ProbeResult(true, connectionCnt, elapsed);

        } catch (Exception e) {
            return new ProbeResult(false, null, null);
        }
    }

    public void execute(DbInstance instance, String sql) throws Exception {
        String url = "jdbc:mysql://%s:%d/?connectTimeout=%d&socketTimeout=%d"
                .formatted(instance.getHost(), instance.getPort(), timeoutMs, timeoutMs);

        Properties props = new Properties();
        props.setProperty("user", instance.getAdminUser());
        props.setProperty("password", encryptor.decrypt(instance.getAdminPwEnc()));

        try (Connection conn = DriverManager.getConnection(url, props);
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    public record ProbeResult(
            boolean reachable,
            Integer connectionCnt,
            Integer responseMs
    ) {}
}