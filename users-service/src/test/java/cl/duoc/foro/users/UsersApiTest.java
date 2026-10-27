package cl.duoc.foro.users;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import cl.duoc.foro.common.JwtTokens;
import cl.duoc.foro.users.entity.User;
import cl.duoc.foro.users.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsersApiTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired UserRepository repository;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtEncoder encoder;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
        void demoCreatesThreeUsersWithDifferentRolesAndIsIdempotent() {
                var seed = new cl.duoc.foro.users.config.UsersDemoData(repository, passwords, "Test12345!");
                seed.run();
                seed.run();
                assertThat(repository.count()).isEqualTo(3);
                assertThat(repository.findByEmail("admin@foro.test").orElseThrow().getRole()).isEqualTo(User.Role.ADMIN);
                assertThat(repository.findByEmail("ana@foro.test").orElseThrow().getRole()).isEqualTo(User.Role.USER);
        }

        @Test
    void registrationLoginAndFullUserCrud() throws Exception {
        JsonNode user = register("ana@foro.test");
        long id = user.get("id").asLong();
        assertThat(user.has("passwordHash")).isFalse();
        assertThat(passwords.matches("Test12345!", repository.findById(id).orElseThrow().getPasswordHash())).isTrue();
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ANA@foro.test\",\"password\":\"Test12345!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).get("accessToken").asText();
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(put("/api/users/{id}", id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"Ana editada\",\"email\":\"ana2@foro.test\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Ana editada"));
        mvc.perform(delete("/api/users/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/{id}", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsDuplicatesBadInputsAndIncorrectCredentials() throws Exception {
        register("ana@foro.test");
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("ANA@foro.test"))).andExpect(status().isConflict());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"displayName\":\"\",\"email\":\"invalid\",\"password\":\"123\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.email").exists());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana@foro.test\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"nadie@foro.test\",\"password\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void enforcesRolesOwnershipAndDoesNotAllowPublicAdminRegistration() throws Exception {
        long id = register("ana@foro.test").get("id").asLong();
        String userToken = "Bearer " + JwtTokens.issue(encoder, id, "USER");
        mvc.perform(get("/api/users")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users").header("Authorization", userToken)).andExpect(status().isForbidden());
        mvc.perform(get("/api/users/{id}", id + 1).header("Authorization", userToken))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/users/{id}/role", id).header("Authorization", userToken)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload("evil@foro.test").replace("}", ",\"role\":\"ADMIN\"}")))
                .andExpect(status().isBadRequest());
        String admin = "Bearer " + JwtTokens.issue(encoder, 999L, "ADMIN");
        mvc.perform(get("/api/users").header("Authorization", admin)).andExpect(status().isOk());
        mvc.perform(put("/api/users/{id}/role", id).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"ADMIN\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        assertThat(repository.findById(id).orElseThrow().getRole()).isEqualTo(User.Role.ADMIN);
    }

    private JsonNode register(String email) throws Exception {
        String response = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(payload(email))).andExpect(status().isCreated())
                .andExpect(header().exists("Location")).andExpect(jsonPath("$.role").value("USER"))
                .andReturn().getResponse().getContentAsString();
        return mapper.readTree(response);
    }

    private String payload(String email) {
        return "{\"displayName\":\"Ana\",\"email\":\"" + email + "\",\"password\":\"Test12345!\"}";
    }
}