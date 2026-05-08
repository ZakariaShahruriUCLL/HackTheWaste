package be.leuven.leuvengo.repository;

import be.leuven.leuvengo.domain.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    Optional<AppUser> findBySessionToken(String sessionToken);
    boolean existsByEmail(String email);
}
