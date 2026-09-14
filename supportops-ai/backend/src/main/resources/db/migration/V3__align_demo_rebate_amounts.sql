-- Existing synthetic seed transactions must agree with their rebate record.
UPDATE transactions t
SET amount=r.amount
FROM rebates r
WHERE t.transaction_id=r.rebate_transaction_id
  AND t.transaction_id ~ '^tx-[0-9]{3}$'
  AND t.reconciliation_id ~ '^rec-[0-9]{4}$'
  AND t.amount<>r.amount;
