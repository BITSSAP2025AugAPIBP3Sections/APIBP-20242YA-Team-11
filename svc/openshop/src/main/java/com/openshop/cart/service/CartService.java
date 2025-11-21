package com.openshop.cart.service;

import com.openshop.cart.exception.UnauthorizedException;
import com.openshop.cart.model.Cart;
import com.openshop.cart.model.CartItem;
import com.openshop.cart.repository.CartRepository;
import com.openshop.product.service.ProductService;
import com.openshop.product.model.Product;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    private final ProductService productService;
    private final CartRepository cartRepository;
    private final CartService self;

    // Constructor with self-injection to enable proper transactional proxy calls
    public CartService(ProductService productService, CartRepository cartRepository, @Lazy CartService self) {
        this.productService = productService;
        this.cartRepository = cartRepository;
        this.self = self;
    }

    @Transactional
    public Cart getCartByUserId(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> cartRepository.save(Cart.builder().userId(userId).build()));
    }

    @Transactional
    public Cart addItem(Long userId, CartItem item) {
        // Fetch product details from ProductService directly (no HTTP call)
        Product product = productService.getProductById(item.getProductId());
        
        if (product == null || !product.isActive()) {
            throw new IllegalArgumentException("Product not available or does not exist");
        }

        // Set price from product
        item.setPrice(product.getPrice());

        // Use self-injection to call transactional method through proxy
        Cart cart = self.getCartByUserId(userId);
        cart.addItem(item);
        return cartRepository.save(cart);
    }

    @Transactional
    public Cart removeItem(Long userId, Long itemId) {
        // Use self-injection to call transactional method through proxy
        Cart cart = self.getCartByUserId(userId);
        
        // Verify that the item belongs to this user's cart
        boolean itemExists = cart.getItems().stream()
                .anyMatch(i -> i.getId().equals(itemId));
        
        if (!itemExists) {
            throw new UnauthorizedException("Cart item does not belong to your cart or does not exist");
        }
        
        cart.getItems().removeIf(i -> i.getId().equals(itemId));
        return cartRepository.save(cart);
    }

    @Transactional
    public void clearCart(Long userId) {
        // Use self-injection to call transactional method through proxy
        Cart cart = self.getCartByUserId(userId);
        cart.clearItems();
        cartRepository.save(cart);
    }
}
