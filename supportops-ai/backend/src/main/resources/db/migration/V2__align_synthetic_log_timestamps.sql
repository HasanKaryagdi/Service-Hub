-- Repair only the synthetic bootstrap request records from early MVP installs.
-- Live simulator / collected logs do not use this marker and are untouched.
UPDATE service_logs l
SET created_at = t.created_at + CASE WHEN l.service_name='wallet-service' THEN interval '500 milliseconds' ELSE interval '0 seconds' END,
    message = CASE WHEN l.service_name='wallet-service' THEN 'Wallet service request sent' WHEN t.transaction_type='REBATE' THEN 'Rebate request received' ELSE 'Payment request received' END,
    service_name = CASE WHEN l.service_name='wallet-service' THEN 'wallet-service' WHEN t.transaction_type='REBATE' THEN 'rebate-service' ELSE 'payment-service' END,
    http_status = NULL,
    response_body = NULL
FROM transactions t
WHERE l.transaction_id=t.transaction_id
  AND l.request_body='{"source":"synthetic"}'
  AND l.message IN ('Wallet service request accepted','Payment request received');
