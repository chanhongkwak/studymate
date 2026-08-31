package com.studymate.member.domain;

import java.util.Optional;
import java.util.UUID;


public interface MemberRepository {
    Member save(Member member);

    Optional<Member> findById(UUID id);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByIdAndDeletedAtIsNull(UUID id);

    boolean existsByEmail(String email);
}
