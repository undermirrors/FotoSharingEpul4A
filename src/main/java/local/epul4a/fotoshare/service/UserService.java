package local.epul4a.fotoshare.service;

import local.epul4a.fotoshare.dto.UserRegistrationDTO;
import local.epul4a.fotoshare.entity.User;
import local.epul4a.fotoshare.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void registerUser(UserRegistrationDTO dto) {
        if (existsByEmail(dto.getEmail())) {
            throw new RuntimeException("Un user avec cet email existe déjà");
        }
        if (existsByUsername(dto.getUsername())) {
            throw new RuntimeException("Ce nom d'user est déjà utilisé");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new RuntimeException("Les mots de passe ne correspondent pas");
        }

        User user = new User();
        user.setUsername(dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(User.Role.USER);
        user.setEnabled(true);

        userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email).orElse(null);
    }

    @Transactional(readOnly = true)
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Transactional(readOnly = true)
    public User findById(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional(readOnly = true)
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public long countAllUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    public void changeUserRole(Long userId, User.Role role) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User introuvable"));

        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Impossible de modifier le rôle d'un administrateur");
        }

        if (role == User.Role.ADMIN) {
            throw new RuntimeException("Le rôle ADMIN ne peut être attribué que via la base de données");
        }

        user.setRole(role);
        userRepository.save(user);
    }

    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User introuvable"));

        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Impossible de supprimer un administrateur");
        }

        userRepository.deleteById(userId);
    }

    public void toggleUserEnabled(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User introuvable"));

        if (user.getRole() == User.Role.ADMIN) {
            throw new RuntimeException("Impossible de désactiver un administrateur");
        }

        user.setEnabled(!user.getEnabled());
        userRepository.save(user);
    }
}

