package cl.duoc.foro.users.service;

import java.util.Locale;
import cl.duoc.foro.common.JwtTokens;
import cl.duoc.foro.users.dto.UserDtos.*;
import cl.duoc.foro.users.entity.User;
import cl.duoc.foro.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository repository;
    private final JwtEncoder jwtEncoder;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.context.annotation.Configuration
    public static class PasswordConfiguration {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    public UserResponse register(RegisterRequest request) {
        String email = normalize(request.email());
        if (repository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }
        User user = new User();
        user.setDisplayName(request.displayName().strip());
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(User.Role.USER);
        return UserResponse.from(repository.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = repository.findByEmail(normalize(request.email())).orElse(null);
        String hash = user == null ? "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy" : user.getPasswordHash();
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (user == null || !matches) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }
        return new TokenResponse(JwtTokens.issue(jwtEncoder, user.getId(), user.getRole().name()),
                "Bearer", JwtTokens.LIFETIME_SECONDS, UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return repository.findAll(pageable).map(UserResponse::from);
    }

    @Transactional(readOnly = true)
    public UserResponse get(Long id, Jwt jwt) {
        checkAccess(id, jwt);
        return UserResponse.from(find(id));
    }

    public UserResponse update(Long id, UpdateRequest request, Jwt jwt) {
        checkAccess(id, jwt);
        User user = find(id);
        String email = normalize(request.email());
        if (!user.getEmail().equals(email) && repository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El correo ya esta registrado");
        }
        user.setDisplayName(request.displayName().strip());
        user.setEmail(email);
        if (request.password() != null) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        return UserResponse.from(repository.saveAndFlush(user));
    }

    public UserResponse changeRole(Long id, RoleRequest request) {
        User user = find(id);
        user.setRole(request.role());
        return UserResponse.from(repository.save(user));
    }

    public void delete(Long id, Jwt jwt) {
        checkAccess(id, jwt);
        repository.delete(find(id));
    }

    private User find(Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private void checkAccess(Long id, Jwt jwt) {
        if (!JwtTokens.isAdmin(jwt) && !id.equals(JwtTokens.userId(jwt))) {
            throw new AccessDeniedException("Solo el propietario o un administrador");
        }
    }

    private String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}