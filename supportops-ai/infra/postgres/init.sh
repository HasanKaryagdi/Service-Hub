#!/bin/sh
set -eu
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=app_password="$APP_PASSWORD" --set=reader_password="$READER_PASSWORD" <<'SQL'
CREATE ROLE supportops LOGIN PASSWORD :'app_password' NOSUPERUSER NOCREATEDB NOCREATEROLE;
CREATE ROLE supportops_reader LOGIN PASSWORD :'reader_password' NOSUPERUSER NOCREATEDB NOCREATEROLE;
REVOKE ALL ON DATABASE supportops FROM PUBLIC;
GRANT CONNECT ON DATABASE supportops TO supportops,supportops_reader;
REVOKE ALL ON SCHEMA public FROM PUBLIC;
GRANT USAGE,CREATE ON SCHEMA public TO supportops;
GRANT CREATE ON DATABASE supportops TO supportops;
ALTER ROLE supportops_reader SET default_transaction_read_only=on;
ALTER ROLE supportops_reader SET statement_timeout='3s';
ALTER ROLE supportops_reader SET lock_timeout='1s';
ALTER ROLE supportops_reader SET search_path=analytics;
SQL
