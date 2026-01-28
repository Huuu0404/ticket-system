# **System Architecture**

### Redis + Message Queue
![未命名繪圖](https://github.com/user-attachments/assets/24479574-3f83-4845-9326-30563ff52d58)



### Database-Only
<img width="649" height="511" alt="未命名繪圖" src="https://github.com/user-attachments/assets/d7dfe758-05dc-4e8a-8880-9c9aeb23ec97" />

# **Performance**

### **Test Configuration**

* **Environment**: Local Docker containers
* **Configuration**: Default thread pool, no special tuning
* **Concurrency**: 200 concurrent users
* **Total Requests**: 50,000
* **Available Tickets**: 100 tickets


| Metric                | Database-Only   | Redis + Message Queue | Improvement        |
| --------------------- | --------------- | --------------------- | ------------------ |
| **99% Response Time** | 3.218s          | 1.059s                | **3x Faster**      |
| **Database Load**     | 50,000 requests | 127 requests          | **394x Reduction** |
| **Over-Selling**      | 0               | 0                     | **Both Perfect**   |

# **Tech Stack**

### Backend

* Java 17 + Spring Boot 3.2
* JWT Authentication

### Data & Cache

* PostgreSQL 15
* Redis 7.4
* RabbitMQ 3.13

### Infrastructure

* Docker + Docker Compose - Containerization
* Maven - Dependency management

# **Quick Start**

### Prerequisites

* Docker
* Java 17
* hey

```bash
git clone git@github.com:Huuu0404/ticket-system.git
cd ticket-system

# Cleanup images and volumes
docker system prune
docker system prune -a --volumes

# Start load testing environment
docker compose up

# Database-Only Mode
./ticket-test.sh db 100 50000 200

# Redis + Message Queue Mode
./ticket-test.sh async 100 50000 200

# Check database results
docker exec ticket-postgres psql -U postgres -d ticketdb -c "
SELECT order_sn, status, remarks, created_at 
FROM orders 
ORDER BY created_at;"

# Check application logs for order status
docker logs ticket-app | grep -E "(✅ 搶票請求已發送MQ|❌ 訂單處理失敗|✅ 訂單處理成功)"

# Arguments:
# 1: Inventory size
# 2: Total number of requests
# 3: Concurrency level
```
