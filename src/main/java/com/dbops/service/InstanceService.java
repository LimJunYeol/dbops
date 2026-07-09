package com.dbops.service;

import com.dbops.dto.InstanceDtos.*;
import com.dbops.entity.DbInstance;
import com.dbops.entity.HealthCheckLog;
import com.dbops.repository.DbInstanceRepository;
import com.dbops.repository.HealthCheckLogRepository;
import com.dbops.util.AesEncryptor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class InstanceService {

    private final DbInstanceRepository instanceRepository;
    private final HealthCheckLogRepository logRepository;
    private final TargetDbConnector connector;
    private final AesEncryptor encryptor;

    public InstanceService(DbInstanceRepository instanceRepository,
                           HealthCheckLogRepository logRepository,
                           TargetDbConnector connector,
                           AesEncryptor encryptor) {
        this.instanceRepository = instanceRepository;
        this.logRepository = logRepository;
        this.connector = connector;
        this.encryptor = encryptor;
    }

    /**
     * 등록. 순서가 중요: 저장 "전에" 실제 접속을 검증한다.
     * 접속 불가 정보를 저장하면 이후 헬스체크가 무의미한 실패를 반복한다.
     */
    @Transactional
    public InstanceResponse register(RegisterRequest req) {
        if (instanceRepository.existsByName(req.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 사용 중인 이름입니다: " + req.name());
        }
        if (instanceRepository.existsByHostAndPort(req.host(), req.port())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "이미 등록된 DB입니다: %s:%d".formatted(req.host(), req.port()));
        }

        DbInstance candidate = new DbInstance(
                req.name(), req.host(), req.port(),
                req.adminUser(), encryptor.encrypt(req.adminPassword()));

        TargetDbConnector.ProbeResult probe = connector.probe(candidate);
        if (!probe.reachable()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "DB 접속에 실패했습니다. host/port/계정 정보를 확인하세요.");
        }

        DbInstance saved = instanceRepository.save(candidate);
        logRepository.save(new HealthCheckLog(saved.getId(), true,
                probe.connectionCnt(), probe.responseMs()));
        return InstanceResponse.from(saved);
    }

    public List<InstanceResponse> findAll() {
        return instanceRepository.findAll().stream()
                .map(InstanceResponse::from)
                .toList();
    }

    public InstanceDetailResponse findDetail(Long id) {
        DbInstance instance = getOrThrow(id);
        List<HealthLogResponse> recent = logRepository
                .findTop10ByInstanceIdOrderByCheckedAtDesc(id).stream()
                .map(HealthLogResponse::from)
                .toList();
        return new InstanceDetailResponse(InstanceResponse.from(instance), recent);
    }

    /** 삭제. 이력은 감사(audit) 목적상 보존한다. */
    @Transactional
    public void delete(Long id) {
        instanceRepository.delete(getOrThrow(id));
    }

    /** 즉시 헬스체크 */
    @Transactional
    public HealthLogResponse checkNow(Long id) {
        return HealthLogResponse.from(performCheck(getOrThrow(id)));
    }

    /** 헬스체크 수행 + 상태 갱신 + 이력 기록. 스케줄러에서도 호출. */
    @Transactional
    public HealthCheckLog performCheck(DbInstance instance) {
        TargetDbConnector.ProbeResult probe = connector.probe(instance);

        if (probe.reachable()) instance.markActive();
        else instance.markUnreachable();
        instanceRepository.save(instance);

        return logRepository.save(new HealthCheckLog(
                instance.getId(), probe.reachable(), probe.connectionCnt(), probe.responseMs()));
    }

    private DbInstance getOrThrow(Long id) {
        return instanceRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "존재하지 않는 인스턴스입니다: " + id));
    }
}