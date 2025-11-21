package com.openshop.user.repository;



import com.openshop.user.model.Role;
import com.openshop.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    
    Optional<User> findByEmail(String email);
    
    Optional<User> findByUsernameOrEmail(String username, String email);

    List<User> findByRole(Role role);

    boolean existsByUsername(String username);
    
    boolean existsByEmail(String email);
}
