-- Run before deploying the meeting response policy, with application writers stopped.
BEGIN;
ALTER TABLE meeting ADD COLUMN IF NOT EXISTS response_deadline_at timestamp(6),
                    ADD COLUMN IF NOT EXISTS response_closed_at timestamp(6),
                    ADD COLUMN IF NOT EXISTS majority_approved boolean,
                    ADD COLUMN IF NOT EXISTS cancellation_reason varchar(64);
CREATE TABLE IF NOT EXISTS meeting_participant (
    id uuid PRIMARY KEY, meeting_id uuid NOT NULL, member_id uuid NOT NULL,
    study_member_id uuid NOT NULL, created_at timestamp(6) NOT NULL, created_by uuid NOT NULL,
    CONSTRAINT uk_meeting_participant_meeting_member UNIQUE (meeting_id, member_id)
);
ALTER TABLE meeting_participant ADD COLUMN IF NOT EXISTS excluded_at_closure boolean NOT NULL DEFAULT false;
-- Pre-policy proposals cannot be closed accurately without their original roster.
UPDATE meeting SET status = 'CANCELED', cancellation_reason = 'LEGACY_PROPOSAL',
    updated_at = CURRENT_TIMESTAMP AT TIME ZONE 'Asia/Seoul',
    updated_by = '00000000-0000-0000-0000-000000000000'
WHERE status = 'PROPOSED' AND response_deadline_at IS NULL;
DELETE FROM meeting_rsvp r USING meeting m
WHERE r.meeting_id = m.id AND m.cancellation_reason = 'LEGACY_PROPOSAL';
COMMIT;
