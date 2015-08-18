// New msig contract -- simplified version of https://github.com/ethereum/meteor-dapp-wallet/blob/master/wallet.sol
//                      we don't need something as flexible as that.
contract multiowned {

    // the number of owners that must confirm the same operation before it is run.
    uint m_required;
    uint m_numOwners;
    // list of owners
    uint[16] m_owners;
    PendingState pending;

    // struct for the status of a pending operation.
    struct PendingState {
        bytes32 hash;
        uint owners; 
      	uint yetNeeded;           
    }

    // simple single-sig function modifier.
    modifier onlyowner {
        if (isOwner(tx.origin))
            _
    }
    // multi-sig function modifier: the operation must have an intrinsic hash in order
    // that later attempts can be realised as the same underlying operation and
    // thus count as confirmations.
    modifier onlymanyowners(bytes32 _operation) {
        if (confirmAndCheck(_operation))
            _
    }

    // constructor is given number of sigs required to do protected "onlymanyowners" transactions
    // as well as the selection of addresses capable of confirming them.
    function multiowned(address[] _owners, uint _required) {
        for (uint i = 0; i < _owners.length; i++)
        {
            m_owners[i] = uint(_owners[i]);
        }
        m_numOwners = _owners.length;
        m_required = _required;
    }
    
    function isOwner(address _addr) returns (bool) {
        for(uint i = 0; i < m_numOwners; i++) {
          if(m_owners[i] == uint(_addr)) return true;
        }
        
        return false;
    }

    function confirmAndCheck(bytes32 _operation) internal returns (bool) {
        // determine what index the present sender is:
        uint ownerIndex = 255;
        for(uint i = 0; i < m_numOwners; i++) {
          if(m_owners[i] == uint(tx.origin)) {
            ownerIndex = i;
            break;
          }
        }
        
        // make sure they're an owner
        if (ownerIndex == 255) return;

        if (pending.hash != _operation) {
            // reset count of confirmations needed.
            pending.yetNeeded = m_required;
            // reset which owners have confirmed (none) - set our bitmap to 0.
            pending.owners = 0;
            pending.hash = _operation;
        }
        // determine the bit to set for this owner.
        uint ownerIndexBit = 2**ownerIndex;
        // make sure we (the message sender) haven't confirmed this operation previously.
        if (pending.owners & ownerIndexBit == 0) {
            // ok - check if count is enough to go ahead.
            if (pending.yetNeeded <= 1) {
                // enough confirmations: reset and run interior.
                pending.yetNeeded = 0;
                pending.owners = 0; 
                pending.hash = 0;
                return true;
            }
            else
            {
                // not enough: record that this owner in particular confirmed.
                pending.yetNeeded--;
                pending.owners |= ownerIndexBit;
            }
        }
    }
}

// interface contract for multisig proxy contracts; see below for docs.
contract multisig {    
    function execute(address _to, uint _value) external returns (bytes32);
}

// usage:
// bytes32 h = Wallet(w).from(oneOwner).transact(to, value, data);
// Wallet(w).from(anotherOwner).confirm(h);
contract Wallet is multisig, multiowned {
    // pending transactions we have at present.
    Transaction tx;

    // Transaction structure to remember details of transaction lest it need be saved for a later call.
    struct Transaction {
        address to;
        uint value;
        bytes32 hash;
    }

    // constructor - just pass on the owner array to the multiowned and
    // the limit to daylimit
    function Wallet(address[] _owners, uint _required)
            multiowned(_owners, _required) {
    }
    
    // kills the contract sending everything to `_to`.
    function kill(address _to) onlymanyowners(sha3(msg.data)) external {
        suicide(_to);
    }
    
    // Outside-visible transact entry point. Executes transacion immediately if below daily spend limit.
    // If not, goes into multisig process. We provide a hash on return to allow the sender to provide
    // shortcuts for the other confirmations (allowing them to avoid replicating the _to, _value
    // and _data arguments). They still get the option of using them if they want, anyways.
    function execute(address _to, uint _value) external onlyowner returns (bytes32 _r) {
        // determine our operation hash.
        _r = sha3(msg.data);
        if(tx.hash != _r) {
          tx.to = 0;
          tx.value = 0;
          tx.hash = _r;
        }
        
        if (!confirm(_r) && tx.to == 0) {
            tx.to = _to;
            tx.value = _value;
        }
    }
    
    // confirm a transaction through just the hash. we use the previous transactions map, m_txs, in order
    // to determine the body of the transaction from the hash provided.
    function confirm(bytes32 _h) internal onlymanyowners(_h) returns (bool) {
        if (tx.to != 0) {
            tx.to.send(tx.value);
            tx.to = 0;
            tx.value = 0;
            tx.hash = 0;
            return true;
        }
    }
}
