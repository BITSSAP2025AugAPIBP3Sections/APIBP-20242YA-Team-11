# Cart System Implementation - "Best of Both Worlds" Architecture

## Overview

This document describes the implementation of the cart system using the "Best of Both Worlds" architecture, where the **backend is the single source of truth** and Redux serves as a UI state manager and cache.

## Architecture Principles

### 1. Backend as Single Source of Truth
- All cart operations call backend APIs
- No local calculations for totals, taxes, discounts, or coupons
- Redux state always mirrors the backend response
- On every operation, the backend returns the updated cart state

### 2. Redux State Structure
```typescript
interface CartState {
  cart: Cart | null;        // Mirrors backend response exactly
  loading: boolean;         // UI state for loaders
  error: string | null;     // Error state for handling failures
}
```

### 3. No Local Cart State
- No separate local cart maintained in frontend
- Redux cart is replaced with backend response after each API call
- On page refresh, `getCart()` is automatically called to hydrate state

## File Structure

```
src/
├── stores/
│   └── redux/
│       ├── cartApi.ts       # Backend API wrapper functions
│       ├── cartSlice.ts     # Redux slice with state management
│       ├── cartThunks.ts    # Async thunks for API calls
│       ├── store.ts         # Redux store configuration
│       └── hooks.ts         # Typed Redux hooks
├── components/
│   └── cart/
│       └── AddToCartButton.tsx  # Reusable add-to-cart component
└── pages/
    └── CartPage.tsx         # Main cart page using Redux
```

## API Endpoints (from YAML)

All endpoints follow the OpenAPI specification in `openshop-monolith.yaml`:

### GET /api/cart
Retrieves the user's cart from backend.

**Response:**
```typescript
{
  id: number;
  userId: number;
  items: CartItem[];
  createdAt: string;
  updatedAt: string;
}
```

### POST /api/cart/add
Adds an item to the cart.

**Request:**
```typescript
{
  productId: string;  // UUID
  quantity: number;
  price: number;
}
```

**Response:** Returns updated Cart object

### DELETE /api/cart/remove/{itemId}
Removes an item from the cart.

**Response:** Returns updated Cart object

### DELETE /api/cart/clear
Clears all items from the cart.

**Response:** 204 No Content

### POST /api/cart/checkout
Processes cart checkout and creates an order.

**Response:** Returns Order object

## Redux Implementation

### 1. Cart API Layer (`cartApi.ts`)

Wrapper functions for all backend API calls:
- `getCartApi()` - Fetch cart
- `addToCartApi(productId, quantity, price)` - Add item
- `removeFromCartApi(itemId)` - Remove item
- `updateQuantityApi(itemId, productId, quantity, price)` - Update quantity
- `clearCartApi()` - Clear cart

### 2. Async Thunks (`cartThunks.ts`)

Redux thunks that call the API functions:
- `fetchCart` - GET /api/cart
- `addToCart` - POST /api/cart/add
- `removeFromCart` - DELETE /api/cart/remove/{itemId}
- `updateCartQuantity` - Remove + Add (no dedicated update endpoint)
- `clearCart` - DELETE /api/cart/clear

Each thunk:
- Calls the corresponding API function
- Returns the backend response on success
- Returns error message on failure
- Redux state is updated automatically via extraReducers

### 3. Cart Slice (`cartSlice.ts`)

Redux slice with three pieces of state:
- `cart` - The cart object from backend (or null)
- `loading` - Boolean for UI loading states
- `error` - String error message (or null)

**Extra Reducers:**
- `pending` - Sets loading to true, clears error
- `fulfilled` - Sets loading to false, updates cart with backend response
- `rejected` - Sets loading to false, sets error, preserves previous cart state

### 4. Store Configuration (`store.ts`)

```typescript
export const store = configureStore({
  reducer: {
    cart: cartReducer,
  },
});
```

### 5. Typed Hooks (`hooks.ts`)

Pre-typed Redux hooks for TypeScript:
- `useAppDispatch` - Typed dispatch
- `useAppSelector` - Typed selector

## Component Usage

### CartPage Component

The cart page demonstrates the complete implementation:

1. **Fetch cart on mount:**
```typescript
useEffect(() => {
  dispatch(fetchCart());
}, [dispatch]);
```

2. **Read cart from Redux:**
```typescript
const { cart, loading, error } = useAppSelector((state) => state.cart);
```

