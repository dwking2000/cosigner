package io.emax.heimdal.bitcoin.bitcoind;

import java.util.Arrays;
import java.util.List;

/**
 * Helper to estimate transaction size
 * 
 * @author dquintela
 * 
 *         https://en.bitcoin.it/wiki/Protocol_specification#tx
 *         http://www.righto.com/2014/02/bitcoins-hard-way-using-raw-bitcoin.html
 *         http://www.soroushjp.com/2014/12/20/bitcoin-multisig-the-hard-way-understanding-raw-
 *         multisignature-bitcoin-transactions/
 *         https://bitcoin.stackexchange.com/questions/1188/what-strategies-can-an-e-wallet-use-in-
 *         order-to-reduce-transaction-fees/1191#1191
 *         https://bitcoin.stackexchange.com/questions/1195/how-to-calculate-transaction-size-before
 *         -sending
 *         https://bitcoin.stackexchange.com/questions/7537/calculator-for-estimated-tx-fees
 */
public class BitcoinHelper {
  public static final int BITCOIN_DEFAULT_CONFIRMATIONS = 6;

  public static final int BITCOIN_AMOUNT_MAX_SCALE = 12;

  public static final int BITCOIN_AMOUNT_MAX_INTEGER = 8; // Max 21M Bitcoins
  public static final int BITCOIN_AMOUNT_MAX_DECIMAL = BITCOIN_AMOUNT_MAX_SCALE;
  public static final int BITCOIN_AMOUNT_MAX_DIGITS =
      BITCOIN_AMOUNT_MAX_INTEGER + BITCOIN_AMOUNT_MAX_DECIMAL;


  public static final int UINT8_SIZE = 1;
  public static final int UINT16_SIZE = 2;
  public static final int UINT32_SIZE = 4;
  public static final int UINT64_SIZE = 8;

  public static final int ADDRESS_SIZE = 20;
  public static final int OUTPOINT_SIZE = 32 + UINT32_SIZE;
  public static final int SEQUENCE_SIZE = UINT32_SIZE;
  public static final int TRANSACTION_VALUE_SIZE = UINT64_SIZE;
  public static final int VERSION_SIZE = UINT32_SIZE;
  public static final int LOCKTIME_SIZE = UINT32_SIZE;
  public static final int OP_SIZE = UINT8_SIZE;
  public static final int PUBKEY_SIZE = 65;

  public static class TxIn {
    private final ScriptPubKeyType scriptPubKeyType;
    private final int signatures;
    private final int ofSignatures;

    public TxIn(ScriptPubKeyType scriptPubKeyType, int signatures, int ofSignatures) {
      this.scriptPubKeyType = scriptPubKeyType;
      this.signatures = signatures;
      this.ofSignatures = ofSignatures;
    }

    public ScriptPubKeyType getScriptPubKeyType() {
      return scriptPubKeyType;
    }

    public int getSignatures() {
      return signatures;
    }

    public int getOfSignatures() {
      return ofSignatures;
    }
  }

  public static class TxOut {
    private final ScriptPubKeyType scriptPubKeyType;

    public TxOut(ScriptPubKeyType scriptPubKeyType) {
      this.scriptPubKeyType = scriptPubKeyType;
    }

    public ScriptPubKeyType getScriptPubKeyType() {
      return scriptPubKeyType;
    }
  }

  public static int estimateTransactionSize(List<TxIn> inputs, List<TxOut> outputs) {
    final int inputsSize = inputs.stream().mapToInt(BitcoinHelper::estimateInputSize).sum();
    final int outputsSize = outputs.stream().mapToInt(BitcoinHelper::estimateOutputSize).sum();
    return VERSION_SIZE + varIntSize(inputs.size()) + inputsSize + varIntSize(outputs.size())
        + outputsSize + LOCKTIME_SIZE;
  }

