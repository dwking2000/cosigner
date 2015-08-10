package io.emax.heimdal.bitcoin.bitcoind;

import java.math.BigDecimal;
import java.util.Map;

import com.googlecode.jsonrpc4j.JsonRpcError;
import com.googlecode.jsonrpc4j.JsonRpcErrors;
import com.googlecode.jsonrpc4j.JsonRpcMethod;

/**
 * Wallet RPCs
 * 
 * Note: the wallet RPCs are only available if Bitcoin Core was built with wallet support, which is the default.
 * 
 * AddMultiSigAddress: adds a P2SH multisig address to the wallet.
 * BackupWallet: safely copies wallet.dat to the specified file, which can be a directory or a path with filename.
 * DumpPrivKey: returns the wallet-import-format (WIP) private key corresponding to an address. (But does not remove it from the wallet.)
 * PENDING: DumpWallet: creates or overwrites a file with all wallet keys in a human-readable format.
 * PENDING: EncryptWallet: encrypts the wallet with a passphrase. This is only to enable encryption for the first time. After encryption is enabled, you will need to enter the passphrase to use private keys.
 * GetAccountAddress: returns the current Bitcoin address for receiving payments to this account. If the account doesnâ€™t exist, it creates both the account and a new address for receiving payment. Once a payment has been received to an address, future calls to this RPC for the same account will return a different address.
 * GetAccount: returns the name of the account associated with the given address.
 * GetAddressesByAccount: returns a list of every address assigned to a particular account.
 * PENDING: GetBalance: gets the balance in decimal bitcoins across all accounts or for a particular account.
 * GetNewAddress: returns a new Bitcoin address for receiving payments. If an account is specified, payments received with the address will be credited to that account.
 * PENDING: GetRawChangeAddress: returns a new Bitcoin address for receiving change. This is for use with raw transactions, not normal use.
 * PENDING: GetReceivedByAccount: returns the total amount received by addresses in a particular account from transactions with the specified number of confirmations. It does not count coinbase transactions.
 * PENDING: GetReceivedByAddress: returns the total amount received by the specified address in transactions with the specified number of confirmations. It does not count coinbase transactions.
 * PENDING: GetTransaction: gets detailed information about an in-wallet transaction. Updated in 0.10.0
 * PENDING: GetUnconfirmedBalance: returns the walletâ€™s total unconfirmed balance.
 * PENDING: GetWalletInfo: provides information about the wallet. New in 0.9.2
 * ImportAddress: adds an address or pubkey script to the wallet without the associated private key, allowing you to watch for transactions affecting that address or pubkey script without being able to spend any of its outputs. New in 0.10.0
 * PENDING: ImportPrivKey: adds a private key to your wallet. The key should be formatted in the wallet import format created by the dumpprivkey RPC.
 * PENDING: ImportWallet: imports private keys from a file in wallet dump file format (see the dumpwallet RPC). These keys will be added to the keys currently in the wallet. This call may need to rescan all or parts of the block chain for transactions affecting the newly-added keys, which may take several minutes.
 * PENDING: KeyPoolRefill: fills the cache of unused pre-generated keys (the keypool).
 * ListAccounts: lists accounts and their balances. Updated in 0.10.0
 * PENDING: ListAddressGroupings: lists groups of addresses that may have had their common ownership made public by common use as inputs in the same transaction or from being used as change from a previous transaction.
 * PENDING: ListLockUnspent: returns a list of temporarily unspendable (locked) outputs.
 * PENDING: ListReceivedByAccount: lists the total number of bitcoins received by each account. Updated in 0.10.0
 * ListReceivedByAddress: lists the total number of bitcoins received by each address. Updated in 0.10.0
 * ListSinceBlock: gets all transactions affecting the wallet which have occurred since a particular block, plus the header hash of a block at a particular depth. Updated in 0.10.0
 * PENDING: ListTransactions: returns the most recent transactions that affect the wallet. Updated in 0.10.0
 * ListUnspent: returns an array of unspent transaction outputs belonging to this wallet. Updated in 0.10.0
 * LockUnspent: temporarily locks or unlocks specified transaction outputs. A locked transaction output will not be chosen by automatic coin selection when spending bitcoins. Locks are stored in memory only, so nodes start with zero locked outputs and the locked output list is always cleared when a node stops or fails.
 * PENDING: Move: moves a specified amount from one account in your wallet to another using an off-block-chain transaction.
 * PENDING: SendFrom: spends an amount from a local account to a bitcoin address.
 * PENDING: SendMany: creates and broadcasts a transaction which sends outputs to multiple addresses.
 * SendToAddress: spends an amount to a given address.
 * PENDING: SetAccount: puts the specified address in the given account.
 * PENDING: SetTxFee: sets the transaction fee per kilobyte paid by transactions created by this wallet.
 * PENDING: SignMessage: signs a message with the private key of an address.
 * PENDING: WalletLock: removes the wallet encryption key from memory, locking the wallet. After calling this method, you will need to call walletpassphrase again before being able to call any methods which require the wallet to be unlocked.
 * PENDING: WalletPassphrase: stores the wallet decryption key in memory for the indicated number of seconds. Issuing the walletpassphrase command while the wallet is already unlocked will set a new unlock time that overrides the old one.
 * PENDING: WalletPassphraseChange: changes the wallet passphrase from â€˜old passphraseâ€™ to â€˜new passphraseâ€™
 * 
 * @author dquintela
 */
