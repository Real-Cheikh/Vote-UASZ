package sn.uasz.voteuasz.otp;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<OtpChallenge, Long> {

    @Query("SELECT o FROM OtpChallenge o WHERE o.user.id = :userId AND o.election.id = :electionId AND o.consomme = false ORDER BY o.dateCreation DESC LIMIT 1")
    Optional<OtpChallenge> findLatestActiveChallenge(@Param("userId") Long userId, @Param("electionId") Long electionId);
}