  public static int estimateInputSize(TxIn txIn) {
    final int scriptSigSize = estimateInputSize_ScriptSig(txIn);
    return OUTPOINT_SIZE + varIntSize(scriptSigSize) + scriptSigSize + SEQUENCE_SIZE;
  }

  public static int estimateOutputSize(TxOut txOut) {
    final int scriptPubKeySize = estimateOutputSize_ScriptPubKey(txOut);
    return TRANSACTION_VALUE_SIZE + varIntSize(scriptPubKeySize) + scriptPubKeySize;
  }

  public static int estimateInputSize_ScriptSig(TxIn txIn) {
    // Use upper interval
    int signatureSize = 73; // 71 or 72 or 73
    switch (txIn.getScriptPubKeyType()) {
      case scripthash: {
        // http://www.soroushjp.com/2014/12/20/bitcoin-multisig-the-hard-way-understanding-raw-multisignature-bitcoin-transactions/
        // <OP_0> <sig A> <sig C> OP_PUSHDATA1 <redeemScript>
        int redeemScriptSize =
            estimateRedeemScriptSize(txIn.getSignatures(), txIn.getOfSignatures());
        return OP_SIZE + txIn.getSignatures() * (varIntSize(signatureSize) + signatureSize)
            + OP_SIZE + varIntSize(redeemScriptSize) + redeemScriptSize;
      }
      case pubkeyhash: {
        int pubkeySize = PUBKEY_SIZE;
        return varIntSize(signatureSize) + signatureSize + varIntSize(pubkeySize) + pubkeySize;
      }
      case pubkey: {
        return varIntSize(signatureSize) + signatureSize;
      }
      default:
        throw new IllegalArgumentException(
            "Unhandled ScriptPubKeyType=<" + txIn.getScriptPubKeyType() + ">");
    }
  }

  public static int estimateOutputSize_ScriptPubKey(TxOut txOut) {
    switch (txOut.getScriptPubKeyType()) {
      case scripthash:
        // OP_HASH160 <redeemScriptHash> OP_EQUAL
        // http://www.soroushjp.com/2014/12/20/bitcoin-multisig-the-hard-way-understanding-raw-multisignature-bitcoin-transactions/
        int redeemScriptHashSize = ADDRESS_SIZE;
        return OP_SIZE + varIntSize(redeemScriptHashSize) + redeemScriptHashSize + OP_SIZE;
      case pubkeyhash:
        // OP_DUP OP_HASH160 <pubKeyHash> OP_EQUALVERIFY OP_CHECKSIG
        // http://www.righto.com/2014/02/bitcoins-hard-way-using-raw-bitcoin.html?m=1
        int pubKeyHashSize = ADDRESS_SIZE;
        return OP_SIZE + OP_SIZE + varIntSize(pubKeyHashSize) + pubKeyHashSize + OP_SIZE + OP_SIZE;
      case pubkey:
        // <pubKey> OP_CHECKSIG
        // https://en.bitcoin.it/wiki/Script#Obsolete_pay-to-pubkey_transaction
        int pubKeySize = PUBKEY_SIZE;
        return varIntSize(pubKeySize) + pubKeySize + OP_SIZE;
      default:
        throw new IllegalArgumentException(
            "Unhandled ScriptPubKeyType=<" + txOut.getScriptPubKeyType() + ">");
    }
  }

  public static int estimateRedeemScriptSize(int signatures, int offSignatures) {
    // http://www.soroushjp.com/2014/12/20/bitcoin-multisig-the-hard-way-understanding-raw-multisignature-bitcoin-transactions/
    // <OP_2> <A pubkey> <B pubkey> <C pubkey> <OP_3> <OP_CHECKMULTISIG>
    int pubkeySize = PUBKEY_SIZE;
    return OP_SIZE + offSignatures * (varIntSize(pubkeySize) + pubkeySize) + OP_SIZE + OP_SIZE;
  }

