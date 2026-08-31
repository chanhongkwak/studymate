package com.studymate.member.infrastructure;

import com.studymate.member.domain.Member;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JpaMemberRepository extends JpaRepository<Member, UUID> {
    Optional<Member> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<Member> findByIdAndDeletedAtIsNull(UUID id);

}
