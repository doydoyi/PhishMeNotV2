package edu.cit.alvarado.phishmenot.user;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByGoogleId(String googleId);

    Optional<User> findByEmailIgnoreCase(String email);

    long countByRole(Role role);

    long countByCreatedAtAfter(LocalDateTime since);

    List<User> findTop5ByOrderByCreatedAtDesc();

    Page<User> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCase(String name, String email, Pageable pageable);
}
