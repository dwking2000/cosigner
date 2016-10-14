pragma soldity ^0.4.2;

contract EthStorageContract {
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
  event Deposit(address indexed _from, address indexed _to, uint256 _value);
  event Reconcile(address indexed _affected, int256 _value);
  event Sweep(address indexed _requestor, address indexed _to, uint256 _value);
  event TopUp(address indexed _from, uint256 _value);

  function calculateTxHash() internal returns (bytes32) {
    transactionHash = 0x00;
    for(uint i = 0; i < numTransactions; i++) {
      transactionHash = sha3(transactionHash, transactions[i].to, transactions[i].value, transactionNonce);
    }

    return transactionHash;
  }

  function calculateAdminTxHash() internal returns (bytes32) {
     transactionHash = sha3(lastNonce + 1);
     return transactionHash;
  }

  function confirmTransactionSig() internal returns (bool) {
    if(transactionNonce != lastNonce + 1)
      return false;

    senderSigned = 0;
    adminSigned = 0;
    signers = 0;
    numSigners = 0;
    for (uint i = 0; i < numSignatures && i < 8; i++) {
      address signer = ecrecover(transactionHash, signatures[i].sigV, signatures[i].sigR, signatures[i].sigS);
      uint ownerBit = ownerIndexBit(signer);
      uint ownerValue = 2**ownerBit;
      if(ownerBit > 0 && (signers & ownerValue == 0)) {
        signers |= ownerValue;
        numSigners++;
      }
      if(signer == sender) {
        senderSigned = 1;
      }
      if(signer == admin) {
        adminSigned = 1;
      }
    }

    if(numSigners >= m_required) {
        lastNonce = transactionNonce;
        return true;
    }

    return false;
  }

  function confirmTransaction() internal returns (bool) {
      calculateTxHash();
      return confirmTransactionSig();
  }

  function confirmAdminTx(uint nonce) internal returns (bool) {
    transactionNonce = nonce;
    calculateAdminTxHash();
    return (confirmTransactionSig() && adminSigned == 1);
  }

  function ownerIndexBit(address addr) internal returns (uint) {
    return ownerIndex[addr];
  }

  function isOwner(address addr) constant returns (bool) {
    return (ownerIndex[addr] > 0);
  }

  function getOwners() constant returns (address[]) {
    address[] memory ownersList = new address[](m_numOwners);
    for(uint i = 0; i < m_numOwners; i++) {
      ownersList[i] = owners[i+1];
    }

    return ownersList;
  }

  function allowance(address _owner, address _spender) constant returns (uint256 remaining) {
    return 0;
  }

  function balanceOf(address _owner) constant returns (uint256 balance) {
    return balances[_owner];
  }

  function totalSupply() constant returns (uint256 supply) {
    return totalBalance;
  }

  function EthStorageContract(address _tokenContract, address _admin, address[] _owners, uint _required) {
    // Extra param is for api compatibility with the sub-token version
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

  function updateOwners(uint nonce, address _admin, address[] _owners, uint _required,
                        uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
    // Must be signed by everyone and the admin.
    numSignatures = sigV.length;
    for(uint i = 0; i < numSignatures; i++) {
      signatures[i].sigV = sigV[i];
      signatures[i].sigR = sigR[i];
      signatures[i].sigS = sigS[i];
    }
    if(confirmAdminTx(nonce)) {
        admin = _admin;
        for (i = 0; i < _owners.length; i++)
        {
          owners[i+1] = _owners[i];
          ownerIndex[_owners[i]] = i+1;
        }
        m_numOwners = _owners.length;
        m_required = _required;
    }
  }

  function deleteContract(uint nonce, uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
    // Must be signed by everyone and the admin
    numSignatures = sigV.length;
    for(uint i = 0; i < numSignatures; i++) {
      signatures[i].sigV = sigV[i];
      signatures[i].sigR = sigR[i];
      signatures[i].sigS = sigS[i];
    }
    if(confirmAdminTx(nonce) == true) {
        suicide(admin);
    }
  }

  function deposit(address _to) payable returns (bool) {
        balances[_to] += msg.value;
        totalBalance += msg.value;
        Deposit(msg.sender, _to, msg.value);
        return true;
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
      for(i = 0; i < numTransactions; i++) {
        if(balances[sender] >= transactions[i].value) {
          balances[sender] -= transactions[i].value;
          totalBalance -= transactions[i].value;
          if(transactions[i].to.call.value(transactions[i].value)()) {
              Transfer(sender, transactions[i].to, transactions[i].value);
          } else {
              throw;
          }
        } else {
          throw;
        }
      }
    }
  }

  // try to prevent deposits
  function () {
    throw;
  }

  function reconcile(uint nonce, address[] _to, int[] amount,
                        uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
    numSignatures = sigV.length;
    for(uint i = 0; i < numSignatures; i++) {
      signatures[i].sigV = sigV[i];
      signatures[i].sigR = sigR[i];
      signatures[i].sigS = sigS[i];
    }

    if(confirmAdminTx(nonce)) {
        for(i = 0; i < _to.length; i++) {
         if(int(balances[_to[i]]) + amount[i] >= 0) {
             balances[_to[i]] = uint(int(balances[_to[i]]) + amount[i]);
             totalBalance = uint(int(totalBalance) + amount[i]);
             Reconcile(_to[i], amount[i]);
         } else {
             throw;
         }
        }
    }
  }

  function sweep(uint nonce, address _to, uint amount,
                  uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
     numSignatures = sigV.length;
     for(uint i = 0; i < numSignatures; i++) {
       signatures[i].sigV = sigV[i];
       signatures[i].sigR = sigR[i];
       signatures[i].sigS = sigS[i];
     }

     if(confirmAdminTx(nonce)) {
       if(_to.send(amount)) {
         Sweep(msg.sender, _to, amount);
       } else {
         throw;
       }
     }
   }

   function topUp() payable {
     TopUp(msg.sender, msg.value);
   }
}
