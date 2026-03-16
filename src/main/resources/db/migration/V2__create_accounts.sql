CREATE TABLE accounts (
  id UUID PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id),
  account_number VARCHAR(32) NOT NULL UNIQUE,
  balance NUMERIC(19,2) NOT NULL,
  status VARCHAR(20) NOT NULL,
  created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_accounts_user_id ON accounts(user_id);
