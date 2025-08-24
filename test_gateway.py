#!/usr/bin/env python3
import requests
import json

def test_gateway():
    # Test token
    token = "test-token-user123"
    
    # Test 1: GET request
    print("=== Test 1: GET /gateway/payment/api/v1/test/health ===")
    try:
        response = requests.get(
            "http://localhost:8080/gateway/payment/api/v1/test/health",
            headers={"Authorization": f"Bearer {token}"}
        )
        print(f"Status: {response.status_code}")
        print(f"Headers: {dict(response.headers)}")
        print(f"Response: {response.text}")
        print()
    except Exception as e:
        print(f"Error: {e}")
        print()
    
    # Test 2: POST request for billing key
    print("=== Test 2: POST /gateway/payment/api/toss/billing/issue ===")
    try:
        payload = {
            "authKey": "test_auth_key_12345",
            "customerKey": "test_customer_user123"
        }
        response = requests.post(
            "http://localhost:8080/gateway/payment/api/toss/billing/issue",
            headers={
                "Authorization": f"Bearer {token}",
                "Content-Type": "application/json"
            },
            json=payload
        )
        print(f"Status: {response.status_code}")
        print(f"Headers: {dict(response.headers)}")
        print(f"Response: {response.text}")
        print()
    except Exception as e:
        print(f"Error: {e}")
        print()
    
    # Test 3: Direct Payment Service call
    print("=== Test 3: Direct call to Payment Service ===")
    try:
        response = requests.get("http://localhost:8081/api/v1/test/health")
        print(f"Status: {response.status_code}")
        print(f"Response: {response.text}")
        print()
    except Exception as e:
        print(f"Error: {e}")
        print()

if __name__ == "__main__":
    test_gateway()