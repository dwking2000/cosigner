pragma soldity ^0.4.2;

contract TokenContract {
  address owner;

  mapping(address => uint) balances;
  uint totalBalance;

  mapping(address => mapping(address => uint)) allowances;

  event Transfer(address indexed _from, address indexed _to, uint256 _value);
  event TransferOnBehalf(address indexed _sender, address indexed _from, address indexed _to, uint256 _value);
  event Approval(address indexed _owner, address indexed _spender, uint256 _value);

  function getOwner() public returns (address) {
    return owner;
  }

  function allowance(address _owner, address _spender) constant returns (uint256 remaining) {
    return allowances[_owner][_spender];
  }

  function balanceOf(address _owner) constant returns (uint256 balance) {
    return balances[_owner];
  }

  function totalSupply() constant returns (uint256 supply) {
    return totalBalance;
  }

  function TokenContract(address _owner) {
    owner = _owner;
  }

  function updateOwner(address _owner) {
    if(msg.sender == owner) {
        owner = _owner;
    }
  }

  function approve(address _spender, uint256 _value) returns (bool success) {
    allowances[msg.sender][_spender] = _value;
    Approval(msg.sender, _spender, _value);
    return true;
  }

  function transfer(address _to, uint256 _value) returns (bool success) {
    if(_to == owner) {
        return false;
    }

    if(balances[msg.sender] >= _value || msg.sender == owner) {
        if(msg.sender != owner) {
            balances[msg.sender] -= _value;
        } else {
            totalBalance += _value;
        }
        balances[_to] += _value;
        Transfer(msg.sender, _to, _value);
        return true;
    } else {
        return false;
    }
  }

  function transferFrom(address _from, address _to, uint256 _value) returns (bool success) {
    if(_to == owner) {
        return false;
    }

    if(balances[_from] >= _value && allowances[_from][msg.sender] >= _value) {
        allowances[_from][msg.sender] -= _value;
        balances[_from] -= _value;
        balances[_to] += _value;
        Transfer(_from, _to, _value);
        TransferOnBehalf(msg.sender, _from, _to, _value);
        return true;
    } else {
        return false;
    }
  }

  function transferOnBehalf(address _from, address _to, uint256 _value) returns (bool success) {
    if(_to == owner && balances[msg.sender] >= _value) {
        TokenAdminContract parent = TokenAdminContract(owner);
        if(parent.deposit(msg.sender, _from, _value)) {
            balances[msg.sender] -= _value;
            totalBalance -= _value;
            Transfer(_from, _to, _value);
            TransferOnBehalf(msg.sender, _from, _to, _value);
            return true;
        }
    } else {
        if(balances[msg.sender] >= _value) {
            balances[msg.sender] -= _value;
            balances[_to] += _value;
            Transfer(_from, _to, _value);
            TransferOnBehalf(msg.sender, _from, _to, _value);
            return true;
        } else {
            return false;
        }
    }
  }

  function () {
    throw;
  }
}

