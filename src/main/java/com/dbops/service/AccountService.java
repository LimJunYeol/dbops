package com.dbops.service;

import com.dbops.dto.AccountDtos.*;
import com.dbops.entity.DbInstance;
import com.dbops.entity.DbTask;
import com.dbops.repository.DbInstanceRepository;
import com.dbops.repository.DbTaskRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * 계정/권한 작업 서비스.
 *
 * [핵심 설계]
 * 1) 이력 기록은 REQUIRES_NEW 트랜잭션으로 분리
 *    -> 대상 DB 실행이 실패해도 FAILED 이력은 반드시 남는다.
 * 2) 비밀번호는 detail에 절대 기록하지 않는다.
 * 3) 식별자는 DTO 정규식 + enum 화이트리스트로 이중 방어.
 */
@Service
public class AccountService {

    private final DbInstanceRepository instanceRepository;
    private final DbTaskRepository taskRepository;
    private final TargetDbConnector connector;

    public AccountService(DbInstanceRepository instanceRepository,
                          DbTaskRepository taskRepository,
                          TargetDbConnector connector) {
        this.instanceRepository = instanceRepository;
        this.taskRepository = taskRepository;
        this.connector = connector;
    }

    public TaskResponse createAccount(Long instanceId, CreateAccountRequest req) {
        DbInstance instance = getInstance(instanceId);

        // 접속 출처는 학습 편의상 '%' 고정. 실무에서는 대역 제한이 기본.
        String sql = "CREATE USER '%s'@'%%' IDENTIFIED BY '%s'"
                .formatted(req.username(), req.password().replace("'", "''"));
        String detail = "CREATE USER '%s'@'%%'".formatted(req.username()); // 비밀번호 제외

        return executeWithTask(instance, DbTask.Type.CREATE_ACCOUNT, detail, sql);
    }

    public TaskResponse grant(Long instanceId, GrantRequest req) {
        DbInstance instance = getInstance(instanceId);

        String sql = "GRANT %s ON %s.* TO '%s'@'%%'"
                .formatted(req.privilege().toSql(), req.database(), req.username());

        return executeWithTask(instance, DbTask.Type.GRANT_PRIVILEGE, sql, sql);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> findTasks(Long instanceId) {
        getInstance(instanceId);
        return taskRepository.findByInstanceIdOrderByCreatedAtDesc(instanceId).stream()
                .map(TaskResponse::from)
                .toList();
    }

    /** 공통 흐름: PENDING 기록 -> 대상 DB 실행 -> 상태 갱신 */
    private TaskResponse executeWithTask(DbInstance instance, DbTask.Type type,
                                         String detail, String sql) {
        DbTask task = saveTask(new DbTask(instance.getId(), type, detail));

        try {
            connector.execute(instance, sql);
            task.succeed();
        } catch (Exception e) {
            task.fail(summarize(e));
        }
        return TaskResponse.from(saveTask(task));
    }

    /** 이력 저장을 독립 트랜잭션으로 -> 실행 실패와 무관하게 기록 보장 */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected DbTask saveTask(DbTask task) {
        return taskRepository.save(task);
    }

    private String summarize(Exception e) {
        String msg = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return msg.length() > 1000 ? msg.substring(0, 1000) : msg;
    }

    private DbInstance getInstance(Long id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 인스턴스입니다: " + id));
    }
}