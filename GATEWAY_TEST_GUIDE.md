# 🧪 Gateway를 통한 토스페이먼츠 API 테스트 가이드

## 🚀 빠른 테스트 방법

### 1. 테스트 토큰 사용
Authentication Service에는 테스트용 토큰 기능이 구현되어 있습니다:

```bash
# 테스트 토큰 형식: test-token-{userId}
Authorization: Bearer test-token-user123
```

### 2. 서비스 실행
```bash
# Terminal 1: Authentication Service 실행
cd /Users/symoon/IdeaProjects/workspace/BE-AuthenticationService
./gradlew bootRun

# Terminal 2: Payment Service 실행  
cd /Users/symoon/IdeaProjects/workspace/BE-PaymentService
./gradlew bootRun
```

### 3. 테스트 API 호출

#### 빌링키 발급 테스트
```bash
curl -X POST http://localhost:8080/gateway/payment/api/toss/billing/issue \\
  -H "Authorization: Bearer test-token-user123" \\
  -H "Content-Type: application/json" \\
  -d '{
    "authKey": "test_auth_key_12345",
    "customerKey": "customer_user123_$(date +%s)"
  }'
```

#### 자동결제 테스트
```bash
curl -X POST http://localhost:8080/gateway/payment/api/toss/billing/test_billing_key_123/payment \\
  -H "Authorization: Bearer test-token-user123" \\
  -H "Content-Type: application/json" \\
  -d '{
    "amount": 10000,
    "customerKey": "customer_user123",
    "orderId": "order_$(date +%s)",
    "orderName": "테스트 구독 결제",
    "customerEmail": "user123@example.com",
    "customerName": "테스트 사용자"
  }'
```

## 🔧 실제 JWT 토큰 발급 (선택사항)

### 1. 회원가입
```bash
curl -X POST http://localhost:8080/auth/signup \\
  -H "Content-Type: application/json" \\
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "fullName": "테스트 사용자"
  }'
```

### 2. 로그인하여 JWT 토큰 받기
```bash
curl -X POST http://localhost:8080/auth/login \\
  -H "Content-Type: application/json" \\
  -d '{
    "email": "test@example.com", 
    "password": "password123"
  }'
```

응답에서 `accessToken`을 복사하여 사용:
```bash
curl -X POST http://localhost:8080/gateway/payment/api/toss/billing/issue \\
  -H "Authorization: Bearer {실제_JWT_토큰}" \\
  -H "Content-Type: application/json" \\
  -d '{...}'
```

## 🎯 테스트 시나리오

### 시나리오 1: 정상적인 빌링키 발급
```bash
# 1. 빌링키 발급 요청
curl -X POST http://localhost:8080/gateway/payment/api/toss/billing/issue \\
  -H "Authorization: Bearer test-token-user123" \\
  -H "Content-Type: application/json" \\
  -d '{
    "authKey": "test_auth_key_12345",
    "customerKey": "customer_user123_unique"
  }'

# 예상 결과: 400 Bad Request (실제 토스 API 호출 실패, 정상)
# Gateway 인증 및 라우팅은 정상 작동
```

### 시나리오 2: 인증 없이 접근
```bash
# Authorization 헤더 없이 요청
curl -X POST http://localhost:8080/gateway/payment/api/toss/billing/issue \\
  -H "Content-Type: application/json" \\
  -d '{...}'

# 예상 결과: 403 Forbidden
```

### 시나리오 3: 잘못된 토큰으로 접근
```bash
curl -X POST http://localhost:8080/gateway/payment/api/toss/billing/issue \\
  -H "Authorization: Bearer invalid-token" \\
  -H "Content-Type: application/json" \\
  -d '{...}'

# 예상 결과: 401 Unauthorized 또는 403 Forbidden
```

## 📋 체크리스트

### 성공 조건
- [ ] Authentication Service (포트 8080) 실행 중
- [ ] Payment Service (포트 8081) 실행 중  
- [ ] 테스트 토큰으로 Gateway 접근 가능
- [ ] Gateway에서 Payment Service로 요청 전달
- [ ] Payment Service에서 토스 API 호출 시도 (실패해도 정상)

### 로그 확인 포인트
```bash
# Authentication Service 로그
"Proxying POST /gateway/payment to payment service (port 8081) with user: test-user@example.com"

# Payment Service 로그  
"Billing key issue request received for customerKey: customer_user123_unique"
"X-User-Id: user123"
"X-User-Email: test-user@example.com"
"X-Gateway-Auth: true"
```

## 🚨 문제 해결

### 403 Forbidden 오류
- JWT 토큰 확인: `Authorization: Bearer test-token-user123`
- Security 설정 확인: `/gateway/**` 경로가 인증 필요

### 502 Bad Gateway 오류
- Payment Service 실행 상태 확인
- 포트 8081 사용 가능 여부 확인

### Connection Refused
- 각 서비스의 포트 및 실행 상태 확인
- 방화벽 설정 확인

토스페이먼츠 실제 API 호출은 유효한 authKey와 customerKey가 필요하므로, 테스트에서는 400 에러가 예상되지만 Gateway 인증과 라우팅이 정상 작동하는지 확인하는 것이 목표입니다.