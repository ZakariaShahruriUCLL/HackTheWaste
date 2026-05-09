package be.leuven.leuvengo.web;

import be.leuven.leuvengo.domain.AppUser;
import be.leuven.leuvengo.repository.AppUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private final AppUserRepository users;

    public AuthController(AppUserRepository users) {
        this.users = users;
    }

    public record RegisterRequest(String email, String password, String facultyShortCode, boolean consentGiven) {}
    public record LoginRequest(String email, String password) {}
    public record AuthResponse(Long id, String email, String facultyShortCode, String token) {}

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest req) {
        if (req.email() == null || req.email().isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        if (req.password() == null || req.password().length() < 6)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 6 characters");
        if (!req.consentGiven())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You must accept the data consent to create an account");
        if (users.existsByEmail(req.email().toLowerCase().trim()))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");

        Instant now = Instant.now();
        AppUser user = new AppUser();
        user.setEmail(req.email().toLowerCase().trim());
        user.setPasswordHash(BCRYPT.encode(req.password()));
        user.setFacultyShortCode(req.facultyShortCode());
        user.setSessionToken(UUID.randomUUID().toString());
        user.setConsentGiven(true);
        user.setConsentGivenAt(now);
        user.setCreatedAt(now);
        users.save(user);

        return toResponse(user);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest req) {
        AppUser user = users.findByEmail(req.email().toLowerCase().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!BCRYPT.matches(req.password(), user.getPasswordHash()))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");

        user.setSessionToken(UUID.randomUUID().toString());
        users.save(user);
        return toResponse(user);
    }

    @GetMapping("/me")
    public AuthResponse me(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        return toResponse(resolve(authHeader));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        AppUser user = resolve(authHeader);
        user.setSessionToken(null);
        users.save(user);
        return ResponseEntity.noContent().build();
    }

    private AppUser resolve(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer "))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing token");
        return users.findBySessionToken(authHeader.substring(7))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token"));
    }

    private static AuthResponse toResponse(AppUser u) {
        return new AuthResponse(u.getId(), u.getEmail(), u.getFacultyShortCode(), u.getSessionToken());
    }
}
