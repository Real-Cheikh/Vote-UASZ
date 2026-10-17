package sn.uasz.voteuasz.voting;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface VoteCredentialRepository extends JpaRepository<VoteCredential, Long> {

    Optional<VoteCredential> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE VoteCredential v SET v.utilise = true, v.dateUtilisation = :now WHERE v.tokenHash = :tokenHash AND v.utilise = false AND v.dateExpiration > :now")
    int consumeTokenAtomically(@Param("tokenHash") String tokenHash, @Param("now") OffsetDateTime now);
}
