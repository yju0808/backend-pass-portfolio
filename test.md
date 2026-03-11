

## 성능 측정 결과

### 복합 인덱스 개선
**API**: `localhost:8080/api/chapter3/orders/complex-search?startDate=2024-01-01T00:00:00&status=COMPLETED&minAmount=100000&page=0&size=20`

| 구분 | 응답 시간 (ms) |
|------|---------------|
| 개선 전 | 395, 336, 377, 343, 335 |
| 개선 후 | 175, 177, 183, 224, 169 |

### 통계 쿼리 개선
**API**: `localhost:8080/api/chapter3/orders/stats?minAmount=50000&page=0&size=20`

| 구분 | 응답 시간 |
|------|----------|
| 개선 전 | 3.61s, 3.81s, 3.71s |
| 개선 후 | 147, 300, 91 |

### 실행 계획 (EXPLAIN)

1 SIMPLE o ALL FKimoar5do6m17xoh9mwvwn7d9l 497640 1 SIMPLE m eq_ref PRIMARY,UK_fi2xc3n414vh8fj4nv28w8a3n PRIMARY 8 portfolio.o.member_id 1

| id | select_type | table | type | possible_keys | key | key_len | ref | rows | Extra |
|---|---|---|---|---|---|---|---|---|---|
| 1 | SIMPLE | o | ALL | FKimoar5do6m17xoh9mwvwn7d9l | | | | 497640 | |
| 1 | SIMPLE | m | eq_ref | PRIMARY,UK_fi2xc3n414vh8fj4nv28w8a3n | PRIMARY | 8 | portfolio.o.member_id | 1 | |

강의와는 달리 드라이빙 테이블이 o가 됨



### n+1 문제 해결

| 구분 | 응답 시간 |
|------|----------|
| 개선 후 | 175, 177, 183, 224, 169 |
| 개선 후 | 154, 156, 148, 158, 156 |

큰 차이는 없다


## 기존 API N+1 발생 원리

### 1. 코드 흐름
```java
// OrderService.java
private OrderResponse toOrderResponse(Order order) {
    return OrderResponse.builder()
        .memberEmail(order.getMember().getEmail())  // ← Member 조회 발생!
        .totalItems(order.getOrderItems().size())   // ← OrderItems 조회 발생!
        .build();
}
```

### 2. 실제 실행되는 쿼리

```
1️⃣ 메인 쿼리 (1번)
SELECT * FROM ch3_orders WHERE ... LIMIT 20

2️⃣ Member 조회 (20번) - order.getMember().getEmail() 호출 시
SELECT * FROM ch2_members WHERE id = 1
SELECT * FROM ch2_members WHERE id = 2
SELECT * FROM ch2_members WHERE id = 3
... (20번 반복)

3️⃣ OrderItems 조회 (20번) - order.getOrderItems().size() 호출 시  
SELECT * FROM ch3_order_items WHERE order_id = 100
SELECT * FROM ch3_order_items WHERE order_id = 200
SELECT * FROM ch3_order_items WHERE order_id = 300
... (20번 반복)

4️⃣ Count 쿼리 (1번)
SELECT COUNT(*) FROM ch3_orders WHERE ...

= 총 42개 쿼리
```

---

### 3. 그런데 왜 빠름?

| 쿼리 종류 | 조회 방식 | 소요 시간 |
|-----------|-----------|-----------|
| Member 조회 | **PK(id)로 조회** | ~0.001ms |
| OrderItems 조회 | **인덱스(order_id)로 조회** | ~0.001ms |

```
42개 쿼리 × 0.001ms = 0.042ms 추가

→ 사람이 체감 불가능 (0.17초 vs 0.16초)
```

---

### 4. 비유로 이해하기

**기존 API:**
> 마트에서 장 볼 때, 물건 하나 집을 때마다 계산대 왔다갔다 (42번)  
> 근데 계산대가 바로 옆이라 1초면 됨 → 총 42초

**개선 API:**
> 카트에 다 담고 한번에 계산 (2번)  
> → 총 2초

**차이:** 42초 vs 2초 → 체감상 둘 다 빠름 (1분 이내)

---

### 결론

| | 기존 | 개선 |
|--|------|------|
| 쿼리 수 | 42개 | 2개 |
| 실제 시간 | ~0.17초 | ~0.16초 |
| 차이 | **거의 없음** | |
| 이유 | PK/인덱스 조회가 너무 빨라서 | |

**N+1이 항상 느린 건 아님!** 인덱스가 잘 걸려있으면 체감 안됨.