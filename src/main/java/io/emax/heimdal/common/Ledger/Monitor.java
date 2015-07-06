package io.emax.heimdal.common.Ledger;


import io.emax.heimdal.common.Balance;
import io.emax.heimdal.common.Transfer;

import java.sql.SQLException;
import java.util.List;

public interface Monitor {

    /**
     * Get the currency the monitor is monitoring
     * @return A string representing the monitor's currency
     */
    String getCurrency();

    /**
     * Set accounts to be monitored
     * @param accounts A list of accounts to monitor
     * @return A list containing the balances of monitored accounts
     */
    List<Balance> monitor(List<String> accounts) throws Exception;

    /**
     * Get balances for all monitored accounts
     * @return A list of balances
     */
    List<Balance> getBalances() throws Exception;

    /**
     * Get balances for specified accounts
     * @return A list of balances
     * @param accounts The list of accounts to be queried
     */
    List<Balance> getBalances(List<String> accounts) throws Exception;
}
