CREATE ROLE supportops_reader LOGIN PASSWORD 'local-reader-password' NOSUPERUSER NOCREATEDB NOCREATEROLE;
ALTER ROLE supportops_reader SET default_transaction_read_only=on;
ALTER ROLE supportops_reader SET statement_timeout='3s';