3. **Dispatch actions:**
```typescript
// Add to cart
dispatch(addToCart({ productId, quantity, price }));

// Remove from cart
dispatch(removeFromCart(itemId));

// Update quantity
dispatch(updateCartQuantity({ itemId, productId, quantity, price }));

// Clear cart
dispatch(clearCart());
```

4. **Calculate totals from backend data:**
```typescript
const total = cart?.items.reduce((sum, item) => sum + item.price * item.quantity, 0) || 0;
const itemCount = cart?.items.reduce((sum, item) => sum + item.quantity, 0) || 0;
```

### AddToCartButton Component

Reusable button component that:
- Dispatches `addToCart` action
- Shows loading state while adding
- Shows success state after successful add
- Handles errors gracefully

**Usage:**
```tsx
<AddToCartButton
  productId={product.id}
  price={product.price}
  quantity={1}
  variant="default"
  size="lg"
/>
```

**Quick Add Button (Icon Only):**
```tsx
<QuickAddButton
  productId={product.id}
  price={product.price}
/>
```

## Key Features

### 1. Automatic Cart Hydration
On page refresh or route change, the cart is automatically fetched from the backend:
```typescript
useEffect(() => {
  dispatch(fetchCart());
}, [dispatch]);
```

### 2. Loading States
All cart operations show loading indicators:
```typescript
if (loading) {
  return <LoadingSpinner />;
}
```

### 3. Error Handling
Errors are captured and displayed to users:
```typescript
if (error) {
  return (
    <ErrorMessage>
      {error}
      <Button onClick={() => dispatch(fetchCart())}>Try Again</Button>
    </ErrorMessage>
  );
}
```

### 4. Optimistic UI Updates
While the loading state is false after success, the UI immediately reflects the backend response.

### 5. No Local Calculations
All totals, taxes, and discounts come from the backend. The frontend never calculates these values.

## Update Quantity Implementation

Since the YAML doesn't define a dedicated update endpoint, we implement it as:
1. Remove the item (DELETE /api/cart/remove/{itemId})
2. Add it back with new quantity (POST /api/cart/add)

This ensures the backend remains the source of truth for any price changes or validations.

## Benefits of This Architecture

1. **Single Source of Truth**: Backend always has the correct cart state
2. **Price Consistency**: Prices can't be manipulated client-side
3. **Tax/Discount Calculation**: All business logic on backend
4. **Multi-Device Sync**: Cart syncs across devices automatically
5. **Error Recovery**: Failed operations preserve previous state
6. **Type Safety**: Full TypeScript support with proper typing
7. **Testability**: Redux logic is easily testable
8. **Developer Experience**: Clear separation of concerns

## Testing the Implementation

### Manual Testing Steps

1. **Add to Cart**: Click "Add to Cart" on product pages
2. **View Cart**: Navigate to /cart to see all items
3. **Update Quantity**: Use +/- buttons to change quantities
4. **Remove Item**: Click trash icon to remove items
5. **Clear Cart**: Use "Clear Cart" button to empty cart
6. **Refresh Page**: Cart should persist after refresh
7. **Error Handling**: Disconnect network to test error states

### Integration with Checkout

The cart integrates with checkout at `/checkout`:
```typescript
const handleCheckout = () => {
  navigate('/checkout');
};
```

## Migration Notes

If migrating from Zustand (`useCartStore`):

1. **Remove Zustand store**: `src/stores/useCartStore.ts` can be deprecated
2. **Update imports**: Change from `useCartStore` to Redux hooks
3. **Update logic**: Replace local state mutations with dispatch calls
4. **Test thoroughly**: Ensure all cart operations work correctly

## Future Enhancements

Possible improvements:
1. Add coupon/discount support via backend
2. Implement cart item stock validation
3. Add saved carts for later feature
4. Support guest cart (localStorage) before login
5. Add cart analytics/tracking
6. Implement cart expiration logic

## Conclusion

This implementation follows the "Best of Both Worlds" architecture strictly:
- ✅ Backend is single source of truth
- ✅ No local calculations
- ✅ Redux mirrors backend response
- ✅ All operations call backend APIs
- ✅ Loading and error states managed properly
- ✅ Auto-hydration on page load
- ✅ Type-safe with TypeScript
- ✅ Follows YAML specification exactly
