-- Split generic UTILITY into explicit categories (run once on Postgres).
-- After this, deploy the backend with updated ExpenseCategory enum.

-- If a value already exists, skip that line (Postgres has no IF NOT EXISTS before v15).
ALTER TYPE expense_category ADD VALUE 'ELECTRICITY';
ALTER TYPE expense_category ADD VALUE 'WATER';
ALTER TYPE expense_category ADD VALUE 'GAS';

-- Optional: remap old UTILITY rows (pick a default or edit WHERE per row).
-- UPDATE expenses SET category = 'ELECTRICITY'::expense_category WHERE category = 'UTILITY'::expense_category;
