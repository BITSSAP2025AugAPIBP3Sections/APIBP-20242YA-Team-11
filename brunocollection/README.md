# OpenShop API - Bruno Collection

This is a comprehensive Bruno API collection for testing the OpenShop API endpoints.

## Getting Started

1. **Install Bruno**: Download and install Bruno from [usebruno.com](https://www.usebruno.com/)

2. **Open Collection**: Open Bruno and load this collection folder

3. **Configure Environment**: 
   - Select the "Local" environment
   - The default base URL is `http://localhost:8080`
   - Update `baseUrl` in `environments/Local.bru` if your API runs on a different port

## Collection Structure

The collection is organized into the following folders:

### 🔐 Auth
Authentication endpoints for user login and registration
- **Login**: Authenticate with username/email and password
- **Register**: Create a new user account

### 👤 User
User profile management endpoints
- **Get My Profile**: Fetch current user details
- **Update My Profile**: Update user profile
- **Delete User (Admin)**: Admin-only user deletion

### 📦 ProductGraphQL
Product management via GraphQL API
- **Get Product by ID**: Fetch single product
- **Get All Products**: List products with pagination and filtering
- **Create Product**: Create new product (SELLER role)
- **Update Product**: Update existing product (SELLER role)
- **Delete Product**: Delete product (SELLER role)

### 🛒 Cart
Shopping cart operations
- **Get Cart**: View current cart
- **Add Item to Cart**: Add products to cart
- **Remove Item from Cart**: Remove specific items
- **Clear Cart**: Empty the entire cart
- **Checkout Cart**: Process checkout and create order

### 📋 Order
Order management endpoints
- **Get My Orders**: List user's orders with pagination
- **Get Order by ID**: View order details
- **Update Order Status**: Cancel orders or update status (ADMIN)

### 📊 Inventory
Inventory and stock management
- **Create Inventory**: Initialize inventory for product (SELLER)
- **Get Inventory by Product**: View stock levels (public)
- **Increase Stock**: Add inventory (SELLER)
- **Decrease Stock**: Remove inventory (SELLER)

### 💳 Payment
Payment processing endpoints
- **Initiate Payment**: Start payment transaction
- **Get Payment Status**: Check payment status
- **Confirm Payment**: Mark payment as successful
- **Fail Payment**: Mark payment as failed
- **Cancel Payment**: Cancel/refund payment

### 🚚 Shipping
Shipping and delivery management
- **Create Shipment**: Create shipment for order
- **Get Shipment by Order**: View shipment details
- **Update Shipment Status**: Update delivery status (SELLER/ADMIN)

## Authentication

Most endpoints require authentication using JWT tokens. The collection handles this automatically:

1. Run the **Login** request first
2. The token is automatically saved to the `authToken` environment variable
3. Subsequent requests use this token via Bearer authentication

### Default Test Users

The application comes with pre-seeded test users:

**Customer Account:**
- Username: `customer1`
- Password: `password123`

**Seller Account:**
- Username: `seller1`  
- Password: `password123`

**Admin Account:**
- Username: `admin`
- Password: `admin123`

## Environment Variables

The collection uses the following environment variables:

- `baseUrl`: API base URL (default: `http://localhost:8080`)
- `authToken`: JWT authentication token (auto-populated after login)
- `userId`: Current user ID (auto-populated after login)
- `productId`: Product ID for testing (auto-populated after product creation)
- `orderId`: Order ID for testing (auto-populated after checkout)
- `cartItemId`: Cart item ID for removal
- `shipmentId`: Shipment ID for tracking

## API Features

### Role-Based Access Control
- **CUSTOMER**: Shopping cart, orders, checkout
- **SELLER**: Product and inventory management
- **ADMIN**: User management, order status updates

### GraphQL Support
Product operations are available via GraphQL at `/graphql` endpoint with:
- Queries: product, products, myProducts, searchProducts
- Mutations: createProduct, updateProduct, deleteProduct
- GraphiQL interface available at `/graphiql`

### Pagination
List endpoints support pagination with parameters:
- `page`: Page number (0-indexed)
- `size`: Items per page
- `sortBy`: Field to sort by
- `sortDir`: Sort direction (ASC/DESC)

### Idempotency
Payment endpoints support idempotency keys to prevent duplicate transactions.

## Workflow Example

### Complete Purchase Flow

1. **Login** as customer
2. **Get All Products** (GraphQL) - Browse available products
3. **Get Inventory by Product** - Check stock availability
4. **Add Item to Cart** - Add product to cart
5. **Get Cart** - View cart contents
6. **Checkout Cart** - Create order
7. **Initiate Payment** - Start payment process
8. **Confirm Payment** - Complete payment
9. **Create Shipment** - Initiate delivery
10. **Get Order by ID** - Track order status

### Seller Flow

1. **Login** as seller
2. **Create Product** (GraphQL) - Add new product
3. **Create Inventory** - Set initial stock
4. **Increase Stock** - Add more inventory
5. **Update Shipment Status** - Update delivery status

## Technical Details

- **API Version**: v1
- **Base URL**: http://localhost:8080
- **Authentication**: JWT Bearer Token
- **GraphQL Endpoint**: /graphql
- **API Documentation**: Available at /swagger-ui.html

## Support

For issues or questions:
- Check API logs for detailed error messages
- Ensure the application is running on the configured port
- Verify authentication token is valid and not expired
- Check role permissions for restricted endpoints

## Notes

- JWT tokens expire after a configured time period
- Most endpoints return appropriate HTTP status codes
- Error responses include detailed messages
- The Product REST controller is currently commented out; use GraphQL for product operations
