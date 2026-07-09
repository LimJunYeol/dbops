# DBOps — 미니 매니지드 DB 플랫폼

여러 MySQL 인스턴스의 등록·상태 점검·계정/권한 관리·작업 이력을 하나의 REST API로 처리하는
운영 플랫폼입니다. "사람이 없어도 틀리지 않는 시스템"을 목표로,
DBMS 운영 업무를 표준화된 API와 자동화 체계로 옮기는 구조를 직접 구현했습니다.

📘 **설계 판단 13가지와 트러블슈팅 기록**: [Notion 기술 문서](노션링크)

## 아키텍처

![architecture](docs/01_architecture.png)

| 기술 | 역할 | 깊이 |
|---|---|---|
| Java/Spring Boot | 메인 API 서버 (상태·계정·이력 관리) | 주력 |
| MySQL | 메타데이터 저장소 + 관리 대상 DBMS | 주력 |
| Python | DB 상태 지표 수집 에이전트 | 활용 |
| Vue 3 | 관리 콘솔 | 활용 |
| Go | 병렬 헬스체크 CLI | 경험 |
| Saltstack | my.cnf 설정 일괄 배포 | 경험 |
| Kubernetes | 플랫폼 배포 (minikube) | 경험 |

## 핵심 설계

- **동적 DB 접속**: 런타임에 등록되는 임의 DB에 접속해야 하므로 단일 DataSource 대신 동적 접속 구조
- **작업 상태 머신**: 메타DB와 대상 DB는 한 트랜잭션으로 묶을 수 없어 PENDING→SUCCESS/FAILED로 관리, 이력 저장은 REQUIRES_NEW로 분리해 실패 기록 보장
- **인젝션 방어**: CREATE USER/GRANT는 파라미터 바인딩 불가 → 정규식 + enum 화이트리스트 검증

## 실행 방법

```bash
# 1. MySQL 4대 기동 (메타 1 + 관리 대상 3)
docker compose up -d

# 2. API 서버 실행
./gradlew bootRun

# 3. 인스턴스 등록
curl -X POST http://localhost:8080/api/instances \
  -H "Content-Type: application/json" \
  -d '{"name":"order-db","host":"localhost","port":3307,"adminUser":"root","adminPassword":"target1pass"}'

# 4. 관리 콘솔 (선택)
cd console && npm install && npm run dev   # http://localhost:5173
```

## 참고
- 학습용 로컬 환경 전제로 접속 정보가 설정 파일에 포함되어 있습니다.
  실무 환경에서는 환경변수 또는 Vault/KMS로 분리해야 합니다.