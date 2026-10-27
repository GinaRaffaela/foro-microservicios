package cl.duoc.foro.users.controller;

import cl.duoc.foro.common.JwtTokens;
import cl.duoc.foro.users.dto.UserDtos.*;
import cl.duoc.foro.users.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {
    private final UserService service;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Page<UserResponse> list(@RequestParam(defaultValue = "0") @Min(0) int page,
                                  @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.list(PageRequest.of(page, size, Sort.by("id")));
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return service.get(JwtTokens.userId(jwt), jwt);
    }

    @GetMapping("/{id}")
    public UserResponse get(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.get(id, jwt);
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable Long id, @Valid @RequestBody UpdateRequest request,
                               @AuthenticationPrincipal Jwt jwt) {
        return service.update(id, request, jwt);
    }

    @PutMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse role(@PathVariable Long id, @Valid @RequestBody RoleRequest request) {
        return service.changeRole(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        service.delete(id, jwt);
        return ResponseEntity.noContent().build();
    }
}