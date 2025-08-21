# DevOps 교육 플랫폼 - 결제 서비스 (Payment Service)

이 서비스는 구독 기반 결제 시스템과 티켓 관리 시스템을 제공하는 마이크로서비스입니다.

## 주요 기능

- **구독 플랜 관리**: Economy, Business, First Class 세 가지 플랜
- **Stripe 결제 연동**: 월간/연간 구독 결제 처리
- **티켓 시스템**: 미션 수행을 위한 티켓 관리 및 자동 충전
- **웹훅 처리**: Stripe 이벤트 실시간 처리
- **Kafka 이벤트 발행**: 결제 및 구독 관련 이벤트 발행

## 기술 스택

- **Framework**: Spring Boot 3.5.4
- **Database**: MySQL 8.0
- **Payment**: Stripe
- **Messaging**: Apache Kafka
- **ORM**: JPA/Hibernate
- **Language**: Java 17

## 설정 요구사항

### 환경 변수
```bash
# Stripe 설정
STRIPE_SECRET_KEY=sk_test_your_secret_key
STRIPE_PUBLISHABLE_KEY=pk_test_your_publishable_key
STRIPE_WEBHOOK_SECRET=whsec_your_webhook_secret

# 데이터베이스 설정
DB_HOST=localhost
DB_PORT=3306
DB_NAME=devops_platform_payment
DB_USERNAME=root
DB_PASSWORD=password

# Kafka 설정
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### 데이터베이스 설정
MySQL 8.0+가 필요하며, 애플리케이션 시작 시 자동으로 테이블이 생성됩니다.

```sql
CREATE DATABASE devops_platform_payment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

## 구독 플랜

### Economy Class (이코노미 클래스)
- **가격**: ₩29,000/월, ₩290,000/년
- **팀 멤버**: 최대 2명
- **월간 시도**: 10회
- **티켓 제한**: 3개
- **티켓 충전**: 24시간마다 3개

### Business Class (비즈니스 클래스)
- **가격**: ₩79,000/월, ₩790,000/년
- **팀 멤버**: 최대 6명
- **월간 시도**: 50회
- **티켓 제한**: 8개
- **티켓 충전**: 12시간마다 5개

### First Class (퍼스트 클래스)
- **가격**: ₩199,000/월, ₩1,990,000/년
- **팀 멤버**: 최대 20명
- **월간 시도**: 200회
- **티켓 제한**: 15개
- **티켓 충전**: 8시간마다 10개

## API 엔드포인트

### 구독 관리
```
GET    /api/v1/subscription-plans           # 활성 플랜 목록 조회
GET    /api/v1/subscription-plans/{id}      # 특정 플랜 조회
POST   /api/v1/subscriptions                # 구독 생성
POST   /api/v1/subscriptions/checkout       # Stripe 결제 페이지 생성
GET    /api/v1/subscriptions/users/{userId} # 사용자 구독 내역
GET    /api/v1/subscriptions/users/{userId}/active # 활성 구독 조회
POST   /api/v1/subscriptions/{id}/cancel    # 구독 취소
```

### 티켓 관리
```
GET    /api/v1/tickets/users/{userId}       # 사용자 티켓 조회
POST   /api/v1/tickets/users/{userId}/use   # 티켓 사용
POST   /api/v1/tickets/users/{userId}/refund # 티켓 환불
POST   /api/v1/tickets/users/{userId}/adjust # 티켓 조정 (관리자)
POST   /api/v1/tickets/refill               # 티켓 자동 충전 (스케줄러)
```

### 웹훅
```
POST   /api/v1/webhooks/stripe              # Stripe 웹훅 처리
```

## 사용 예시

### 1. 구독 생성 요청
```json
POST /api/v1/subscriptions
{
  "userId": 1,
  "planId": 1,
  "billingCycle": "MONTHLY"
}
```

### 2. Stripe 결제 페이지 생성
```json
POST /api/v1/subscriptions/checkout
{
  "userId": 1,
  "planId": 1,
  "billingCycle": "MONTHLY",
  "successUrl": "https://yourapp.com/success",
  "cancelUrl": "https://yourapp.com/cancel"
}
```

### 3. 티켓 사용
```json
POST /api/v1/tickets/users/1/use?amount=1&attemptId=123&reason=mission_start
```

