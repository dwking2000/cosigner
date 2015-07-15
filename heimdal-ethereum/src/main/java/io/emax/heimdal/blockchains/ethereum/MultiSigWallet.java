package io.emax.heimdal.blockchains.ethereum;


import io.emax.heimdal.common.Utility;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pojo for multi-signature wallets for Ethereum
 */
public class MultiSigWallet implements Contract {
    private static AtomicReference<String> multiSigWalletTemplate = new AtomicReference<String>("");

    /**
     * Lazy getter for the multisig wallet template
     *
     * @return The multisig wallet template
     */
    private String getMultiSigWalletTemplate() {
        if (Objects.equals(multiSigWalletTemplate.get(), "")) {
            InputStream inputStream = this.getClass().getResourceAsStream("multisig.sl");
            multiSigWalletTemplate.set(Utility.slurpStream(inputStream));
        }
        return multiSigWalletTemplate.get();
    }

    public String getContract() {
        return contract;
    }

    public String getCode() {
        return code;
    }

    public String getCoinbase1() {
        return coinbase1;
    }

    public String getCoinbase2() {
        return coinbase2;
    }

    public String getColdStorage() {
        return coldStorage;
    }

    public String getAddress() {
        return address;
    }

    final String contract;
    final String code;
    final String coinbase1;
    final String coinbase2;
    final String coldStorage;
    final String address;


    public MultiSigWallet(String address,
                          String coinbase1,
                          String coinbase2,
                          String coldStorage) throws IOException, InterruptedException, ExecutionException {
        this.address = address;
        this.coinbase1 = coinbase1;
        this.coinbase2 = coinbase2;
        this.coldStorage = coldStorage;
        contract = getMultiSigWalletTemplate()
                .replaceAll("0xDEADBEEF0", getColdStorage())
                .replaceAll("0xDEADBEEF1", getCoinbase1())
                .replaceAll("0xDEADBEEF2", getCoinbase2());
        code = Solidity.compile(contract).get();
    }

    @Override
    public String toString() {
        return contract;
    }
}
