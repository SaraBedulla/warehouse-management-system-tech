# 🏬 Warehouse Management System

A Spring Boot–based backend application for managing warehouse operations, including order handling, user roles, and file attachments using MinIO object storage.

---

## 🚀 Features

* 🔐 Authentication & Authorization (JWT-based)
* 👤 Role-based access:

    * CLIENT
    * WAREHOUSE_MANAGER
    * ADMIN
* 📦 Order management
* 📎 File attachments (stored in MinIO)
* 🗄️ PostgreSQL database integration
* ☁️ S3-compatible storage via MinIO

---

## 🏗️ Tech Stack

* Java 17
* Spring Boot
* Spring Security
* PostgreSQL
* MinIO (S3-compatible storage)
* Maven

---

## ⚙️ Setup Instructions

### 1️⃣ Clone the repository

```bash
git clone https://github.com/your-username/warehouse-management.git
cd warehouse-management
```

---

### 2️⃣ Configure Database

Update `application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/postgres
spring.datasource.username=your_user
spring.datasource.password=your_password
spring.jpa.hibernate.ddl-auto=update
```

---

### 3️⃣ Setup MinIO

#### Run MinIO locally:

```bash
minio.exe server D:\minio-data --console-address ":9001"
```

#### Default access:

* API: http://127.0.0.1:9000
* Console: http://127.0.0.1:9001

#### Configure in application:

```properties
minio.endpoint=http://127.0.0.1:9000
minio.access-key=admin
minio.secret-key=admin123
minio.bucket=orders-attachments
```

---

### 4️⃣ Run the Application

```bash
mvn spring-boot:run
```

App runs at:

```
http://localhost:8080
```

---

## 👥 User Roles

| Role              | Description                    |
| ----------------- | ------------------------------ |
| CLIENT            | Can create and view orders     |
| WAREHOUSE_MANAGER | Manages orders and attachments |
| ADMIN             | Manages users and roles        |

---

## 🔑 Default Users (for development)

| Username | Password | Role              |
| -------- | -------- | ----------------- |
| admin    | admin123 | ADMIN             |
| manager  | 123456   | WAREHOUSE_MANAGER |

> These users are created automatically at startup.

---

## 📡 API Endpoints

### 🔐 Authentication

* `POST /auth/register` → Register as CLIENT
* `POST /auth/login` → Login

---

### 👤 Admin

* `POST /admin/create-manager` → Create warehouse manager

---

### 📦 Orders

* `POST /orders` → Create order
* `GET /orders/{id}` → Get order

---

### 📎 Attachments

* `POST /orders/{orderId}/attachments` → Upload attachment

---

## 📁 File Storage Structure (MinIO)

```
orders/
 └── {orderId}/
      └── filename.ext
```

---

## 🔒 Security

* Passwords are hashed using BCrypt
* Role-based authorization using Spring Security
* Protected endpoints with `@PreAuthorize`

---

## 🧪 Development Notes

* MinIO must be running before starting the app
* Bucket is created automatically if it does not exist
* Use Postman or Swagger to test endpoints

---

## 📌 Future Improvements

* ✅ Email notifications for user onboarding
* ⏳ Password reset flow
* ⏳ File download & delete endpoints
* ⏳ Pagination & filtering for orders

---


