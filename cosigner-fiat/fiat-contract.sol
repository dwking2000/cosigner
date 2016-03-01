contract fiatcontract {
  uint m_required;
  uint lastNonce;

  uint signers;
  uint numSigners;

  uint m_numOwners;
  address admin;
  uint adminSigned;
  mapping(address => uint) ownerIndex;
  mapping(uint => address) owners;

  uint transactionNonce;
  address sender;
  uint senderSigned;
  bytes32 transactionHash;
  uint numTransactions;
  mapping(uint => Transaction) transactions;

  uint numSignatures;
  mapping(uint => SigData) signatures;

  // TODO Add address => block # mapping for confirmations
  mapping(address => uint) balances;
  uint totalBalance;

  // Transaction and signature structures
  struct Transaction {
    address to;
    uint value;
  }

  struct SigData { 
    uint8 sigV;
    bytes32 sigR;
    bytes32 sigS;
  }      

  function confirmTransaction() internal returns (bool) {
    transactionHash = 0x00;
    senderSigned = 0;
    adminSigned = 0;
    for(uint i = 0; i < numTransactions; i++) {
      transactionHash = sha3(transactionHash, transactions[i].to, transactions[i].value, transactionNonce);        
    }


    if(transactionNonce != lastNonce + 1)
      return false;

    signers = 0;
    numSigners = 0;
    for (i = 0; i < numSignatures && i < 8; i++) {
      address signer = ecrecover(transactionHash, signatures[i].sigV, signatures[i].sigR, signatures[i].sigS);
      uint ownerBit = ownerIndexBit(signer);
      uint ownerValue = 2**ownerBit;
      if(ownerBit > 0 && (signers & ownerValue == 0)) {
        signers |= ownerValue;
        numSigners++;
      } else if(signer == sender && senderSigned == 0) {
        numSigners++;
        senderSigned = 1;
      } else if(signer == admin && adminSigned == 0) {
        numSigners++;
        adminSigned = 1;
      }
    }

    if(numSigners >= m_required) return true;

    return false;
  }

  function ownerIndexBit(address addr) internal returns (uint) {
    return ownerIndex[addr]; 
  }

  function isOwner(address addr) public returns (bool) {
    return (ownerIndex[addr] > 0);
  }

  function getOwners() public returns (address[]) {
    address[] memory ownersList = new address[](m_numOwners);
    for(uint i = 0; i < m_numOwners; i++) {
      ownersList[i] = owners[i+1];
    }

    return ownersList;
  }

  // TODO function getBalanceConfirmations(address addr) public returns (uint)

  function getBalance(address addr) public returns (uint) {
    return balances[addr];
  }

  function getTotalBalance() public returns (uint) {
    return totalBalance;
  }

  // constructor is given number of sigs required to do protected "onlymanyowners" transactions
  // as well as the selection of addresses capable of confirming them.
  function fiatcontract(address _admin, address[] _owners, uint _required) {
    lastNonce = 0;
    totalBalance = 0;
    admin = _admin;
    for (uint i = 0; i < _owners.length; i++)
    {
      owners[i+1] = _owners[i];
      ownerIndex[_owners[i]] = i+1;
    }
    m_numOwners = _owners.length;
    m_required = _required;
  }

  function transfer(uint nonce, address _sender, address[] to, uint[] value,
      uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
    sender = _sender;
    numTransactions = to.length;
    for(uint i = 0; i < numTransactions; i++) {
      transactions[i].to = to[i];
      transactions[i].value = value[i];
    }
    transactionNonce = nonce;

    numSignatures = sigV.length;
    for(i = 0; i < numSignatures; i++) {
      signatures[i].sigV = sigV[i];  
      signatures[i].sigR = sigR[i];  
      signatures[i].sigS = sigS[i];
    }

    if(confirmTransaction() && senderSigned == 1) {
      lastNonce = transactionNonce;
      for(i = 0; i < numTransactions; i++) {
        if(balances[sender] >= transactions[i].value) {
          balances[sender] -= transactions[i].value;
          balances[transactions[i].to] += transactions[i].value;
        } else {
          // Rollback the tx if there'x a problem executing it.
          throw;
        }
      }
    }
  }

  // try to prevent deposits
  function () {
    throw;
  }

  function createTokens(uint amount) {
    if(msg.sender == admin) {
      balances[admin] += amount;
      totalBalance += amount;
    }
  }

  function destroyTokens(uint amount) {
    if(balances[admin] >= amount && msg.sender == admin) {
      balances[admin] -= amount;
      totalBalance -= amount;
    } else {
      throw;
    }
  }
}