public interface WalletRpc {
    /**
     * AddMultiSigAddress
     * Requires wallet support.
     * The addmultisigaddress RPC adds a P2SH multisig address to the wallet.
     * 
     * @param nrequired The minimum (m) number of signatures required to spend this m-of-n multisig script
     * @param keys the full public keys, or addresses for known public keys
     * @param account The account name in which the address should be stored. Default is the default account, "" (an empty string)
     * @return The P2SH multisig address. The address will also be added to the wallet, and outputs paying that address will be tracked by the wallet
     * 
     * https://bitcoin.org/en/developer-reference#addmultisigaddress
     */
    @JsonRpcMethod("addmultisigaddress")
    String addmultisigaddress(int nrequired, String[] keys, String account);
    
    @JsonRpcMethod("createmultisig")
    MultiSig createmultisig(int nrequired, String[] keys);

    /**
     * BackupWallet
     * Requires wallet support.
     * 
     * The backupwallet RPC safely copies wallet.dat to the specified file, 
     * which can be a directory or a path with filename.
     * 
     * @param destination A filename or directory name. If a filename, it will be created or overwritten. 
     *        If a directory name, the file wallet.dat will be created or overwritten within that directory
     */
    @JsonRpcMethod("backupwallet")
    void backupWallet(String destination);

    /**
     * DumpPrivKey
     * Requires wallet support. Requires an unlocked wallet or an unencrypted wallet.
     * 
     * The dumpprivkey RPC returns the wallet-import-format (WIP) private key corresponding to an address. 
     * (But does not remove it from the wallet.)
     * 
     * @param address The P2PKH address corresponding to the private key you want returned. 
     *                Must be the address corresponding to a private key in this wallet
     * @return The private key encoded as base58check using wallet import format
     */
    @JsonRpcMethod("dumpprivkey")
    String dumpPrivateKey(String address);

    /**
     * GetAccountAddress
     * Requires wallet support.
     * 
     * The getaccountaddress RPC returns the current Bitcoin address for receiving payments to this account. 
     * If the account doesnâ€™t exist, it creates both the account and a new address for receiving payment. 
     * Once a payment has been received to an address, future calls to this RPC for the same account will return a different address.
     * 
     * @param accountName The name of an account. Use an empty string ("") for the default account. 
     *        If the account doesnâ€™t exist, it will be created
     * @return An address [string (base58)], belonging to the account specified, which has not yet received any payments 
     */
    @JsonRpcMethod("getaccountaddress")
    String getAccountAddress(String accountName);

    /**
     * GetAccount
     * Requires wallet support.
     * 
     * The getaccount RPC returns the name of the account associated with the given address.
     * 
     * NOTE: If address is not present in wallet, "" is returned though
     * 
     * @param address [string (base58)] A P2PKH or P2SH Bitcoin address belonging either to a specific account or the default account (â€œâ€�)
     * @return The name of an account, or an empty string ("", the default account)
     */
    @JsonRpcMethod("getaccount")
    String getAccount(String address);

    /**
     * GetAddressesByAccount
     * Requires wallet support.
     * 
     * The getaddressesbyaccount RPC returns a list of every address assigned to a particular account.
     * 
     */
    @JsonRpcMethod("getaddressesbyaccount")
    String[] getaddressesbyaccount(String accountName);

