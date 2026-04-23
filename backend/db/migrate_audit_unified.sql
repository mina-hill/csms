-- Unified audit_log: entity_type, flock_id, migrate flock_audit_log → audit_log
-- Run the FULL file in order. If you only pasted the INSERT, run STEP A below first.
--
-- STEP A (run these two blocks first if you see "column entity_type does not exist"):
--   ALTER TABLE public.audit_log ADD COLUMN entity_type VARCHAR(50);
--   ALTER TABLE public.audit_log ADD COLUMN flock_id UUID;
--   (Ignore "already exists" if you re-run.)

-- 1) Add columns — always attempts ADD; ignores duplicate_column (idempotent)
DO $$ BEGIN
  ALTER TABLE public.audit_log ADD COLUMN entity_type VARCHAR(50);
EXCEPTION
  WHEN duplicate_column THEN NULL;
END $$;

DO $$ BEGIN
  ALTER TABLE public.audit_log ADD COLUMN flock_id UUID;
EXCEPTION
  WHEN duplicate_column THEN NULL;
END $$;

UPDATE public.audit_log
SET entity_type = table_name
WHERE entity_type IS NULL AND table_name IS NOT NULL;

UPDATE public.audit_log
SET flock_id = (NULLIF(trim(both '"' FROM details::jsonb->>'flock_id'), ''))::uuid
WHERE flock_id IS NULL
  AND details IS NOT NULL
  AND details::jsonb ? 'flock_id';

CREATE INDEX IF NOT EXISTS idx_audit_log_flock_logged
  ON public.audit_log (flock_id, logged_at DESC)
  WHERE flock_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_audit_log_entity_logged
  ON public.audit_log (entity_type, logged_at DESC)
  WHERE entity_type IS NOT NULL;

INSERT INTO public.audit_log (
  log_id,
  user_id,
  action,
  table_name,
  entity_type,
  record_id,
  flock_id,
  details,
  logged_at
)
SELECT
  gen_random_uuid(),
  fal.changed_by,
  CASE
    WHEN fal.old_values IS NULL OR fal.old_values::text = 'null' OR fal.old_values::text = '' THEN 'FLOCK_CREATED'
    WHEN fal.new_values::text LIKE '%"status":"CLOSED"%' THEN 'FLOCK_CLOSED'
    ELSE 'FLOCK_UPDATED'
  END,
  'flocks',
  'flocks',
  fal.flock_id,
  fal.flock_id,
  jsonb_build_object(
    'old_values', COALESCE(fal.old_values::jsonb, 'null'::jsonb),
    'new_values', COALESCE(fal.new_values::jsonb, 'null'::jsonb)
  ),
  COALESCE(fal.changed_at, now())
FROM public.flock_audit_log fal;

-- After verifying: -- DROP TABLE IF EXISTS public.flock_audit_log;
