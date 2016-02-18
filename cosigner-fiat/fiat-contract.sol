contract fiatcontract {
  uint m_required;
  uint lastNonce;

  uint signers;
  uint numSigners;

  uint m_numOwners;
  address admin;
  bool adminSigned;
  mapping(address => uint) ownerIndex;
  mapping(uint => address) owners;

  uint transactionNonce;
  address sender;
  bool senderSigned;
  bytes32 transactionHash;
  uint numTransactions;
  mapping(uint => Transaction) transactions;

  uint numSignatures;
  mapping(uint => SigData) signatures;

  mapping(address => uint) balances;

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
      } else if(signer == sender && !senderSigned) {
        numSigners++;
        senderSigned = true;
      } else if(signer == admin && !adminSigned) {
        numSigners++;
        adminSigned = true;
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
    address[] ownersList;
    ownersList.length = m_numOwners;
    for(uint i = 0; i < m_numOwners; i++) {
      ownersList[i] = owners[i+1];
    }

    return ownersList;
  }

  // constructor is given number of sigs required to do protected "onlymanyowners" transactions
  // as well as the selection of addresses capable of confirming them.
  function multisigwallet(address _admin, address[] _owners, uint _required) {
    lastNonce = 0;
    admin = _admin;
    for (uint i = 0; i < _owners.length; i++)
    {
      owners[i+1] = _owners[i];
      ownerIndex[_owners[i]] = i+1;
    }
    m_numOwners = _owners.length;
    m_required = _required;
  }

  // kills the contract sending everything to `_to`.
  function kill(uint nonce, address[] to, uint[] value,  
      uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
    numTransactions = 1;
    transactions[0].to = to[0];
    transactions[0].value = value[0];
    transactionNonce = nonce;      

    numSignatures = sigV.length;
    for(uint i = 0; i < sigV.length && i < 8; i++) {
      signatures[i].sigV = sigV[i];  
      signatures[i].sigR = sigR[i];  
      signatures[i].sigS = sigS[i];
    }

    if(confirmTransaction()) {
      lastNonce = transactionNonce;
      suicide(transactions[0].to);
    }
  }

  function execute(uint nonce, address _sender, address[] to, uint[] value,
      uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
    sender = _sender;
    senderSigned = false;
    adminSigned = false;
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

    if(confirmTransaction()) {
      lastNonce = transactionNonce;
      for(i = 0; i < numTransactions; i++) {
        // TODO Don't send(), update balances in map.
        if(!transactions[i].to.send(transactions[i].value)) {
          // Rollback the tx if there'x a problem executing it.
          throw;
        }
      }
    }
  }

  // deposit
  function () {
  }

  // TODO CreationFunction
  // TODO DestructionFunction
}