    /**
     * GetNewAddress
     * Requires wallet support.
     * 
     * The getnewaddress RPC returns a new Bitcoin address for receiving payments. 
     * If an account is specified, payments received with the address will be credited to that account.
     * @param accountName The name of the account to put the address in. The default is the default account, an empty string ("")
     * @return A P2PKH address which has not previously been returned by this RPC. 
     *         The address will be marked as a receiving address in the wallet. 
     *         The address may already have been part of the keypool, so other RPCs such as the dumpwallet 
     *         RPC may have disclosed it previously. If the wallet is unlocked, its keypool will also be filled 
     *         to its max (by default, 100 unused keys). If the wallet is locked and its keypool is empty, 
     *         this RPC will fail
     */
    @JsonRpcMethod("getnewaddress")
    String getnewaddress(String accountName);

    /**
     * ImportAddress
     * Requires wallet support. Added in Bitcoin Core 0.10.0.
     * 
     * The importaddress RPC adds an address or pubkey script to the wallet without the associated private key, allowing you to watch 
     * for transactions affecting that address or pubkey script without being able to spend any of its outputs.
     * 
     * @param addressOrScript Either a P2PKH or P2SH address encoded in base58check, or a pubkey script encoded as hex
     * @param account The account into which to place the address or pubkey script
     *                An account name into which the address should be placed. Default is the default account, an empty string("")
     * @param rescan whether to rescan the block chain
     *               Set to true (the default) to rescan the entire local block database for transactions affecting any address or 
     *               pubkey script in the wallet (including transaction affecting the newly-added address or pubkey script). 
     *               Set to false to not rescan the block database (rescanning can be performed at any time by restarting Bitcoin 
     *               Core with the -rescan command-line argument). Rescanning may take several minutes. 
     *               Notes: if the address or pubkey script is already in the wallet, the block database will not be rescanned even 
     *               if this parameter is set
     */
    @JsonRpcMethod("importaddress")
    void importaddress(String addressOrScript, String account, boolean rescan);
    
    @JsonRpcMethod("importprivkey")
    void importprivkey(String privateKey, String account, boolean rescan);

    /**
     * ListAccounts
     * Requires wallet support.
     * 
     * The listaccounts RPC lists accounts and their balances.
     * 
     * @param confirmations The minimum number of confirmations an externally-generated transaction 
     *        must have before it is counted towards the balance. Transactions generated by this 
     *        node are counted immediately. Typically, externally-generated transactions are payments 
     *        to this wallet and transactions generated by this node are payments to other wallets. 
     *        Use 0 to count unconfirmed transactions. Default is 1
     *        
     * @param includeWatchOnly If set to true, include watch-only addresses in details and 
     *        calculations as if they were regular addresses belonging to the wallet. 
     *        If set to false (the default), treat watch-only addresses as if they didnâ€™t 
     *        belong to this wallet
     *        
     * @return a list of accounts and their balances
     */
    @JsonRpcMethod("listaccounts")
    Map<String, BigDecimal> listAccounts(int confirmations, boolean includeWatchOnly);

    /**
     * ListReceivedByAddress
     * Requires wallet support.
     * 
     * The listreceivedbyaddress RPC lists the total number of bitcoins received by each address.
     * 
     * @param confirmations The minimum number of confirmations an externally-generated transaction must have 
     *                  before it is counted towards the balance. Transactions generated by this node are counted immediately. 
     *                  Typically, externally-generated transactions are payments to this wallet and transactions generated 
     *                  by this node are payments to other wallets. Use 0 to count unconfirmed transactions. Default is 1
     * @param includeEmpty Set to true to display accounts which have never received a payment. Set to false (the default) to 
     *                  only include accounts which have received a payment. Any account which has received a payment will be 
     *                  displayed even if its current balance is 0
     * @param includeWatchOnly Added in Bitcoin Core 0.10.0 - If set to true, include watch-only addresses in details and 
     *                  calculations as if they were regular addresses belonging to the wallet. If set to false (the default), 
     *                  treat watch-only addresses as if they didnâ€™t belong to this wallet
     */
    @JsonRpcMethod("listreceivedbyaddress")
    AddressReceived[] listReceivedByAddress(int confirmations, boolean includeEmpty, boolean includeWatchOnly);

