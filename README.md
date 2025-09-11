# Payment Service

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-6DB33F?style=flat-square&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=flat-square&logo=mysql&logoColor=white)
![Stripe](https://img.shields.io/badge/Stripe-API-008CDD?style=flat-square&logo=stripe&logoColor=white)
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.6-231F20?style=flat-square&logo=apache-kafka&logoColor=white)

DevTrip 플랫폼의 **결제 및 구독 관리**를 담당하는 마이크로서비스입니다. Stripe 연동을 통한 구독/티켓 결제, 결제 이력 관리, Webhook 처리를 제공합니다.

## 기술 스택

### Backend Framework
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.x-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![Spring Data JPA](https://img.shields.io/badge/Spring%20Data%20JPA-3.x-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-8.5-02303A?style=for-the-badge&logo=gradle&logoColor=white)

### Database & External APIs
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![Stripe](https://img.shields.io/badge/Stripe-API%20v2023-008CDD?style=for-the-badge&logo=stripe&logoColor=white)

### Messaging & Monitoring
![Apache Kafka](https://img.shields.io/badge/Apache%20Kafka-3.6-231F20?style=for-the-badge&logo=apache-kafka&logoColor=white)
![Prometheus](https://img.shields.io/badge/Prometheus-Monitoring-E6522C?style=for-the-badge&logo=prometheus&logoColor=white)

## 주요 기능

- **구독 관리**: 월간/연간 구독 플랜 관리
- **티켓 결제**: 일회성 미션 티켓 구매
- **Stripe 연동**: 안전한 결제 처리 및 Webhook 관리
- **결제 이력**: 모든 거래 내역 추적 및 관리
- **자동 갱신**: 구독 자동 갱신 및 실패 처리

## 로컬 실행

### 환경 설정
```bash
cd BE-payment-service

# 환경변수 설정
cp .env.example .env
```

**필수 환경변수:**
```bash
# Stripe 설정
STRIPE_SECRET_KEY=sk_test_...
STRIPE_PUBLISHABLE_KEY=pk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Database
DB_USERNAME=devtrip
DB_PASSWORD=your_password
DB_NAME=devtrip-payment

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
```

### 데이터베이스 설정
```sql
CREATE DATABASE `devtrip-payment` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 서비스 실행
```bash
# 빌드 및 실행
./gradlew bootRun --args='--spring.profiles.active=local'
```

## API 엔드포인트

### 구독 관리
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| `GET` | `/api/v1/subscriptions/plans` | 구독 플랜 목록 | ❌ |
| `POST` | `/api/v1/subscriptions` | 구독 생성 | ✅ |
| `GET` | `/api/v1/subscriptions/me` | 내 구독 조회 | ✅ |
| `PUT` | `/api/v1/subscriptions/{id}/cancel` | 구독 취소 | ✅ |
| `POST` | `/api/v1/subscriptions/{id}/resume` | 구독 재개 | ✅ |

### 티켓 관리
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| `GET` | `/api/v1/tickets/balance` | 티켓 잔량 조회 | ✅ |
| `POST` | `/api/v1/tickets/purchase` | 티켓 구매 | ✅ |
| `POST` | `/api/v1/tickets/use` | 티켓 사용 | ✅ |
| `GET` | `/api/v1/tickets/history` | 사용 이력 | ✅ |

### 결제 관리
| Method | Endpoint | 설명 | 인증 |
|--------|----------|------|------|
| `GET` | `/api/v1/payments/history` | 결제 이력 | ✅ |
| `GET` | `/api/v1/payments/{id}` | 결제 상세 | ✅ |
| `POST` | `/api/v1/payments/refund` | 환불 요청 | ✅ |

### Webhook
| Method | Endpoint | 설명 |
|--------|----------|------|
| `POST` | `/api/v1/webhooks/stripe` | Stripe Webhook |

### 시스템
| Method | Endpoint | 설명 |
|--------|----------|------|
| `GET` | `/api/health` | 헬스체크 |
| `GET` | `/actuator/prometheus` | 메트릭 |

## 설정

### 구독 플랜 설정
```yaml
payment:
  plans:
    free:
      name: "Free Plan"
      price: 0
      missions-per-month: 5
      concurrent-sessions: 1
    pro:
      name: "Pro Plan"  
      monthly-price: 19.99
      yearly-price: 199.99
      missions-per-month: 50
      concurrent-sessions: 3
    team:
      name: "Team Plan"
      monthly-price: 49.99
      yearly-price: 499.99
      missions-per-month: 200
      concurrent-sessions: 10
```

### Stripe Webhook 설정
```yaml
stripe:
  webhook:
    events:
      - invoice.payment_succeeded
      - invoice.payment_failed  
      - customer.subscription.deleted
      - customer.subscription.updated
```

## 테스트

```bash
# 단위 테스트
./gradlew test

# Stripe Mock Server를 이용한 통합 테스트
./gradlew integrationTest -Dspring.profiles.active=test

# 테스트 커버리지
./gradlew jacocoTestReport
```

### Stripe 테스트 카드
```
성공: 4242 4242 4242 4242
실패: 4000 0000 0000 0002
3D Secure: 4000 0025 0000 3155
```

## 모니터링

### 주요 메트릭
- **결제 성공률**: `payment_success_rate`
- **구독 전환율**: `subscription_conversion_rate` 
- **월간 반복 수익**: `monthly_recurring_revenue`
- **고객 생존기간 가치**: `customer_lifetime_value`

### 알림 설정
- 결제 실패율 5% 초과
- Webhook 처리 실패
- 구독 취소율 급증
- API 응답시간 2초 초과

## 보안

### 결제 보안 체크리스트
- [ ] Stripe Secret Key 환경변수 관리
- [ ] Webhook 서명 검증 구현
- [ ] PCI DSS 준수 (Stripe 위임)
- [ ] 개인정보 암호화 저장
- [ ] API Rate Limiting 적용
- [ ] 거래 이력 감사 로그

### Webhook 보안
```java
// Stripe Webhook 서명 검증
@PostMapping("/webhooks/stripe")
public ResponseEntity<String> handleWebhook(
    @RequestBody String payload,
    @RequestHeader("Stripe-Signature") String signature) {
    
    if (!stripeWebhookValidator.isValidSignature(payload, signature)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }
    // 처리 로직...
}
```

## 의존성 서비스

### 필수 의존성
- **MySQL**: 결제/구독 데이터 저장
- **Stripe API**: 결제 처리
- **Kafka**: 결제 완료 이벤트 발행

### 연동 서비스
- **User Management**: 사용자 정보 조회
- **Authentication**: JWT 토큰 검증

## 트러블슈팅

### 일반적인 문제

**1. Stripe Webhook 실패**
```bash
# ngrok을 이용한 로컬 테스트
ngrok http 8081

# Stripe Dashboard에서 Webhook URL 업데이트
# https://your-ngrok-url.ngrok.io/api/v1/webhooks/stripe
```

**2. 결제 처리 지연**
```bash
# Kafka Consumer 상태 확인
curl http://localhost:8081/actuator/health
```

**3. 구독 상태 불일치**
```java
// Stripe와 동기화 작업
@Scheduled(fixedRate = 300000) // 5분마다
public void syncSubscriptionStatus() {
    // Stripe API로 실제 상태 조회 후 업데이트
}
```

## 개발 가이드

### 새로운 결제 방식 추가
1. `PaymentMethod` enum에 추가
2. `PaymentProcessor` 인터페이스 구현
3. 관련 설정 및 테스트 추가

### 새로운 구독 플랜 추가
1. `SubscriptionPlan` 엔티티 수정
2. Stripe Product/Price 생성
3. 프론트엔드 UI 업데이트

## 관련 문서
- [Stripe 연동 가이드](./docs/STRIPE_INTEGRATION.md)
- [결제 플로우 다이어그램](./docs/PAYMENT_FLOW.md)
- [보안 정책](./docs/SECURITY_POLICY.md)