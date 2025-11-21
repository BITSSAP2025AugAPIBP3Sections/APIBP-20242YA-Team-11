package com.openshop.user.service;



import com.openshop.user.model.Role;
import com.openshop.user.model.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User createUser(User user);
    List<User> getAllUsers();
    User getUserById(Long id);
    User updateUser(Long id, User user);
    void deleteUser(Long id);
    List<User> getUsersByRole(Role role);
    Optional<User> findByUsername(String username);
    boolean existsByEmail(String email);
}