    /**
     * ListSinceBlock
     * Requires wallet support.
     * 
     * The listsinceblock RPC gets all transactions affecting the wallet which have occurred since a particular block, plus the 
     * header hash of a block at a particular depth.
     * 
     * @param blockHash The hash of a block header encoded as hex in RPC byte order. 
     *                   All transactions affecting the wallet which are not in that block or any earlier block will be returned, 
     *                   including unconfirmed transactions. Default is the hash of the genesis block, so all transactions affecting 
     *                   the wallet are returned by default
     * @param confirmations Sets the lastblock field of the results to the header hash of a block with this many confirmations. 
     *                   This does not affect which transactions are returned. 
     *                   Default is 1, so the hash of the most recent block on the local best block chain is returned
     * @param includeWatchOnly If set to true, include watch-only addresses in details and calculations as if they were regular 
     *                  addresses belonging to the wallet. If set to false (the default), treat watch-only addresses as if they 
     *                  didnâ€™t belong to this wallet
     *                  Added in Bitcoin Core 0.10.0
     */
    @JsonRpcMethod("listsinceblock")
    LastPayments listSinceBlock(String blockHash, int confirmations, boolean includeWatchOnly);

    /**
     * ListUnspent
     * Requires wallet support.
     * 
     * The listunspent RPC returns an array of unspent transaction outputs belonging to this wallet. 
     * Note: as of Bitcoin Core 0.10.0, outputs affecting watch-only addresses will be returned; 
     * see the spendable field in the results described below.
     * 
     * @param minimumConfirmations the minimum number of confirmations an output must have
     *              The minimum number of confirmations the transaction containing an output must have in order to be returned. 
     *              Use 0 to return outputs from unconfirmed transactions. Default is 1
     * @param maximumConfirmations the maximum number of confirmations an output may have
     *              The maximum number of confirmations the transaction containing an output may have in order to be returned. 
     *              Default is 9999999 (~10 million)
     * @param addresses If present, only outputs which pay an address in this array will be returned 
     *                  the addresses an output must pay
     *                  A P2PKH or P2SH address
     * @return the list of unspent outputs
     */
    @JsonRpcMethod("listunspent")
    Output[] listunspent(int minimumConfirmations, int maximumConfirmations, String[] addresses);

    /**
     * LockUnspent
     * Requires wallet support.
     * 
     * The lockunspent RPC temporarily locks or unlocks specified transaction outputs. 
     * A locked transaction output will not be chosen by automatic coin selection when spending bitcoins. 
     * Locks are stored in memory only, so nodes start with zero locked outputs and the locked output list 
     * is always cleared when a node stops or fails.
     * 
     * @param lockOrUnlock Set to true to lock the outputs specified in the following parameter. Set to false 
     *                     to unlock the outputs specified. If this is the only argument specified, all 
     *                     outputs will be unlocked (even if this is set to false)
     * @param unspentOutpoints the outputs to lock or unlock
     * 
     * @return Set to true if the outputs were successfully locked or unlocked
     */
    @JsonRpcMethod("lockunspent")
    boolean lockUnspent(boolean lockOrUnlock, Outpoint[] unspentOutpoints);

    /**
     * SendToAddress
     * Requires wallet support. Requires an unlocked wallet or an unencrypted wallet.
     * 
     * The sendtoaddress RPC spends an amount to a given address.
     * 
     * @param address A P2PKH or P2SH address to which the bitcoins should be sent
     * @param amount  The amount to spent in bitcoins
     * @param comment A locally-stored (not broadcast) comment assigned to this transaction. 
     *                Default is no comment
     *                Parameter #4â€�?a comment about who the payment was sent to
     * @param commentTo A locally-stored (not broadcast) comment assigned to this transaction. 
     *                Meant to be used for describing who the payment was sent to. 
     *                Default is no comment
     * @return The TXID of the sent transaction, encoded as hex in RPC byte order
     */
    @JsonRpcMethod("sendtoaddress")
    @JsonRpcErrors(@JsonRpcError(code = -6, message = "Insufficient funds", exception = InsuficientFundsException.class))
    String sendToAddress(String address, BigDecimal amount, String comment, String commentTo) throws InsuficientFundsException;
}