  // https://en.bitcoin.it/wiki/Protocol_specification#Variable_length_integer
  public static int varIntSize(long value) {
    assert(value >= 0);

    if (value < 0xfd) {
      return UINT8_SIZE;
    } else if (value <= 0xffff) {
      return 1 + UINT16_SIZE;
    } else if (value <= 0xffffffff) {
      return 1 + UINT32_SIZE;
    } else {
      return 1 + UINT64_SIZE;
    }
  }

  public static void main(String[] args) {
    /*
     * A) 1 input/1 output (sending exact balance of one input) = 191 bytes B) 1 input/2 output
     * (sending money from single input with some change) = 258 bytes C) 2 input/1 output
     * (consolidating two address balances to one output) = 404 bytes D) 12 input/2 output (sending
     * from many inputs with change) = 1851 bytes
     */
    TxIn in = new TxIn(ScriptPubKeyType.pubkeyhash, 1, 1);
    TxIn inscript = new TxIn(ScriptPubKeyType.scripthash, 2, 3);
    TxOut out = new TxOut(ScriptPubKeyType.pubkeyhash);
    TxOut outscript = new TxOut(ScriptPubKeyType.scripthash);

    TxIn[] in_2 = new TxIn[2];
    TxIn[] in_12 = new TxIn[12];
    TxIn[] in_40 = new TxIn[40];
    TxOut[] out_2 = new TxOut[2];
    TxOut[] out_16 = new TxOut[16];
    Arrays.fill(in_2, in);
    Arrays.fill(in_12, in);
    Arrays.fill(in_40, in);
    Arrays.fill(out_2, out);
    Arrays.fill(out_16, out);

    System.out.println("estimateInputSize(in)=" + estimateInputSize(in));
    System.out.println("estimateInputSize_ScriptSig(in)=" + estimateInputSize_ScriptSig(in));
    System.out.println("estimateOutputSize(out)=" + estimateOutputSize(out));
    System.out
        .println("estimateOutputSize_ScriptPubKey(out)=" + estimateOutputSize_ScriptPubKey(out));

    System.out.println("1 input[pubkeyhash]/1 output[pubkeyhash] [ 223 bytes ] : "
        + estimateTransactionSize(Arrays.asList(in), Arrays.asList(out)));
    System.out.println("1 input[pubkeyhash]/1 output[scripthash] [ 221 bytes ] : "
        + estimateTransactionSize(Arrays.asList(in), Arrays.asList(outscript)));
    System.out.println("1 input[scripthash]/1 output[pubkeyhash] [ 436 bytes ] : "
        + estimateTransactionSize(Arrays.asList(inscript), Arrays.asList(out)));
    System.out.println("1 input[scripthash]/1 output[scripthash] [ xxx bytes ] : "
        + estimateTransactionSize(Arrays.asList(inscript), Arrays.asList(outscript)));
    System.out
        .println("2 input[scripthash,scripthash]/2 output[scripthash,scripthash] [ xxx bytes ] : "
            + estimateTransactionSize(Arrays.asList(inscript, inscript),
                Arrays.asList(outscript, outscript)));

    System.out.println("B) 1 input/2 output [ 258 bytes? ]: "
        + estimateTransactionSize(Arrays.asList(in), Arrays.asList(out_2)));
    System.out.println("C) 2 input/1 output [ 404 bytes? ]: "
        + estimateTransactionSize(Arrays.asList(in_2), Arrays.asList(out)));
    System.out.println("D) 12 input/2 output [ 1851 bytes? ]: "
        + estimateTransactionSize(Arrays.asList(in_12), Arrays.asList(out_2)));
    System.out.println(
        "https://blockexplorer.com/tx/57488f3f8c3961dfa6e76b361aabb1f8e82fb08ea41746604ee6566dd52830de\n40 input/16 output [ 7.761 kilobytes ]: "
            + estimateTransactionSize(Arrays.asList(in_40), Arrays.asList(out_16)) / 1000f);

  }
}
