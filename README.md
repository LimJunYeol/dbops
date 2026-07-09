# DBOps — Mini DBMS 운영 관리 플랫폼

반복되는 DBMS 운영 업무를 API 기반으로 표준화하고, 작업 상태와 실패 이력을
추적 가능한 구조로 설계한 미니 운영 플랫폼입니다.
여러 MySQL 인스턴스의 등록·상태 점검·계정/권한 관리·설정 배포를
하나의 플랫폼에서 처리하는 것을 목표로 구현했습니다.

📘 **설계 판단 13가지와 트러블슈팅 기록**: [Notion 기술 문서](https://app.notion.com/p/DBOps-DB-3923cf5231a980c1aa10c5c5b2db03ec)

## 아키텍처
<img width="1480" height="1000" alt="68de8264-e26e-438b-b3de-544d0c1b4a00" src="https://github.com/user-attachments/assets/274c85e3-9d5f-4eed-8436-607c51b0ff4e" />

| 기술 | 프로젝트 내 역할 | 깊이 |
|---|---|---|
| Java/Spring Boot | 메인 API 서버 — 작업 요청·상태·이력 관리 | 주력 |
| MySQL | 메타데이터 저장소 + 관리 대상 DBMS | 주력 |
| Vue 3 | 관리 콘솔 — 상태·이력 조회, 계정/권한 작업 | 활용 |
| Python | DB 상태 지표 수집 에이전트 | 활용 |
| Go | 병렬 헬스체크 CLI | 경험 |
| SaltStack | my.cnf 설정 배포 | 경험 |
| Kubernetes | 플랫폼 컨테이너 배포 (minikube) | 경험 |

## 핵심 설계

- **동적 DB 접속** — 런타임에 등록되는 임의의 DB에 접속해야 하므로,
  설정 파일 기반 단일 DataSource 대신 동적 접속 구조를 사용
- **작업 상태 머신** — Meta DB와 Target DB는 하나의 트랜잭션으로 묶을 수 없어
  작업을 `PENDING → SUCCESS / FAILED`로 관리하고, 이력 저장은
  독립 트랜잭션(REQUIRES_NEW)으로 분리해 실패 상황에서도 기록을 보장
- **인젝션 방어** — CREATE USER / GRANT는 파라미터 바인딩이 불가하므로
  정규식 검증 + enum 화이트리스트로 허용된 권한 유형과 스키마만 실행

## 실행 방법

### 사전 준비
- JDK 17, Docker Desktop, Node.js (콘솔 실행 시)

### 1. MySQL 4대 기동 (Meta 1 + Target 3)
```bash
docker compose up -d
```

### 2. API 서버 실행
```bash
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

### 3. 인스턴스 등록 및 확인
```bash
# 관리 대상 DB 등록 (등록 시 저장 전 접속 검증 수행)
curl -X POST http://localhost:8080/api/instances \
  -H "Content-Type: application/json" \
  -d '{"name":"order-db","host":"localhost","port":3307,"adminUser":"root","adminPassword":"target1pass"}'

# 목록 조회 (30초 주기 헬스체크로 상태 자동 갱신)
curl http://localhost:8080/api/instances

# 계정 생성 + 권한 부여 (작업 이력 자동 기록)
curl -X POST http://localhost:8080/api/instances/1/accounts \
  -H "Content-Type: application/json" \
  -d '{"username":"app_reader","password":"Reader1234!"}'

curl -X POST http://localhost:8080/api/instances/1/grants \
  -H "Content-Type: application/json" \
  -d '{"username":"app_reader","database":"sample_order","privilege":"READ_ONLY"}'

# 작업 이력 조회
curl http://localhost:8080/api/instances/1/tasks
```

### 4. 관리 콘솔 (선택)
```bash
cd console && npm install && npm run dev   # http://localhost:5173
```

### 5. 자동화 계층 (선택)
```bash
# Python 지표 수집 에이전트
cd agent && pip install -r requirements.txt && python collector.py

# Go 병렬 헬스체크 CLI
cd healthcheck-cli && go run main.go

# SaltStack 설정 배포
docker exec dbops-salt-master salt 'target-db-1-host' state.apply mysql-config
```

## 프로젝트 구조

```
dbops/
├── src/main/java/com/dbops/   # Spring Boot API 서버
├── console/                   # Vue 3 관리 콘솔
├── agent/                     # Python 지표 수집 에이전트
├── healthcheck-cli/           # Go 병렬 헬스체크 CLI
├── salt/                      # SaltStack 이미지·state
├── k8s/                       # Kubernetes 매니페스트
└── docker-compose.yml         # 로컬 환경 (MySQL 4대 + Salt)
```

## 참고

- 학습용 로컬 환경을 전제로 DB 접속 정보가 설정 파일에 포함되어 있습니다.
  실무 환경에서는 환경변수 또는 Vault/KMS 등 시크릿 관리 체계로 분리해야 합니다.
