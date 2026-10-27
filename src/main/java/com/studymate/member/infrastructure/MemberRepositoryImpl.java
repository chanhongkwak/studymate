package com.studymate.member.infrastructure;

import com.studymate.member.domain.Member;
import com.studymate.member.domain.MemberRepository;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class MemberRepositoryImpl implements MemberRepository {
    private final JpaMemberRepository jpaMemberRepository;
    private final EntityManager entityManager;


    @Override
    public Member save(Member member) {
        return jpaMemberRepository.save(member);
    }

    @Override
    public Optional<Member> findById(UUID id) {
        return jpaMemberRepository.findById(id);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return jpaMemberRepository.findByEmail(email);
    }

    @Override
    public Optional<Member> findByIdAndDeletedAtIsNull(UUID id) {
        return jpaMemberRepository.findByIdAndDeletedAtIsNull(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaMemberRepository.existsByEmail(email);
    }

    @Override
    public Optional<Member> findByIdForUpdate(UUID memberId) {
        Optional<Member> result =
                jpaMemberRepository.findByIdForUpdate(memberId);

        result.ifPresent(member -> entityManager.refresh(member));

        return result;
    }}
