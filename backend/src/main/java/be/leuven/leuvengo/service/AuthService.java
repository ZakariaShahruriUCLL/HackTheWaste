package be.leuven.leuvengo.service;

import be.leuven.leuvengo.domain.AppUser;
import be.leuven.leuvengo.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final AppUserRepository users;

    public AuthService(AppUserRepository users) {
        this.users = users;
    }

    public AppUser requireAuth(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        return users.findBySessionToken(authHeader.substring(7))
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED, "Invalid or expired session"));
    }
}
