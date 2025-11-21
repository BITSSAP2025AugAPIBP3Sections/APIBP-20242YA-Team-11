/**
 * Cart API - Backend API wrapper functions
 * Following the YAML specification exactly
 */

import API, { type Cart, type AddToCartRequest } from '@/api/api';

/**
 * Get user's cart from backend
 * Endpoint: GET /api/cart
 */
export const getCartApi = async (): Promise<Cart> => {
  return await API.cart.getCart();
};

/**
 * Add item to cart
 * Endpoint: POST /api/cart/items
 * @param productId - UUID of the product
 * @param quantity - Quantity to add
 */
export const addToCartApi = async (
  productId: string,
  quantity: number
): Promise<Cart> => {
  const item: AddToCartRequest = {
    productId,
    quantity,
  };
  return await API.cart.addItem(item);
};

/**
 * Remove item from cart
 * Endpoint: DELETE /api/cart/remove/{itemId}
 * @param itemId - ID of the cart item to remove
 */
export const removeFromCartApi = async (itemId: number): Promise<Cart> => {
  return await API.cart.removeItem(itemId);
};

/**
 * Update item quantity in cart
 * NOTE: The YAML doesn't have a dedicated update endpoint.
 * We need to remove and re-add the item with new quantity.
 * This follows the backend contract strictly.
 * @param itemId - ID of the cart item
 * @param productId - UUID of the product
 * @param newQuantity - New quantity
 */
export const updateQuantityApi = async (
  itemId: number,
  productId: string,
  newQuantity: number
): Promise<Cart> => {
  // First remove the item
  await API.cart.removeItem(itemId);
  // Then add it back with new quantity
  return await addToCartApi(productId, newQuantity);
};

/**
 * Clear entire cart
 * Endpoint: DELETE /api/cart/clear
 */
export const clearCartApi = async (): Promise<void> => {
  await API.cart.clearCart();
};

/**
 * Checkout cart and create order
 * Endpoint: POST /api/cart/checkout
 */
export const checkoutCartApi = async () => {
  return await API.cart.checkout();
};