contract TokenAdminContract {
  uint m_required;
  uint lastNonce;

  uint signers;
  uint numSigners;

  uint m_numOwners;
  address admin;
  uint adminSigned;
  mapping(address => uint) ownerIndex;
  mapping(uint => address) owners;
  address tokenContract;

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

  address[] vestingAddresses;
  mapping(address => VestingSchedule[]) vestingData;

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

  struct VestingSchedule {
      uint256 startTime;
      uint256 totalAmount;
      uint256 totalTime;
      bool prorated;
      uint256 lastUpdated;
      uint256 issuedToDate;
  }

  event Transfer(address indexed _from, address indexed _to, uint256 _value);
  event Deposit(address indexed _from, address indexed _to, uint256 _value);
  event Reconcile(address indexed _affected, int256 _value);
  event Issuance(uint256 _value);
  event Retirement(uint256 _value);
  event VestingCalculated();

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

  function TokenAdminContract(address _admin, address[] _owners, uint _required) {
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

  function deleteContract(uint nonce, address newAdminContract, uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
    // Must be signed by everyone and the admin
    numSignatures = sigV.length;
    for(uint i = 0; i < numSignatures; i++) {
      signatures[i].sigV = sigV[i];
      signatures[i].sigR = sigR[i];
      signatures[i].sigS = sigS[i];
    }
    if(confirmAdminTx(nonce) == true) {
        TokenContract child = TokenContract(tokenContract);
        child.updateOwner(newAdminContract);
        suicide(admin);
    }
  }

  function deposit(address _from, address _to, uint256 _value) external returns (bool) {
      if(msg.sender != tokenContract) {
        return false;
      } else {
        balances[_to] += _value;
        totalBalance += _value;
        Deposit(_from, _to, _value);
        return true;
      }
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
      TokenContract child = TokenContract(tokenContract);
      for(i = 0; i < numTransactions; i++) {
        if(balances[sender] >= transactions[i].value) {
          if(child.transfer(transactions[i].to, transactions[i].value)) {
              balances[sender] -= transactions[i].value;
              totalBalance -= transactions[i].value;
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

  function setTokenContract(uint nonce, address _child,
                        uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
      numSignatures = sigV.length;
      for(uint i = 0; i < numSignatures; i++) {
        signatures[i].sigV = sigV[i];
        signatures[i].sigR = sigR[i];
        signatures[i].sigS = sigS[i];
      }
      if(confirmAdminTx(nonce)) {
          tokenContract = _child;
      }
  }

  function issueTokens(uint nonce, address _to, uint amount,
                        uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
    numSignatures = sigV.length;
    for(uint i = 0; i < numSignatures; i++) {
      signatures[i].sigV = sigV[i];
      signatures[i].sigR = sigR[i];
      signatures[i].sigS = sigS[i];
    }

    if(confirmAdminTx(nonce)) {
        TokenContract child = TokenContract(tokenContract);
        if(child.transfer(_to, amount)) {
            Issuance(amount);
        } else {
            throw;
        }
    }
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

  function destroyTokens(uint nonce, address _from, uint amount,
                        uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {

    numSignatures = sigV.length;
    for(uint i = 0; i < numSignatures; i++) {
      signatures[i].sigV = sigV[i];
      signatures[i].sigR = sigR[i];
      signatures[i].sigS = sigS[i];
    }

    if(confirmAdminTx(nonce) && balances[_from] >= amount) {
      balances[_from] -= amount;
      totalBalance -= amount;
      Retirement(amount);
    } else {
      throw;
    }
  }

  function scheduleVesting(uint nonce, address _to, uint _amount, uint _timeFrame, bool _prorated,
                        uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
    numSignatures = sigV.length;
    for(uint i = 0; i < numSignatures; i++) {
      signatures[i].sigV = sigV[i];
      signatures[i].sigR = sigR[i];
      signatures[i].sigS = sigS[i];
    }

    if(confirmAdminTx(nonce)) {
        VestingSchedule memory schedule;
        schedule.startTime = block.timestamp;
        schedule.totalAmount = _amount;
        schedule.totalTime = _timeFrame;
        schedule.prorated = _prorated;
        schedule.lastUpdated = block.timestamp;
        schedule.issuedToDate = 0;

        VestingSchedule[] existingSchedules = vestingData[_to];
        if(existingSchedules.length == 0) {
          vestingAddresses.length++;
          vestingAddresses[vestingAddresses.length-1] = _to;
        }
        existingSchedules.length += 1;
        existingSchedules[existingSchedules.length-1] = schedule;
        vestingData[_to] = existingSchedules;
    }
  }

  function cancelVesting(uint nonce, address _to, uint _index,
      uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
      numSignatures = sigV.length;
      for(uint i = 0; i < numSignatures; i++) {
        signatures[i].sigV = sigV[i];
        signatures[i].sigR = sigR[i];
        signatures[i].sigS = sigS[i];
      }

      if(confirmAdminTx(nonce)) {
        VestingSchedule[] existingSchedules = vestingData[_to];
        VestingSchedule schedule = existingSchedules[_index];
        schedule.totalAmount = schedule.issuedToDate;
        schedule.lastUpdated = block.timestamp;
        existingSchedules[_index] = schedule;
        vestingData[_to] = existingSchedules;
      }
  }

  function calculateVesting(uint nonce,
                        uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
    numSignatures = sigV.length;
    for(uint i = 0; i < numSignatures; i++) {
      signatures[i].sigV = sigV[i];
      signatures[i].sigR = sigR[i];
      signatures[i].sigS = sigS[i];
    }

    if(confirmAdminTx(nonce)) {
        TokenContract child = TokenContract(tokenContract);
        for(i = 0; i < vestingAddresses.length; i++) {
            VestingSchedule[] existingSchedules = vestingData[vestingAddresses[i]];
            for(uint j = 0; j < existingSchedules.length; j++) {
                VestingSchedule schedule = existingSchedules[j];
                if(schedule.issuedToDate < schedule.totalAmount) {
                    uint percentFilled = ((block.timestamp - schedule.startTime) * 100) / schedule.totalTime;
                    if(schedule.prorated || percentFilled >= 100) {
                        uint amountDue = ((schedule.totalAmount * percentFilled) / 100) - schedule.issuedToDate;
                        if(amountDue >= (schedule.totalAmount - schedule.issuedToDate) || percentFilled >= 100) {
                            amountDue = schedule.totalAmount - schedule.issuedToDate;
                        }
                        if(child.transfer(vestingAddresses[i], amountDue)) {
                            schedule.issuedToDate += amountDue;
                            Issuance(amountDue);
                        } else {
                            throw;
                        }
                    }
                    schedule.lastUpdated = block.timestamp;
                    existingSchedules[j] = schedule;
                }
            }
            vestingData[vestingAddresses[i]] = existingSchedules;
        }
        VestingCalculated();
    }
  }

  function listVestees() constant returns (address[]) {
    return vestingAddresses;
  }

  function getScheduleCount(address _vestee) constant returns (uint256) {
      return vestingData[_vestee].length;
  }

  function listVestingSchedule(address _vestee, uint _scheduleIndex) constant returns (uint256, uint256, uint256,
                                                                              bool, uint256, uint256) {
      VestingSchedule schedule = vestingData[_vestee][_scheduleIndex];
      return (schedule.startTime, schedule.totalAmount, schedule.totalTime,
              schedule.prorated , schedule.lastUpdated, schedule.issuedToDate);
  }
}

contract TokenStorageContract {
  uint m_required;
  uint lastNonce;

  uint signers;
  uint numSigners;

  uint m_numOwners;
  address admin;
  uint adminSigned;
  mapping(address => uint) ownerIndex;
  mapping(uint => address) owners;
  address tokenContract;

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

  function TokenStorageContract(address _tokenContract, address _admin, address[] _owners, uint _required) {
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
    tokenContract = _tokenContract;
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
        TokenContract child = TokenContract(tokenContract);
        child.transfer(admin, child.balanceOf(this));
        suicide(admin);
    }
  }

  function deposit(address _to, uint256 _value) external returns (bool) {
      TokenContract child = TokenContract(tokenContract);
      if(child.transferFrom(msg.sender, this, _value)) {
        balances[_to] += _value;
        totalBalance += _value;
        Deposit(msg.sender, _to, _value);
        return true;
      } else {
        return false;
      }
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
      TokenContract child = TokenContract(tokenContract);
      for(i = 0; i < numTransactions; i++) {
        if(balances[sender] >= transactions[i].value) {
          if(child.transfer(transactions[i].to, transactions[i].value)) {
              balances[sender] -= transactions[i].value;
              totalBalance -= transactions[i].value;
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

  function setTokenContract(uint nonce, address _child,
                        uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
      numSignatures = sigV.length;
      for(uint i = 0; i < numSignatures; i++) {
        signatures[i].sigV = sigV[i];
        signatures[i].sigR = sigR[i];
        signatures[i].sigS = sigS[i];
      }
      if(confirmAdminTx(nonce)) {
          tokenContract = _child;
      }
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
         TokenContract child = TokenContract(tokenContract);
         if(child.transfer(_to, amount)) {
           Sweep(msg.sender, _to, amount);
         } else {
           throw;
         }
       }
     }
}