## Kafka 이벤트

다음 이벤트들이 Kafka로 발행됩니다:

- `SUBSCRIPTION_CREATED`: 구독 생성됨
- `SUBSCRIPTION_CANCELLED`: 구독 취소됨
- `SUBSCRIPTION_EXPIRED`: 구독 만료됨
- `PAYMENT_SUCCEEDED`: 결제 성공
- `PAYMENT_FAILED`: 결제 실패
- `TICKETS_USED`: 티켓 사용됨
- `TICKETS_REFILLED`: 티켓 충전됨

## 실행 방법

### 1. 로컬 개발 환경
```bash
# 의존성 설치
./gradlew build

# 애플리케이션 실행
./gradlew bootRun
```

### 2. Docker 실행
```bash
# Docker 이미지 빌드
docker build -t payment-service .

# 컨테이너 실행
docker run -p 8083:8083 \
  -e STRIPE_SECRET_KEY=your_key \
  -e DB_HOST=your_db_host \
  payment-service
```

## 모니터링

애플리케이션은 다음 헬스체크 엔드포인트를 제공합니다:
- `/actuator/health`: 애플리케이션 상태
- `/actuator/metrics`: 메트릭 정보
- `/actuator/info`: 애플리케이션 정보

## 테스트

### 테스트 실행
```bash
# 전체 테스트 실행
./gradlew test

# 특정 테스트 클래스 실행
./gradlew test --tests "SubscriptionServiceTest"

# 통합 테스트만 실행
./gradlew test --tests "*IntegrationTest"

# 테스트 커버리지 리포트 생성
./gradlew test jacocoTestReport
```

### 테스트 구조
프로젝트는 TDD 방식으로 개발되었으며 다음과 같은 테스트들을 포함합니다:

#### 단위 테스트 (Unit Tests)
- **SubscriptionServiceTest**: 구독 서비스 로직 테스트
- **TicketServiceTest**: 티켓 관리 서비스 로직 테스트
- **StripeServiceTest**: Stripe API 연동 테스트
- **PaymentEventServiceTest**: Kafka 이벤트 발행 테스트

#### 통합 테스트 (Integration Tests)
- **SubscriptionControllerTest**: 구독 관련 REST API 테스트
- **TicketControllerTest**: 티켓 관련 REST API 테스트
- **StripeWebhookControllerTest**: Stripe 웹훅 API 테스트
- **PaymentServiceIntegrationTest**: 전체 비즈니스 플로우 테스트

#### 데이터 접근 테스트 (Data Access Tests)
- **SubscriptionRepositoryTest**: 구독 데이터 접근 테스트
- **UserTicketRepositoryTest**: 티켓 데이터 접근 테스트

#### 메시징 테스트 (Messaging Tests)
- **PaymentEventServiceTest**: Kafka 이벤트 발행/수신 테스트 (EmbeddedKafka 사용)

### 테스트 설정
- **H2 인메모리 데이터베이스**: 빠른 테스트 실행
- **EmbeddedKafka**: Kafka 통합 테스트
- **MockMvc**: REST API 테스트
- **Testcontainers**: 격리된 테스트 환경 (선택적)

### 테스트 커버리지
주요 비즈니스 로직과 API 엔드포인트에 대해 90% 이상의 테스트 커버리지를 목표로 합니다.

## 개발 참고사항

### Stripe 설정
1. Stripe 계정에서 API 키 발급
2. 웹훅 엔드포인트 설정: `https://yourapp.com/api/v1/webhooks/stripe`
3. 다음 이벤트 구독:
   - `customer.subscription.created`
   - `customer.subscription.updated`
   - `customer.subscription.deleted`
   - `invoice.payment_succeeded`
   - `invoice.payment_failed`
   - `checkout.session.completed`

### 데이터베이스 마이그레이션
JPA Auto DDL이 활성화되어 있어 스키마가 자동 생성됩니다. 
프로덕션 환경에서는 `spring.jpa.hibernate.ddl-auto=validate`로 설정하세요.

### 로깅
구조화된 로깅을 위해 JSON 포맷 사용을 권장합니다.
주요 이벤트(결제, 구독 변경)는 모두 로그로 기록됩니다.