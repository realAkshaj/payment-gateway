-- Each service owns its own database
-- This enforces the microservice principle of data isolation
-- Services cannot query each other's databases directly

CREATE DATABASE card_vault_db;
CREATE DATABASE ledger_db;
CREATE DATABASE webhook_db;
CREATE DATABASE settlement_db;

-- payments_db already created by POSTGRES_DB env var in docker-compose