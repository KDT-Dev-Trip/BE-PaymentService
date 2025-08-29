# DevTrip Payment Service API 명세서

## 개요
DevTrip 플랫폼의 결제, 구독, 티켓 관리를 담당하는 Payment Service API 명세서입니다.

**Base URL**: `http://localhost:8081` (개발환경)

## 인증
Gateway 기반 인증을 사용하며, 다음 헤더가 필요합니다:
- `X-User-Id`: 사용자 ID (UUID)
- `X-User-Email`: 사용자 이메일
- `X-User-Role`: 사용자 역할 (USER, ADMIN)

## 1. 구독 관리 API

### 1.1 구독 생성
```http
POST /api/subscriptions?userId={userId}
Content-Type: application/json

{
  "planId": 1,
  "billingCycle": "MONTHLY",
  "startTrial": false,
  "trialDays": 0
}
```

**Parameters:**
- `userId` (Query String, Required): 사용자 ID

**Request Body:**
- `planId` (Long, Required): 구독 플랜 ID
- `billingCycle` (String, Required): 청구 주기 (MONTHLY, YEARLY)
- `startTrial` (Boolean): 무료 체험 시작 여부
- `trialDays` (Integer): 체험 기간 (일)
- `teamId` (Long): 팀 ID (선택사항)

**Response:**
```json
{
  "id": 1,
  "userId": 12345,
  "status": "ACTIVE",
  "plan": {
    "id": 1,
    "planName": "Pro Plan",
    "price": 29.99,
    "currency": "USD"
  },
  "billingCycle": "MONTHLY",
  "startDate": "2025-08-29T08:00:00Z",
  "endDate": "2025-09-29T08:00:00Z"
}
```

### 1.2 결제 체크아웃 세션 생성
```http
POST /api/subscriptions/checkout?userId={userId}
Content-Type: application/json

{
  "planId": 1,
  "billingCycle": "MONTHLY",
  "successUrl": "https://example.com/success",
  "cancelUrl": "https://example.com/cancel"
}
```

**Response:**
```json
{
  "checkoutUrl": "https://checkout.stripe.com/...",
  "apiType": "REAL_BUSINESS_API",
  "operation": "CREATE_CHECKOUT_SESSION"
}
```

### 1.3 사용자 구독 조회
```http
GET /api/subscriptions/users/{userId}
```

**Parameters:**
- `userId` (Path, Required): 사용자 ID

**Response:**
```json
[
  {
    "id": 1,
    "userId": 12345,
    "status": "ACTIVE",
    "plan": {
      "id": 1,
      "planName": "Pro Plan",
      "price": 29.99,
      "currency": "USD"
    },
    "billingCycle": "MONTHLY",
    "startDate": "2025-08-29T08:00:00Z",
    "endDate": "2025-09-29T08:00:00Z"
  }
]
```

### 1.4 구독 취소
```http
PUT /api/subscriptions/{subscriptionId}/cancel
```

**Parameters:**
- `subscriptionId` (Path, Required): 구독 ID

**Response:**
```json
{
  "id": 1,
  "userId": 12345,
  "status": "CANCELLED",
  "plan": {...},
  "cancelledAt": "2025-08-29T10:00:00Z"
}
```

## 2. 구독 플랜 API

### 2.1 활성 플랜 목록 조회
```http
GET /api/subscription-plans
```

**Response:**
```json
[
  {
    "id": 1,
    "planName": "Basic Plan",
    "description": "기본 플랜",
    "price": 9.99,
    "currency": "USD",
    "planType": "BASIC",
    "ticketsPerMonth": 100,
    "isActive": true
  },
  {
    "id": 2,
    "planName": "Pro Plan",
    "description": "프로 플랜",
    "price": 29.99,
    "currency": "USD",
    "planType": "PRO",
    "ticketsPerMonth": 500,
    "isActive": true
  }
]
```

### 2.2 특정 플랜 조회
```http
GET /api/subscription-plans/{planId}
```

### 2.3 플랜 타입별 조회
```http
GET /api/subscription-plans/types/{planType}
```

**Parameters:**
- `planType`: BASIC, PRO, ENTERPRISE

## 3. 티켓 관리 API

### 3.1 사용자 티켓 조회
```http
GET /api/tickets/users/{userId}
```

**Response:**
```json
{
  "userId": 12345,
  "currentTickets": 450,
  "monthlyAllowance": 500,
  "lastRefillDate": "2025-08-01T00:00:00Z",
  "nextRefillDate": "2025-09-01T00:00:00Z"
}
```

### 3.2 티켓 사용
```http
POST /api/tickets/users/{userId}/use?amount=5&attemptId=123&reason=mission_start
```

