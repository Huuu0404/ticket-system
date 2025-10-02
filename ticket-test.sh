#!/bin/bash

# 參數
MODE=$1          # db / async
STOCK=$2         # 初始庫存
REQUESTS=$3      # 總請求數
CONCURRENCY=$4   # 併發數

if [ -z "$MODE" ] || [ -z "$STOCK" ] || [ -z "$REQUESTS" ] || [ -z "$CONCURRENCY" ]; then
  echo "用法: $0 [db|async] [stock數量] [request數量] [concurrency數量]"
  exit 1
fi

echo "=== 測試模式: $MODE ==="
echo "初始庫存: $STOCK, 總請求數: $REQUESTS, 併發數: $CONCURRENCY"
echo

# Step 1. 登入拿 Token
TOKEN=$(curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "testuser", "password": "123456"}' -s | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$TOKEN" ]; then
  echo "❌ 無法取得 TOKEN，請確認服務有跑起來"
  exit 1
fi

echo "✅ 取得 Token: $TOKEN"
echo

# Step 2. 初始化資料庫
echo "=== 初始化資料庫 ==="
docker exec ticket-postgres psql -U postgres -d ticketdb -c "DELETE FROM orders;"
docker exec ticket-postgres psql -U postgres -d ticketdb -c "DELETE FROM tickets;"
docker exec ticket-postgres psql -U postgres -d ticketdb -c "
INSERT INTO tickets (id, name, description, price, stock, available_stock, version, created_at, updated_at) 
VALUES 
(1, '周杰倫嘉年華', '2024最終場', 2500.00, $STOCK, $STOCK, 0, NOW(), NOW());"

if [ "$MODE" = "async" ]; then
  echo "初始化 Redis..."
  docker exec ticket-redis redis-cli SET "ticket:stock:1" $STOCK > /dev/null
fi

echo "=== 初始化完成 ==="
INIT_STOCK=$(docker exec ticket-postgres psql -U postgres -d ticketdb -t -c "select available_stock from tickets where id=1;" | xargs)
INIT_ORDERS=$(docker exec ticket-postgres psql -U postgres -d ticketdb -t -c "select count(*) from orders;" | xargs)
echo "📦 DB 初始可用庫存: $INIT_STOCK"
echo "📝 DB 初始訂單數: $INIT_ORDERS"
if [ "$MODE" = "async" ]; then
  INIT_REDIS=$(docker exec ticket-redis redis-cli GET "ticket:stock:1")
  echo "📦 Redis 初始庫存: $INIT_REDIS"
fi
echo

# Step 3. 壓測
echo "=== 開始壓測 ==="
if [ "$MODE" = "db" ]; then
  hey -n $REQUESTS -c $CONCURRENCY \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -m POST \
    -d '{}' \
    "http://localhost:8080/api/tickets/1/purchase-db-only?quantity=1"
else
  hey -n $REQUESTS -c $CONCURRENCY \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -m POST \
    -d '{}' \
    "http://localhost:8080/api/tickets/1/purchase-async?quantity=1"
fi
echo

# Step 4. 結果檢查
echo "=== 測試結果 ==="

if [ "$MODE" = "async" ]; then
  echo "⏳ 等待 MQ 消費 (15 秒)"
  sleep 15
fi

FINAL_STOCK=$(docker exec ticket-postgres psql -U postgres -d ticketdb -t -c "select available_stock from tickets where id=1;" | xargs)
echo "📦 DB 剩餘庫存: $FINAL_STOCK"

echo "📝 訂單狀態統計:"
docker exec ticket-postgres psql -U postgres -d ticketdb -c "select o.status, count(*) from orders o group by o.status;"

if [ "$MODE" = "async" ]; then
  FINAL_REDIS=$(docker exec ticket-redis redis-cli GET "ticket:stock:1")
  echo "📦 Redis 剩餘庫存: $FINAL_REDIS"

  SUCCESS=$(docker logs ticket-app 2>&1 | grep -E "訂單處理成功" | wc -l | xargs)
  FAIL=$(docker logs ticket-app 2>&1 | grep -E "訂單處理失敗" | wc -l | xargs)
  START=$(docker logs ticket-app 2>&1 | grep -E "開始處理訂單" | wc -l | xargs)
  echo "📨 MQ 訂單進入佇列數: $START"
  echo "✅ MQ 處理成功: $SUCCESS"
  echo "❌ MQ 處理失敗: $FAIL"
fi

