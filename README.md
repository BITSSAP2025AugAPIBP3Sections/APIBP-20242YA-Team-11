# 🚀 OpenShop - E-commerce Platform

---

## 📚 Table of Contents
1. Product Overview
2. API-First Approach
3. Stakeholders
4. Core Functionalities
5. Scope In
6. Scope Out
7. Prerequisites
8. Quick Start Guide
9. API Endpoints
10. C4 Model Diagrams

---

## 1. Product Overview

OpenShop is a robust, API-driven e-commerce backend designed to facilitate seamless interaction between buyers and sellers. Built as a monolithic application for streamlined deployment, it provides a unified interface for catalog management, shopping cart operations, and order processing. The project demonstrates a hybrid API implementation, utilizing REST for transactional resources (Users, Orders, Cart) and GraphQL for flexible, high-volume data fetching (Product Catalog).

---

## 2. API-First Approach

We adopted an API-first approach by designing the OpenAPI (Swagger) and GraphQL schema before implementation. This allowed us to:

- **Stakeholder Analysis**: We first identified the core needs of Customers (browsing, buying) and Sellers (inventory management).
- **Schema Definition**: Before writing any Java code, we defined the interface contracts. We used OpenAPI 3.0 (Swagger) to model REST resources and defined a GraphQL Schema (SDL) for the product catalog.
- **Mocking & Validation**: These schemas allowed us to validate the data flow and business logic requirements (Scope In/Out) before implementation, ensuring the endpoints met the requirements of our stakeholders.

---

## 3. Stakeholders

- 👤 **Customer**: Authenticated users who browse the catalog, manage their shopping cart, and checkout orders.
- 🛒 **Seller**: Users with elevated permissions responsible for creating products, managing inventory, and updating order statuses.

---

## 4. Core Functionalities

- 🔑 Multi-role Support: Customer and Seller roles with granular permissions  
- 🔐 User Authentication & Registration (JWT)  
- 🛍️ Product Catalog: Full CRUD operations with GraphQL query support  
- 🛒 Shopping Cart: Add, remove, update quantities, checkout  
- 📦 Inventory Management: Real-time stock tracking and reservation  
- 💳 Payment Integration: Mock payment gateway with success/failure simulation  
- 🗄️ Database Persistence: PostgreSQL  
- ⚠️ Exception Handling  
- ✅ Validations  

---

## 5. Scope In

- 👥 **Customer and Seller flows**: Registration, login, product management, cart, order, inventory, and mocked payment  
- 🔄 **Hybrid API Implementation**: REST + GraphQL  
- 🧑‍💻 **User Management**: Registration, Authentication (JWT), Profile management  

---

## 6. Scope Out

- 🏗️ Microservices  
- 🛠️ Admin features  
- 📧 Notifications (Email/SMS)  
- 🚚 Real-time shipping/tracking  
- 💸 Actual payment gateway integration  

---

## 7. Prerequisites

### ⚙️ Required Software

#### For Local Development

```bash
# Java Development Kit (JDK) 17 or higher
java -version

# Apache Maven 3.6 or higher
mvn -version

# Docker Desktop (for PostgreSQL)
docker --version

# Git
git --version
```
### 🖥️ System Requirements

- **RAM**: Minimum 8GB (16GB recommended for full stack)
- **CPU**: Minimum 4 cores
- **Disk Space**: 5GB free space
- **OS**: macOS, Linux, or Windows with WSL2

---

## 8. Quick Start Guide

Follow these steps to set up the database and start the application locally:

### 📝 Step 1: Clone the Repository

```bash
git clone https://github.com/yourusername/openshop.git
cd svc/openshop/src/main/java/com/openshop/
```

### 🐳 Step 2: Start PostgreSQL Container

Run the following command to pull the PostgreSQL image and start it in a Docker container. This maps port 5432 on your machine to the container.

```bash
docker run --name openshop-postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=postgres \
  -p 5432:5432 \
  -d postgres:15
```

### 🛠️ Step 3: Configure the Database

Now, access the running container to create the specific database and user for the application.

1. **Enter the PostgreSQL interactive terminal:**

```bash
docker exec -it openshop-postgres psql -U postgres
```

2. **Run the following SQL commands inside the terminal:** (Copy and paste these commands one by one or as a block)