**Parameters:**
- `userId` (Path, Required): 사용자 ID
- `amount` (Query, Required): 사용할 티켓 수
- `attemptId` (Query): 시도 ID (중복 방지용)
- `reason` (Query): 사용 사유

**Response:**
```json
{
  "success": true,
  "message": "Tickets used successfully",
  "tickets": {
    "userId": 12345,
    "currentTickets": 445,
    "monthlyAllowance": 500
  },
  "apiType": "REAL_BUSINESS_API",
  "eventTriggered": "Automatic low balance event check performed"
}
```

### 3.3 티켓 환불
```http
POST /api/tickets/users/{userId}/refund?amount=5&attemptId=123&reason=mission_failed
```

**Response:**
```json
{
  "success": true,
  "message": "Tickets refunded successfully",
  "tickets": {
    "userId": 12345,
    "currentTickets": 450,
    "monthlyAllowance": 500
  },
  "apiType": "REAL_BUSINESS_API",
  "operation": "TICKET_REFUND"
}
```

### 3.4 티켓 조정 (관리자용)
```http
POST /api/tickets/users/{userId}/adjust?adjustment=100&reason=bonus_tickets
```

**Parameters:**
- `adjustment`: 조정할 티켓 수 (양수/음수 가능)

### 3.5 티켓 자동 충전 (스케줄러용)
```http
POST /api/tickets/refill
```

## 4. TossPayments API

### 4.1 빌링키 발급
```http
POST /api/toss/billing/issue
Content-Type: application/json

{
  "customerKey": "customer_key_123",
  "cardNumber": "1234567890123456",
  "cardExpiryMonth": "12",
  "cardExpiryYear": "25",
  "cardPassword": "12",
  "customerBirth": "900101",
  "customerName": "홍길동"
}
```

### 4.2 빌링키로 결제
```http
POST /api/toss/billing/{billingKey}/payment
Content-Type: application/json

{
  "customerKey": "customer_key_123",
  "amount": 29990,
  "orderId": "order_123",
  "orderName": "Pro Plan 구독"
}
```

### 4.3 체크아웃 결제
```http
POST /api/toss/payments/checkout
```

### 4.4 결제 승인
```http
POST /api/toss/payments/confirm
```

## 5. 헬스체크 API

### 5.1 표준 헬스체크
```http
GET /api/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "BE-payment-service",
  "version": "1.0.0",
  "timestamp": "2025-08-29T08:00:00Z",
  "details": {
    "features": {
      "subscriptions": "enabled",
      "tickets": "enabled",
      "stripe": "enabled",
      "kafka": "enabled",
      "gateway-auth": "enabled"
    }
  }
}
```

### 5.2 서비스 정보
```http
GET /api/v1/info
```

## 6. 테스트 API

### 6.1 테스트용 플랜 조회
```http
GET /api/test/plans
```

### 6.2 테스트용 헬스체크
```http
GET /api/test/health
```

## 응답 코드

| 코드 | 설명 |
|------|------|
| 200 | 성공 |
| 201 | 생성 완료 |
| 400 | 잘못된 요청 |
| 401 | 인증 실패 |
| 403 | 권한 없음 |
| 404 | 리소스 없음 |
| 409 | 충돌 (중복 요청) |
| 500 | 서버 오류 |
| 501 | 구현되지 않음 |
| 503 | 서비스 사용 불가 |

## 에러 응답 형식

```json
{
  "timestamp": "2025-08-29T08:00:00.000+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for object='createSubscriptionRequest'. Error count: 1",
  "path": "/api/subscriptions"
}
```

## 주요 이벤트

Payment Service는 다음 이벤트를 발행합니다:

1. **구독 관련**
   - `subscription.created`: 구독 생성
   - `subscription.renewed`: 구독 갱신
   - `subscription.cancelled`: 구독 취소
   - `subscription.expired`: 구독 만료

2. **티켓 관련**
   - `ticket.used`: 티켓 사용
   - `ticket.refunded`: 티켓 환불
   - `ticket.balance.low`: 잔여 티켓 부족
   - `ticket.refilled`: 티켓 자동 충전

3. **결제 관련**
   - `payment.completed`: 결제 완료
   - `payment.failed`: 결제 실패
   - `payment.refunded`: 결제 환불

## 참고사항

- 모든 API는 Gateway를 통해 호출되며, 직접 호출 시 인증이 필요합니다.
- userId는 UUID 형태의 문자열로 전달되며, 내부적으로 Long 타입으로 변환됩니다.
- 티켓 시스템은 매월 자동 충전되며, 구독 플랜에 따라 월 허용량이 달라집니다.
- 실시간 이벤트 시스템을 통해 다른 서비스와 연동됩니다.