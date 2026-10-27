package cl.duoc.foro.users.dto;

import cl.duoc.foro.users.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 80) String displayName,
            @NotBlank @Email @Size(max = 254) String email,
            @NotBlank @Pattern(regexp = "[\\x21-\\x7E]{8,72}",
                    message = "Use de 8 a 72 caracteres ASCII sin espacios") String password) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 80) String displayName,
            @NotBlank @Email @Size(max = 254) String email,
            @Pattern(regexp = "[\\x21-\\x7E]{8,72}",
                    message = "Use de 8 a 72 caracteres ASCII sin espacios") String password) {}

    public record RoleRequest(@NotNull User.Role role) {}
    public record LoginRequest(@NotBlank @Email @Size(max = 254) String email,
                               @NotBlank @Size(max = 72) String password) {}
    public record UserResponse(Long id, String displayName, String email, User.Role role) {
        public static UserResponse from(User user) {
            return new UserResponse(user.getId(), user.getDisplayName(), user.getEmail(), user.getRole());
        }
    }
    public record TokenResponse(String accessToken, String tokenType, long expiresIn, UserResponse user) {}
}