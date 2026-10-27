package cl.duoc.foro.users.controller;

import java.net.URI;
import cl.duoc.foro.users.dto.UserDtos.*;
import cl.duoc.foro.users.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService service;

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = service.register(request);
        return ResponseEntity.created(URI.create("/api/users/" + user.id())).body(user);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }
}