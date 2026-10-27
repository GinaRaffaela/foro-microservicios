package cl.duoc.foro.users.config;

import cl.duoc.foro.users.entity.User;
import cl.duoc.foro.users.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("demo")
public class UsersDemoData implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(UsersDemoData.class);
    private final UserRepository repository;
    private final PasswordEncoder passwords;
    private final String demoPassword;

    public UsersDemoData(UserRepository repository, PasswordEncoder passwords,
                         @Value("${DEMO_PASSWORD}") String demoPassword) {
        this.repository = repository;
        this.passwords = passwords;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!demoPassword.matches("[\\x21-\\x7E]{8,72}")) {
            throw new IllegalArgumentException("DEMO_PASSWORD requiere de 8 a 72 caracteres ASCII sin espacios");
        }
        if (repository.count() != 0) {
            log.info("Datos demo omitidos: la tabla de usuarios ya contiene registros");
            return;
        }
        create("Administrador", "admin@foro.test", User.Role.ADMIN);
        create("Ana", "ana@foro.test", User.Role.USER);
        create("Luis", "luis@foro.test", User.Role.USER);
        log.info("Creados tres usuarios de demostracion");
    }

    private void create(String name, String email, User.Role role) {
        User user = new User();
        user.setDisplayName(name);
        user.setEmail(email);
        user.setPasswordHash(passwords.encode(demoPassword));
        user.setRole(role);
        repository.save(user);
    }
}