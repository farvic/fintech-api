CREATE TABLE transactions (
  id UUID PRIMARY KEY,
  from_account_id UUID NOT NULL REFERENCES accounts(id),
  to_account_id UUID NOT NULL REFERENCES accounts(id),
  amount NUMERIC(19,2) NOT NULL CHECK (amount > 0),
  type VARCHAR(20) NOT NULL,
  status VARCHAR(20) NOT NULL,
  description VARCHAR(255),
  created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_transactions_from_account_id ON transactions(from_account_id);
CREATE INDEX idx_transactions_to_account_id ON transactions(to_account_id);
CREATE INDEX idx_transactions_created_at ON transactions(created_at);
