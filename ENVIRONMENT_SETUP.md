# 환경별 실행 가이드

## 📋 환경 구성 개요

### 포트 통합
- **모든 환경**: 8081 포트 사용 (일관성 확보)

### 데이터베이스 구성
- **Local**: MySQL (로컬 개발용)
- **Dev/Prod**: PostgreSQL (Docker/K8s 환경용)

### 프로파일 구성
- **local**: 로컬 개발 (MySQL + 개발용 설정)
- **dev**: Docker 개발 환경 (PostgreSQL + 개발용 설정)
- **production**: 운영 환경 (PostgreSQL + 운영용 설정)

## 🚀 환경별 실행 방법

### 1. Local 환경 (MySQL)
```bash
# 환경변수 설정
cp .env.example .env
# .env 파일에서 실제 값으로 수정

# MySQL 실행 (별도)
# 애플리케이션 실행 (local 프로파일 - 기본값)
./gradlew bootRun

# 또는 프로파일 명시적 지정
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

**설정 파일**: `application-local.properties`
- MySQL 사용
- 포트: 8081
- 데이터베이스: localhost:3306

### 2. Dev 환경 (Docker + PostgreSQL)
```bash
# Docker Compose로 실행
docker-compose -f docker-compose.dev.yml up --build

# 또는 별도 실행
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun
```

**설정 파일**: `application-dev.properties`
- PostgreSQL 사용
- 포트: 8081
- 데이터베이스: postgres:5432 (Docker 내부)

### 3. Production 환경 (Kubernetes)
```bash
# Helm 차트로 배포
helm install payment-service ./helm/payment-service \
  -f environments/prod/values.yaml

# 또는 환경별 배포
helm install payment-service-dev ./helm/payment-service \
  -f environments/dev/values.yaml
```

**설정 파일**: `application-production.properties`
- PostgreSQL 사용
- 포트: 8081
- 데이터베이스: postgresql-service:5432 (K8s 서비스)

## 🔧 환경별 주요 차이점

| 구분 | Local | Dev | Production |
|------|-------|-----|------------|
| 데이터베이스 | MySQL | PostgreSQL | PostgreSQL |
| JPA DDL | update | update | validate |
| 로깅 레벨 | DEBUG | DEBUG | INFO |
| SQL 로깅 | true | true | false |
| Health 상세 | always | always | when-authorized |
| 복제본 수 | 1 | 1 | 3 |
| 자동 스케일링 | 비활성 | 비활성 | 활성 |

## 🔐 환경변수 관리

### Local 개발
`.env` 파일 사용:
```bash
TOSS_PAYMENTS_CLIENT_KEY=test_ck_yZqmkKeP8gJoxxW7gzz4rbQRxB9l
TOSS_PAYMENTS_SECRET_KEY=test_sk_24xLea5zVAkeX7pqXzgY8QAMYNwW
DB_URL=jdbc:mysql://localhost:3306/devops_platform_db...
```

### Docker 환경
`docker-compose.dev.yml`의 environment 섹션 사용

### Kubernetes 환경
Helm values 파일의 secrets 섹션 사용

## 🧪 테스트 방법

### Health Check
```bash
# 모든 환경 공통
curl http://localhost:8081/actuator/health
```

### 결제 테스트 페이지
```bash
# 토스페이먼츠 결제창 테스트
curl http://localhost:8081/payment/checkout
```

### API 테스트
```bash
# 게이트웨이 통한 테스트 (AuthenticationService 필요)
curl -H "Authorization: Bearer YOUR_JWT_TOKEN" \
     http://localhost:8080/gateway/payment/api/v1/test/health
```

## 🔍 문제 해결

### 포트 충돌
- 모든 환경에서 8081 포트 사용으로 통일
- Gateway는 8080 포트에서 8081로 프록시

### 데이터베이스 연결 실패
- Local: MySQL 서버 실행 확인
- Dev: `docker-compose up postgres` 실행
- Prod: PostgreSQL 서비스 상태 확인

### 환경변수 누락
- `.env` 파일 존재 및 내용 확인
- Kubernetes Secret 설정 확인
- 프로파일 설정 확인 (`SPRING_PROFILES_ACTIVE`)

## 📝 추가 참고사항

1. **데이터베이스 마이그레이션**: Local(MySQL) ↔ Dev/Prod(PostgreSQL) 간 스키마 차이 주의
2. **로그 레벨**: 환경별로 적절한 로그 레벨 설정
3. **보안**: Production 환경에서는 실제 TossPayments 키 사용 필요
4. **모니터링**: Prometheus 메트릭은 모든 환경에서 활성화