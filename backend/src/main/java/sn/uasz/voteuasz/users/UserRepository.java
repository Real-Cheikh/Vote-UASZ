package sn.uasz.voteuasz.users;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByNumeroIdentification(String numeroIdentification);
    boolean existsByEmail(String email);
    boolean existsByNumeroIdentification(String numeroIdentification);
    List<User> findByCategorie(UserCategory categorie);
}