```sql
-- Create the application database
CREATE DATABASE openshopdb;

-- Create the application user
CREATE USER openshop WITH PASSWORD 'openshop123';

-- Grant privileges to the user on the database
GRANT ALL PRIVILEGES ON DATABASE openshopdb TO openshop;

-- Switch to the new database
\c openshopdb

-- Grant schema privileges
GRANT ALL ON SCHEMA public TO openshop;

-- Ensure future tables are accessible to the user
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO openshop;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO openshop;
```

3. **Exit the terminal:** Type `\q` or press `Ctrl + D`.

### 🚦 Step 4: Run the Application

Navigate to your project root directory (where the `pom.xml` file is located) and run the application using Maven.

```bash
mvn spring-boot:run
```

### 🔗 Step 5: Verify Access

Once the application starts successfully, you can access it at:

- **Base URL**: http://localhost:8080
- **API Endpoints**: http://localhost:8080/api/... (e.g., /api/auth/login)

---

## 9. API Endpoints

| 🗂️ Domain | 🔀 Method Type | 🔗 Endpoint | 📝 Description | 👤 Who can use it |
|-----------|---------------|-------------|---------------|------------------|
| Authentication | POST | `/api/auth/login` | Authenticate user and return JWT token with user details | CUSTOMER, SELLER |
| Authentication | POST | `/api/auth/register` | Register a new user account | CUSTOMER, SELLER |
| Users | GET | `/api/users/me` | Get profile details of the currently logged-in user | CUSTOMER, SELLER |
| Users | PUT | `/api/users/me` | Update profile information of the currently logged-in user | CUSTOMER, SELLER |
| Products (GraphQL) | POST | `/graphql` | GraphQL endpoint for product operations (Queries & Mutations). Queries: Get/Search Products. Mutations: Create/Update/Delete Products. | Queries: CUSTOMER, SELLER; Mutations: SELLER |
| Cart | GET | `/api/cart` | Retrieve the shopping cart for the current user | CUSTOMER |
| Cart | POST | `/api/cart/items` | Add a product to the user's shopping cart | CUSTOMER |
| Cart | DELETE | `/api/cart/items` | Remove all items from the user's cart | CUSTOMER |
| Cart | DELETE | `/api/cart/items/{itemId}` | Remove a specific item from the user's cart | CUSTOMER |
| Cart | POST | `/api/cart/checkout` | Process cart checkout and create an order | CUSTOMER |
| Orders | GET | `/api/orders/user` | Retrieve orders for the current user with pagination | CUSTOMER |
| Orders | GET | `/api/orders/{orderId}` | Retrieve a specific order | CUSTOMER (own), SELLER (related) |
| Orders | PUT | `/api/orders/{orderId}/status` | Update order status (Customer can cancel; Seller can update fulfillment status) | CUSTOMER (cancel only), SELLER |
| Inventory | POST | `/api/inventory` | Create inventory for a product | SELLER |
| Inventory | GET | `/api/inventory/{productId}` | Retrieve inventory information for a specific product | CUSTOMER, SELLER |
| Inventory | PATCH | `/api/inventory/{productId}/increase` | Increase inventory stock for a product | SELLER |
| Inventory | PATCH | `/api/inventory/{productId}/decrease` | Decrease inventory stock for a product | SELLER |
| Payments | POST | `/api/payments` | Initiate a payment for an order | CUSTOMER |
| Payments | GET | `/api/payments/{orderId}` | Retrieve payment status for an order | CUSTOMER (own), SELLER (related) |
| Payments | DELETE | `/api/payments/{orderId}` | Cancel or refund a payment | CUSTOMER (own) |
| Payments | PUT | `/api/payments/{orderId}/confirm` | Mark payment as successful | CUSTOMER, System |
| Payments | PUT | `/api/payments/{orderId}/fail` | Mark payment as failed | System |
| Shipping | POST | `/api/shipping` | Create a new shipment for an order | CUSTOMER, SELLER |
| Shipping | GET | `/api/shipping/{orderId}` | Retrieve shipment information for an order | CUSTOMER (own), SELLER (related) |
| Shipping | PATCH | `/api/shipping/{shipmentId}/status` | Update the status of a shipment | SELLER |
| Notifications | GET | `/api/notifications` | Retrieve notifications for the current user | CUSTOMER, SELLER |

---

# 🗺️ C4 Model Diagrams

## System Context Diagram

![System Context Diagram](assets/system%20context%20diagram.png)

## Container Diagram

![Container Diagram](assets/container%20diagram.png)

## Component Diagram

![Component Diagram](assets/component%20diagram.png)



**Made with ❤️ using Spring Boot and modern API design patterns**
