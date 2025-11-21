package com.openshop.cart.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Builder
@Table(name = "carts")
public class Cart {

    @Builder
    public Cart(Long id, Long userId, List<CartItem> items) {
        this.id = id;
        this.userId = userId;
        this.items = items != null ? items : new ArrayList<>();
    }


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    @JsonManagedReference
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CartItem> items = new ArrayList<>(); // ✅ Initialize here

    public void addItem(CartItem item) {
        items.add(item);
        item.setCart(this);
    }

    public void clearItems() {
        items.forEach(i -> i.setCart(null));
        items.clear();
    }
}
