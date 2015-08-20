package io.emax.heimdal.ethereum;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import io.emax.heimdal.ethereum.common.ByteUtilities;
import io.emax.heimdal.ethereum.common.DeterministicTools;
import io.emax.heimdal.ethereum.common.RLP;
import io.emax.heimdal.ethereum.common.RLPEntity;
import io.emax.heimdal.ethereum.common.RLPItem;
import io.emax.heimdal.ethereum.common.RLPList;
import io.emax.heimdal.ethereum.common.Secp256k1;
import io.emax.heimdal.ethereum.gethrpc.DefaultBlock;
import io.emax.heimdal.ethereum.gethrpc.EthereumRpc;

public class Wallet implements io.emax.heimdal.api.currency.Wallet {
  private EthereumRpc ethereumRpc = EthereumResource.getResource().getBitcoindRpc();
  private CurrencyConfiguration config = new CurrencyConfiguration();
  private static HashMap<String, Integer> addressRounds = new HashMap<>();
  private static HashMap<String, String> msigContracts = new HashMap<>();
  private static HashMap<String, String> reverseMsigContracts = new HashMap<>();

  public Wallet() {
    // TODO contract may very likely change
    String contractPayload = "0x6060604052361561002a5760e060020a6000350463c52ab77881146100c7578063cbf0b0c01461010e575b6101766101786706f05b59d3b20000600160a060020a0332163134016000805b600154821015610233576002826008811015610002570154600160a060020a0316319050838110156100bb578303808310156100835750815b60008111156100bb57600282600881101561000257604051910154600160a060020a031690600090839082818181858883f150505050505b6001919091019061004a565b61017a60043560243560443560006101a1326000805b60015481101561023957600160a060020a03831660028260088110156100025701541415610244576001915061023e565b610176600435600036604051808383808284375050509081018190039020905061018c815b600060ff81805b60015482101561016857600160a060020a0332166002836008811015610002570154141561024c5781925082505b8260ff1415610258576102bb565b005b565b60408051918252519081900360200190f35b1561019d5781600160a060020a0316ff5b5050565b1561022c57600036604051808383808284375050509081018190039020601054909250821490506101e757600d8054600160a060020a03191690556000600e5560108190555b6101f7816000816102c381610133565b15801561020f5750600d54600160a060020a03166000145b1561022c57600d8054600160a060020a03191685179055600e8390555b9392505050565b50505050565b600091505b50919050565b6001016100dd565b6001919091019061013a565b600a5485146102715760008054600c55600b55600a8590555b50600b54600283900a908116600014156102bb57600c54600190116102a8576000600c819055600b819055600a55600193506102bb565b600c8054600019019055600b8054821790555b505050919050565b1561023e57600d54600160a060020a031660001461023e57600d54600e54604051600160a060020a0392909216916000919082818181858883f15050600d8054600160a060020a031916905550600e819055601055506001939250505056";
    String txCount =
        ethereumRpc.eth_getTransactionCount("0x" + config.getContractAccount(), DefaultBlock.latest.toString());    
    int rounds = new BigInteger(1, ByteUtilities.toByteArray(txCount)).intValue();
    for(int i = 0; i < rounds; i++){
      RLPList contractAddress = new RLPList();
      RLPItem contractCreator = new RLPItem(ByteUtilities.toByteArray(config.getContractAccount()));
      RLPItem nonce = new RLPItem(ByteUtilities.stripLeadingNullBytes(BigInteger.valueOf(i).toByteArray()));
      contractAddress.add(contractCreator);
      contractAddress.add(nonce);
      
      String contract = DeterministicTools.hashSha3(ByteUtilities.toHexString(contractAddress.encode())).substring(96/4, 256/4);
      String contractCode = ethereumRpc.eth_getCode("0x" + contract.toLowerCase(), DefaultBlock.latest.toString());
      
      if(contractCode.equalsIgnoreCase(contractPayload)) {
        // We found an existing contract, data @ 2 should be the user address
    	  // TODO use the "2f54bf6e isOwner(address)" call maybe... 
    	  // Actually, should remove isOwner and replace with list-owners
        String userAddress = ethereumRpc.eth_getStorageAt("0x"+contract.toLowerCase(), "0x02", DefaultBlock.latest.toString());
        userAddress = ByteUtilities.toHexString(ByteUtilities.stripLeadingNullBytes(ByteUtilities.toByteArray(userAddress)));
        msigContracts.put(userAddress.toLowerCase(), contract.toLowerCase());        
        reverseMsigContracts.put(contract.toLowerCase(), userAddress.toLowerCase());
      }
    }   
  }
  
