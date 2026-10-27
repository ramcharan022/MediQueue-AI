package com.queueless.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByRole(UserRole role);
    List<User> findByRole(UserRole role);

    @Query(value = "SELECT id FROM admin_bootstrap_lock WHERE id = 1 FOR UPDATE", nativeQuery = true)
    Long lockAdminBootstrap();
}
