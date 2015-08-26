// When signing the message, take the to/amount/nonce, sha3 and sign this
// Append this data as a signature object in the data block
// Sign the whole message for submission
// Contract takes fees from the sender
// Contract verifies each signature against the provided to/amount/nonce
// If there are enough signatures, send the money
contract multisigwallet {
    // the number of owners that must confirm the same operation before it is run.
    uint m_required;

    // list of owners
    uint m_numOwners;
    uint[8] m_owners;
    uint lastNonce;
    
    uint signers;
    uint numSigners;
    address signer;
        
    Transaction transaction;
    bytes32 transactionHash;
    SigData[] signatures;    
    
    // Transaction and signature structures
    struct Transaction {
        address to;
        uint value;
        uint nonce;
    }
    
    struct SigData { 
      uint8 sigV;
      bytes32 sigR;
      bytes32 sigS;
    }      
    
    function confirmTransaction() internal returns (bool) {
        transactionHash = sha3(transaction.to, transaction.value, transaction.nonce);        
        
        if(transaction.nonce <= lastNonce)
          return false;
        
        signers = 0;
        numSigners = 0;
        for (uint i = 0; i < signatures.length; i++) {
          signer = ecrecover(transactionHash, signatures[i].sigV, signatures[i].sigR, signatures[i].sigS);
          if(isOwner(signer) && (signers & 2**ownerIndexBit(signer) == 0)) {
            signers |= 2**ownerIndexBit(signer);
            numSigners++;
          }
        }
        
        if(numSigners >= m_required) return true;
        
        return false;
    }
    
    function isOwner(address _addr) internal returns (bool) {
        for(uint i = 0; i < m_numOwners; i++) {
          if(m_owners[i] == uint(_addr)) return true;
        }
        
        return false;
    }
    
    function ownerIndexBit(address _addr) internal returns (uint) {
      for(uint i = 0; i < m_numOwners; i++) {
          if(m_owners[i] == uint(_addr))            
            return i;
        }
    }

    // constructor is given number of sigs required to do protected "onlymanyowners" transactions
    // as well as the selection of addresses capable of confirming them.
    function multisigwallet(address[] _owners, uint _required) {
        lastNonce = 0;
        for (uint i = 0; i < _owners.length; i++)
        {
            m_owners[i] = uint(_owners[i]);
        }
        m_numOwners = _owners.length;
        m_required = _required;
    }
   
    // kills the contract sending everything to `_to`.
    function kill(address to, uint value, uint nonce, 
                   uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
        transaction.to = to;
        transaction.value = value;
        transaction.nonce = nonce;      
        
        delete(signatures);                  
        for(uint i = 0; i < sigV.length; i++) {
          signatures.length++;
          signatures[i].sigV = sigV[i];         
        }
        for(i = 0; i < sigR.length; i++) {
          signatures[i].sigR = sigR[i];         
        }
        for(i = 0; i < sigS.length; i++) {
          signatures[i].sigS = sigS[i];         
        }
        
        
        if(confirmTransaction()) {
          lastNonce = transaction.nonce;
          //delete(signatures);
          suicide(transaction.to);
         }
    }
    
    function execute(address to, uint value, uint nonce, 
                      uint8[] sigV, bytes32[] sigR, bytes32[] sigS) external {
        transaction.to = to;
        transaction.value = value;
        transaction.nonce = nonce;
        
        delete(signatures);                  
        for(uint i = 0; i < sigV.length; i++) {
          signatures.length++;
          signatures[i].sigV = sigV[i];         
        }
        for(i = 0; i < sigR.length; i++) {
          signatures[i].sigR = sigR[i];         
        }
        for(i = 0; i < sigS.length; i++) {
          signatures[i].sigS = sigS[i];         
        }
        
        if(confirmTransaction()) {
          lastNonce = transaction.nonce;
          //delete(signatures);
          transaction.to.send(value);
        }
    }
    
    // deposit
    function () {
    }
}