  @Override
  public String createAddress(String name) {
    // Generate the next private key
    int rounds = 1;
    String privateKey =
        DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);

    // Convert to an Ethereum address
    String publicAddress = DeterministicTools.getPublicAddress(privateKey);
    
    while(msigContracts.containsKey(publicAddress.toLowerCase())) {
    	rounds++;
    	privateKey =
    	        DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), rounds);
    	publicAddress = DeterministicTools.getPublicAddress(privateKey);
    }
    addressRounds.put(name, rounds);

    return publicAddress;
  }

  @Override
  public Iterable<String> getAddresses(String name) {
    int maxRounds = 1;
    if (addressRounds.containsKey(name)) {
      maxRounds = addressRounds.get(name);
    } else {
    	// Generate rounds
    	createAddress(name);
    	maxRounds = addressRounds.get(name);
    }

    LinkedList<String> addresses = new LinkedList<>();
    for (int i = 1; i <= maxRounds; i++) {
      addresses.add(DeterministicTools.getPublicAddress(
          DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), i)));
    }
    
    LinkedList<String> contracts = new LinkedList<>();
    for(String address : addresses){
      if(msigContracts.containsKey(address.toLowerCase())){
        contracts.add(msigContracts.get(address.toLowerCase()));
      }
    }

    return contracts;
  }

  @Override
  public String getMultiSigAddress(Iterable<String> addresses, String name) {
    // Look for existing msig account for this address.
    String userAddress = addresses.iterator().next().toLowerCase();
    if(msigContracts.containsKey(userAddress.toLowerCase())) {
      return msigContracts.get(userAddress).toLowerCase();
    }
    
    // Create the TX data structure
    RLPList tx = new RLPList();
    RLPItem nonce = new RLPItem();
    RLPItem gasPrice = new RLPItem(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    RLPItem gasLimit = new RLPItem(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));
    RLPItem to = new RLPItem();
    RLPItem value = new RLPItem();
    
    // Setup parameters for contract
    String contractInit = "606060405260405161039e38038061039e833981016040528051608051910190818160005b82518110156060578281815181101560025790602001906020020151600160a060020a0316600260005082600881101560025701556001016024565b50505160015560009081556103229150819061007c90396000f300";
    String contractPayload = "6060604052361561002a5760e060020a6000350463c52ab77881146100c7578063cbf0b0c01461010e575b6101766101786706f05b59d3b20000600160a060020a0332163134016000805b600154821015610233576002826008811015610002570154600160a060020a0316319050838110156100bb578303808310156100835750815b60008111156100bb57600282600881101561000257604051910154600160a060020a031690600090839082818181858883f150505050505b6001919091019061004a565b61017a60043560243560443560006101a1326000805b60015481101561023957600160a060020a03831660028260088110156100025701541415610244576001915061023e565b610176600435600036604051808383808284375050509081018190039020905061018c815b600060ff81805b60015482101561016857600160a060020a0332166002836008811015610002570154141561024c5781925082505b8260ff1415610258576102bb565b005b565b60408051918252519081900360200190f35b1561019d5781600160a060020a0316ff5b5050565b1561022c57600036604051808383808284375050509081018190039020601054909250821490506101e757600d8054600160a060020a03191690556000600e5560108190555b6101f7816000816102c381610133565b15801561020f5750600d54600160a060020a03166000145b1561022c57600d8054600160a060020a03191685179055600e8390555b9392505050565b50505050565b600091505b50919050565b6001016100dd565b6001919091019061013a565b600a5485146102715760008054600c55600b55600a8590555b50600b54600283900a908116600014156102bb57600c54600190116102a8576000600c819055600b819055600a55600193506102bb565b600c8054600019019055600b8054821790555b505050919050565b1561023e57600d54600160a060020a031660001461023e57600d54600e54604051600160a060020a0392909216916000919082818181858883f15050600d8054600160a060020a031916905550600e819055601055506001939250505056";
    // Parameters for constructor are appended after the contract code
    // Each value is a 64-byte hex entry, one after the next with no delimiters
    // Addresses[] - because it's an array we provide a pointer relative to the input data start, showing where you can find the data
    String accountOffset = String.format("%64s",  "40").replace(' ', '0');
    // Required sigs
    String requiredSigs = ByteUtilities.toHexString(BigInteger.valueOf(config.getMinSignatures()).toByteArray());
    requiredSigs = String.format("%64s", requiredSigs).replace(' ', '0');
    // Address[] - first entry in an array parameter is how many elements there are
    String numberOfAddresses = ByteUtilities.toHexString(BigInteger.valueOf(config.getMultiSigAddresses().length + 1).toByteArray());
    numberOfAddresses = String.format("%64s", numberOfAddresses).replace(' ', '0');
    // Build the array
    String[] addressesUsed = new String[config.getMultiSigAddresses().length + 1];
    addressesUsed[0] = String.format("%64s",  userAddress).replace(' ', '0');
    for(int i = 0; i < config.getMultiSigAddresses().length; i++) {
      addressesUsed[i+1] = String.format("%64s",  config.getMultiSigAddresses()[i]).replace(' ', '0');
    }
    // Contract code is the init code which copies the payload and constructor parameters, then runs the constructor
    // Followed by the payload, i.e. contract code that gets installed
    // Followed by the constructor params.
    String contractCode = contractInit + contractPayload + accountOffset + requiredSigs + numberOfAddresses;
    for(String addr : addressesUsed){
      contractCode += addr;
    }
    RLPItem data = new RLPItem(ByteUtilities.toByteArray(contractCode));

    tx.add(nonce);
    tx.add(gasPrice);
    tx.add(gasLimit);
    tx.add(to);
    tx.add(value);
    tx.add(data);
    
    // Sign it with our contract creator, creator needs funds to pay for the creation
    String rawTx = ByteUtilities.toHexString(tx.encode());
    String signedTx = signTransaction(rawTx, config.getContractAccount());
    
    // Signature failed, we got the same thing back
    if(signedTx.equalsIgnoreCase(rawTx)) {
    	return "";
    }
    
    // According to yellow paper address should be RLP(Sender, nonce)
    // We use this to predict the address, instead of waiting for a receipt.
    tx = (RLPList)RLP.parseArray(ByteUtilities.toByteArray(signedTx));
    nonce = (RLPItem)tx.get(0);
    
    RLPList contractAddress = new RLPList();
    RLPItem contractCreator = new RLPItem(ByteUtilities.toByteArray(config.getContractAccount()));
    contractAddress.add(contractCreator);
    contractAddress.add(nonce);
    
    sendTransaction(signedTx);
    
    // Figure out the contract address and store it in lookup tables for future use
    String contract = DeterministicTools.hashSha3(ByteUtilities.toHexString(contractAddress.encode())).substring(96/4, 256/4);
    msigContracts.put(userAddress, contract.toLowerCase());
    reverseMsigContracts.put(contract.toLowerCase(), userAddress);
    return contract;
  }

  @Override
  public String getBalance(String address) {
    // Get latest block
    BigInteger latestBlockNumber =
        new BigInteger("00" + ethereumRpc.eth_blockNumber().substring(2), 16);
    BigInteger confirmedBlockNumber =
        latestBlockNumber.subtract(BigInteger.valueOf(config.getMinConfirmations()));

    // Get balance at latest & latest - (min conf)
    BigInteger latestBalance = new BigInteger("00"
        + ethereumRpc.eth_getBalance(address, "0x" + latestBlockNumber.toString(16)).substring(2),
        16);
    BigInteger confirmedBalance = new BigInteger("00" + ethereumRpc
        .eth_getBalance(address, "0x" + confirmedBlockNumber.toString(16)).substring(2), 16);

    // convert to Ether and return the lower of the two
    confirmedBalance = confirmedBalance.min(latestBalance);
    BigDecimal etherBalance = new BigDecimal(confirmedBalance).divide(BigDecimal.valueOf(config.getWeiMultiplier()));
    return etherBalance.toPlainString();
  }

  @Override
  public String createTransaction(Iterable<String> fromAddress, String toAddress,
      BigDecimal amount) {
    
    String senderAddress = fromAddress.iterator().next();
    boolean isMsigSender = false;
    LinkedList<String> possibleSenders = new LinkedList<>();
    msigContracts.forEach((user, msig) -> {
      if(msig.equalsIgnoreCase(senderAddress))  {
        possibleSenders.add(user);
      }
    });
    if(!possibleSenders.isEmpty()) {
      isMsigSender = true;
    }

    // function addresses for the current contract:
    /* c52ab778 execute(address,uint256,uint256)
       cbf0b0c0 kill(address) */
    
    BigDecimal amountWei = amount.multiply(BigDecimal.valueOf(config.getWeiMultiplier()));

    // Create the transaction structure and serialize it
    RLPList tx = new RLPList();
    RLPItem nonce = new RLPItem();
    RLPItem gasPrice = new RLPItem(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getGasPrice()).toByteArray()));
    RLPItem gasLimit = new RLPItem(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(config.getSimpleTxGas()).toByteArray()));
    RLPItem to = new RLPItem(
        ByteUtilities.stripLeadingNullBytes(new BigInteger(toAddress, 16).toByteArray()));
    RLPItem value = new RLPItem(ByteUtilities
        .stripLeadingNullBytes(BigInteger.valueOf(amountWei.longValue()).toByteArray()));
    RLPItem data = new RLPItem();
    
    // If we're sending this from one of our msig accounts
    // We need to restructure things a little
    // TX info like to/from are moved into data, and the to is pointed to the contract.
    if(isMsigSender){
      // move things around to match the contract
      gasLimit = new RLPItem(ByteUtilities
          .stripLeadingNullBytes(BigInteger.valueOf(config.getContractGas()).toByteArray()));
      value = new RLPItem();
      // data... c52ab778 execute(address,uint256,uint256)
      String dataString = "c52ab778000000000000000000000000";
      dataString +=  toAddress;
      dataString += String.format("%64s", ByteUtilities.toHexString(amountWei.toBigInteger().toByteArray())).replace(' ', '0');
      byte[] dataNonce = DeterministicTools.getRandomBytes(64);
      String dataNonceFormated = ByteUtilities.toHexString(dataNonce);
      dataNonceFormated = String.format("%64s", dataNonceFormated).replace(' ', '0');
      dataString += dataNonceFormated;
      data = new RLPItem(ByteUtilities.toByteArray(dataString));
           
      to = new RLPItem(
        ByteUtilities.stripLeadingNullBytes(new BigInteger(senderAddress, 16).toByteArray()));
      
    }

    tx.add(nonce);
    tx.add(gasPrice);
    tx.add(gasLimit);
    tx.add(to);
    tx.add(value);
    tx.add(data);

    return ByteUtilities.toHexString(tx.encode());
  }

  @Override
  public String signTransaction(String transaction, String address) {
    // Validate the transaction data
    RLPEntity decodedTransaction = RLP.parseArray(ByteUtilities.toByteArray(transaction));
    if (decodedTransaction == null || decodedTransaction.getClass() != RLPList.class
        || ((RLPList) decodedTransaction).size() < 6) {
      return "";
    }
    
    // We've been asked to sign something for the multi-sig contract without a user-key
    // Going on the assumption that the local geth wallet only has one key
    if(reverseMsigContracts.containsKey(address.toLowerCase())) {
    	for(int i = 0; i < config.getMultiSigAddresses().length; i++) {
    		String altSig = signTransaction(transaction, config.getMultiSigAddresses()[i]);
    		if(!altSig.isEmpty()){
    			return altSig;
    		}
    	}
    }

    // Get the sigHash.
    // TODO create a TX class that knows how to do this.
    RLPList sigTx = new RLPList();
    sigTx.add(((RLPList) decodedTransaction).get(0)); // nonce
    sigTx.add(((RLPList) decodedTransaction).get(1)); // gasPrice
    sigTx.add(((RLPList) decodedTransaction).get(2)); // gasLimit
    sigTx.add(((RLPList) decodedTransaction).get(3)); // to
    sigTx.add(((RLPList) decodedTransaction).get(4)); // value
    sigTx.add(((RLPList) decodedTransaction).get(5)); // data

    String txCount =
        ethereumRpc.eth_getTransactionCount("0x" + address, DefaultBlock.latest.toString());
    BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount));
    if(nonce.equals(BigInteger.ZERO)) {
      sigTx.get(0).setDecodedContents(new byte[] {});
    } else {
      sigTx.get(0).setDecodedContents(ByteUtilities.stripLeadingNullBytes(nonce.toByteArray()));
    }

    String sigString = ByteUtilities.toHexString(sigTx.encode());
    sigString = DeterministicTools.hashSha3(sigString);

    // Catch errors here
    String sig = "";
    try{
    	sig = ethereumRpc.eth_sign("0x" + address, sigString);
    } catch (Exception e) {
    	return transaction;
    }
    
    byte[] sigBytes = ByteUtilities.toByteArray(sig);
    byte[] sigR = Arrays.copyOfRange(sigBytes, 0, 32);
    byte[] sigS = Arrays.copyOfRange(sigBytes, 32, 64);
    byte[] sigV = Arrays.copyOfRange(sigBytes, 64, 65);

    // convert recoveryId, because it has to be 27/28.
    sigV[0] += 27;
    RLPItem recId = new RLPItem(sigV);
    RLPItem r = new RLPItem(sigR);
    RLPItem s = new RLPItem(sigS);

    sigTx.add(recId);
    sigTx.add(r);
    sigTx.add(s);

    return ByteUtilities.toHexString(sigTx.encode());
  }

  @Override
  public String signTransaction(String transaction, String address, String name) {
    // Validate the transaction data
    RLPEntity decodedTransaction = RLP.parseArray(ByteUtilities.toByteArray(transaction));
    if (decodedTransaction == null || decodedTransaction.getClass() != RLPList.class
        || ((RLPList) decodedTransaction).size() < 6) {
      return transaction;
    }
    
    // We've been asked to sign something for the multi-sig contract 
    // Replace it with our user's address
    if(reverseMsigContracts.containsKey(address.toLowerCase())) {
    	String translatedAddress = reverseMsigContracts.get(address.toLowerCase());
    	return signTransaction(transaction, translatedAddress, name);    	
    }

    // Get the sigHash.
    // TODO create a TX class that knows how to do this.
    RLPList sigTx = new RLPList();
    sigTx.add(((RLPList) decodedTransaction).get(0)); // nonce
    sigTx.add(((RLPList) decodedTransaction).get(1)); // gasPrice
    sigTx.add(((RLPList) decodedTransaction).get(2)); // gasLimit
    sigTx.add(((RLPList) decodedTransaction).get(3)); // to
    sigTx.add(((RLPList) decodedTransaction).get(4)); // value
    sigTx.add(((RLPList) decodedTransaction).get(5)); // data

    String txCount =
        ethereumRpc.eth_getTransactionCount("0x" + address, DefaultBlock.latest.toString());
    BigInteger nonce = new BigInteger(1, ByteUtilities.toByteArray(txCount));
    if(nonce.equals(BigInteger.ZERO)) {
      sigTx.get(0).setDecodedContents(new byte[] {});
    } else {
      sigTx.get(0).setDecodedContents(ByteUtilities.stripLeadingNullBytes(nonce.toByteArray()));
    }

    String sigString = ByteUtilities.toHexString(sigTx.encode());
    sigString = DeterministicTools.hashSha3(sigString);

    // Determine the private key to use
    int rounds = 1;
    if (addressRounds.containsKey(name)) {
      rounds = addressRounds.get(name);
    } else {
    	// Generate rounds
    	createAddress(name);
    	rounds = addressRounds.get(name);
    }
    
    String privateKey = "";
    for (int i = 1; i <= rounds; i++) {
      String privateKeyCheck =
          DeterministicTools.getDeterministicPrivateKey(name, config.getServerPrivateKey(), i);
      if (DeterministicTools.getPublicAddress(privateKeyCheck).equalsIgnoreCase(address)) {
        privateKey = privateKeyCheck;
        break;
      } 
    }
    if (privateKey == "") {
      return transaction;
    }

    // Sign and return it
    byte[] privateBytes = ByteUtilities.toByteArray(privateKey);
    byte[] sigBytes = ByteUtilities.toByteArray(sigString);
    byte[] sig = Secp256k1.signTransaction(sigBytes, privateBytes);

    byte[] recoveryId = Arrays.copyOfRange(sig, 0, 1);
    byte[] sigR = Arrays.copyOfRange(sig, 1, 33);
    byte[] sigS = Arrays.copyOfRange(sig, 33, 65);

    RLPItem recId = new RLPItem(recoveryId);
    RLPItem r = new RLPItem(sigR);
    RLPItem s = new RLPItem(sigS);

    sigTx.add(recId);
    sigTx.add(r);
    sigTx.add(s);

    return ByteUtilities.toHexString(sigTx.encode());
  }

  @Override
  public String sendTransaction(String transaction) {
    return ethereumRpc.eth_sendRawTransaction(transaction);
  }

}
