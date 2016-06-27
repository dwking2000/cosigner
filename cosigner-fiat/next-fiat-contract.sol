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

  mapping(address => uint) lastActiveBlock;
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

  event Transfer(address indexed _from, address indexed _to, uint256 _value);
  event Approval(address indexed _owner, address indexed _spender, uint256 _value);

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

  // This is a multi-sig based contract designed to be controlled by cosigner
  // As such, we do not support the write/update functions of the token API
  // Implementing the read-only portion of the official token API
  function allowance(address _owner, address _spender) constant returns (uint256 remaining) {
    // This exists for API compatibility, we do not support allowances.
    return 0;
  }

  function balanceOf(address _owner) constant returns (uint256 balance) {
    return balances[_owner];
  }

  function totalSupply() constant returns (uint256 supply) {
    return totalBalance;
  }
  // End Token API

  function getConfirmations(address addr) public returns (uint) {
    return (block.number - lastActiveBlock[addr]);
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

  // TODO Consider implementing these, though the token standard is apparently in "E"-RFC
  // function transfer(address _to, uint256 _value) returns (bool success)
  // function transferFrom(address _from, address _to, uint256 _value) returns (bool success)
  // function approve(address _spender, uint256 _value) returns (bool success)
  // function allowance(address _owner, address _spender) constant returns (uint256 remaining)

  // TODO Create a suicide function for migration

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
          lastActiveBlock[sender] = block.number;
          lastActiveBlock[transactions[i].to] = block.number;
          Transfer(sender, transactions[i].to, transactions[i].value);
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
      lastActiveBlock[admin] = block.number;
    }
  }

  function destroyTokens(uint amount) {
    if(balances[admin] >= amount && msg.sender == admin) {
      balances[admin] -= amount;
      totalBalance -= amount;
      lastActiveBlock[admin] = block.number;
    } else {
      throw;
    }
  }
}
