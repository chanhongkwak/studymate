-- Manual migration; not automatically executed by Spring Boot.
-- Allow all current StudyMemberStatus values while preserving existing rows.
BEGIN;
ALTER TABLE public.study_members
    DROP CONSTRAINT study_members_status_check;
ALTER TABLE public.study_members
    ADD CONSTRAINT study_members_status_check
    CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE', 'REJECTED', 'LEFT', 'COMPLETE', 'CANCELED'));
COMMIT;
