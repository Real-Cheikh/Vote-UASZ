package sn.uasz.voteuasz.users;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.uasz.voteuasz.common.BusinessException;
import sn.uasz.voteuasz.common.ResourceNotFoundException;
import sn.uasz.voteuasz.roles.Role;
import sn.uasz.voteuasz.roles.RoleName;
import sn.uasz.voteuasz.roles.RoleRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<UserDto> findAll() {
        return userRepository.findAll().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> findAllPaged(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public UserDto findById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé avec l'id : " + id));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto createUser(UserRegistrationDto dto) {
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new BusinessException("Un utilisateur existe déjà avec cet email : " + dto.getEmail());
        }
        if (userRepository.existsByNumeroIdentification(dto.getNumeroIdentification())) {
            throw new BusinessException("Ce numéro d'identification est déjà attribué : " + dto.getNumeroIdentification());
        }

        User user = new User();
        user.setEmail(dto.getEmail().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setNom(dto.getNom().trim());
        user.setPrenom(dto.getPrenom().trim());
        user.setNumeroIdentification(dto.getNumeroIdentification().trim());
        user.setTelephone(dto.getTelephone());
        user.setCategorie(dto.getCategorie());
        user.setActif(true);
        user.setBloque(false);

        Set<Role> roles = new HashSet<>();
        if (dto.getRoles() != null && !dto.getRoles().isEmpty()) {
            for (String roleNameStr : dto.getRoles()) {
                RoleName roleName = RoleName.valueOf(roleNameStr);
                Role role = roleRepository.findByName(roleName)
                        .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + roleNameStr));
                roles.add(role);
            }
        } else {
            // Rôle par défaut : Électeur
            Role defaultRole = roleRepository.findByName(RoleName.ROLE_ELECTEUR)
                    .orElseGet(() -> roleRepository.save(new Role(RoleName.ROLE_ELECTEUR, "Électeur universitaire")));
            roles.add(defaultRole);
        }
        user.setRoles(roles);

        User saved = userRepository.save(user);
        return UserDto.fromEntity(saved);
    }

    @Transactional
    public UserDto updateUserRoles(Long userId, Set<String> roleNames) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        Set<Role> updatedRoles = new HashSet<>();
        for (String roleStr : roleNames) {
            RoleName roleName = RoleName.valueOf(roleStr);
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Rôle introuvable : " + roleStr));
            updatedRoles.add(role);
        }

        user.setRoles(updatedRoles);
        return UserDto.fromEntity(userRepository.save(user));
    }

    @Transactional
    public UserDto toggleUserStatus(Long userId, boolean actif, boolean bloque) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

        user.setActif(actif);
        user.setBloque(bloque);
        return UserDto.fromEntity(userRepository.save(user));
    }
}
