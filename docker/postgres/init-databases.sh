#!/bin/sh
# Database-per-service on one Postgres server: each service gets its own database AND its own login,
# so no service can read another's tables. Runs once, on first container start (empty data volume).
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname postgres <<-EOSQL
    CREATE USER ingestion_user WITH PASSWORD '${INGESTION_DB_PASSWORD}';
    CREATE DATABASE ingestion_db OWNER ingestion_user;

    CREATE USER sentiment_user WITH PASSWORD '${SENTIMENT_DB_PASSWORD}';
    CREATE DATABASE sentiment_db OWNER sentiment_user;

    CREATE USER reconciliation_user WITH PASSWORD '${RECONCILIATION_DB_PASSWORD}';
    CREATE DATABASE reconciliation_db OWNER reconciliation_user;

    REVOKE CONNECT ON DATABASE ingestion_db FROM PUBLIC;
    REVOKE CONNECT ON DATABASE sentiment_db FROM PUBLIC;
    REVOKE CONNECT ON DATABASE reconciliation_db FROM PUBLIC;
EOSQL
