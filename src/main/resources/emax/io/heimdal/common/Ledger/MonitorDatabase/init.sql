CREATE TABLE IF NOT EXISTS BalanceConfirmations(
   blockNumber int primary key not null,
   timestamp int not null,
   blockHash varchar(64),
   CONSTRAINT blockNumber_UNIQUE UNIQUE (blockNumber),
   CONSTRAINT timestamp_UNIQUE UNIQUE (timestamp));

CREATE TABLE IF NOT EXISTS Accounts(
   account varchar(35) primary key not null,
   amount varchar(64),
   firstConfirmation int,
   lastConfirmation int,
   FOREIGN KEY (firstConfirmation) REFERENCES public.BalanceConfirmations(blockNumber),
   FOREIGN KEY (lastConfirmation) REFERENCES public.BalanceConfirmations(blockNumber),
   CONSTRAINT account_UNIQUE UNIQUE (account)
);

-- TODO: Create triggers for inserting into Balances view

CREATE VIEW IF NOT EXISTS Balances AS SELECT
   Accounts.account as account,
   Accounts.amount as amount,
   firstConfirmation.blockNumber as firstConfirmationBlockNumber,
   firstConfirmation.timestamp as firstConfirmationTimestamp,
   firstConfirmation.blockHash as firstConfirmationBlockHash,
   lastConfirmation.blockNumber as lastConfirmationBlockNumber,
   lastConfirmation.timestamp as lastConfirmationTimestamp,
   lastConfirmation.blockHash as lastConfirmationBlockHash
FROM Accounts
   LEFT OUTER JOIN BalanceConfirmations firstConfirmation
   ON Accounts.firstConfirmation = firstConfirmation.blockNumber
   LEFT OUTER JOIN BalanceConfirmations lastConfirmation
   ON Accounts.lastConfirmation = lastConfirmation.blockNumber