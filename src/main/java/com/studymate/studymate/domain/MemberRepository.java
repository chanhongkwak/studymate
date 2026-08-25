package com.studymate.studymate.domain;

import java.util.Optional;
import java.util.UUID;


public interface MemberRepository {
    Member save(Member member);

    Optional<Member> findById(UUID id);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByIdAndDeleteAtIsNull(UUID id);

    boolean existsByEmail(String email);
}
