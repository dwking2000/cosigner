contract multisig {
    address private signer0 = 0xDEADBEEF0;
    address private signer1 = 0xDEADBEEF1;
    address private signer2 = 0xDEADBEEF2;
    
    uint public counter = 0;
    address public lastSigner = 0;
    address public closingLastSinger = 0;
    address public pendingClosingDestination = 0;
    address public pendingTransactionDestination = 0;
    uint public pendingTransactionAmount = 0;

    function sendAmount(uint newCounterValue, 
                        address destination,
                        uint amount) 
    public returns (bool) {
        if ((msg.sender == signer0 || 
             msg.sender == signer1 ||
             msg.sender == signer2) &&
            address(this).balance >= amount) {
            // If nothing pending, 
            // make a new pending transaction
            if (counter + 1 == newCounterValue &&
                lastSigner == 0 &&
                pendingTransactionDestination == 0 &&
                pendingTransactionAmount == 0) {
                counter = newCounterValue;
                pendingTransactionDestination = destination;
                pendingTransactionAmount = amount;
                lastSigner = msg.sender;
                return true;
            }
            // If pending transaction, send and clear
            if (counter == newCounterValue &&
                lastSigner != msg.sender &&
                pendingTransactionDestination == destination &&
                pendingTransactionAmount == amount) {
                pendingTransactionDestination.send(pendingTransactionAmount);
                pendingTransactionDestination = 0;
                pendingTransactionAmount = 0;
                lastSigner = 0;
                return true;
            }    
            return false;
        }
        return false;
    }
  // TODO: Cancel Send Amount
    
  function closeWallet(uint newCounterValue, 
                       address destination)
    public returns (bool) {
        if (msg.sender == signer0 || 
            msg.sender == signer1 ||
            msg.sender == signer2) {
            if (counter + 1 == newCounterValue &&
                closingLastSinger == 0 &&
                pendingClosingDestination == 0) {
                counter = newCounterValue;
                pendingClosingDestination = destination;
                closingLastSinger = msg.sender;
                return true;
            }
            if (counter == newCounterValue &&
                closingLastSinger != msg.sender &&
                pendingClosingDestination == destination) {
                suicide(pendingClosingDestination);
                return true;
            }
        }
        return false;
    }
  // TODO: Cancel closeWallet
}