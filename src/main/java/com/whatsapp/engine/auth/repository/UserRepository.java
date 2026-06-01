package com.whatsapp.engine.auth.repository;

import com.whatsapp.engine.auth.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "organization")
    Optional<User> findByEmailIgnoreCase(String email);
}
