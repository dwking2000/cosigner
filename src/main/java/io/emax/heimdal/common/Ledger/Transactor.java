package io.emax.heimdal.common.Ledger;


import io.emax.heimdal.common.Balance;
import io.emax.heimdal.common.Transfer;

import java.util.List;

public interface Transactor {
    /**
     * Get the currency the transactor is transacting
     * @return A string representing the monitor's currency
     */
    String getCurrency();

    /**
     * Get balance for the transactor
     * @return A list of balances
     */
    Balance getBalance() throws Exception;

    /**
     * Transfer money from accounts to other accounts
     * @param transfers A list of transfers to be made
     * @return A list of the new balances
     */
    List<Balance> transfer(List<Transfer> transfers);

    /**
     * Create a new account
     * @return The new account's balance
     */
    Balance newAccount();

    /**
     * Close an account and transfer the money to a different account
     * @param account the account to close
     * @param destinationAccountForRemainingBalance the destination for the remaining balance in the account to go
     * @return The new balance of the destination for the remainder of the closed account's balance
     */
    Balance closeAccount(String account, String destinationAccountForRemainingBalance);
}
