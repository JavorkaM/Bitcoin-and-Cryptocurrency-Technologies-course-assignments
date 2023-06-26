import java.util.ArrayList;

// Meno študenta: Marcell Jávorka
public class HandleTxs {

    UTXOPool tempUTXOPool;

    ArrayList<UTXO> blockInputsUTXOs = new ArrayList<>(); // array for checking if the inputs in the whole block have different UTXO pairs

    /**
     * Vytvorí verejný ledger (účtovnú knihu), ktorého aktuálny UTXOPool (zbierka nevyčerpaných
     * transakčných výstupov) je {@code utxoPool}. Malo by to vytvoriť bezpečnú kópiu
     * utxoPool pomocou konštruktora UTXOPool (UTXOPool uPool).
     */
    public HandleTxs(UTXOPool utxoPool) {
        // IMPLEMENTOVAŤ
        tempUTXOPool = new UTXOPool(utxoPool);
    }

    /**
     * @return aktuálny UTXO pool. 
     * Ak nenájde žiadny aktuálny UTXO pool, tak vráti prázdny (nie nulový) objekt {@code UTXOPool}.
     */
    public UTXOPool UTXOPoolGet() {
        // IMPLEMENTOVAŤ
        if(tempUTXOPool == null){
            return new UTXOPool();
        }
        return tempUTXOPool;
    }

    /**
     * @return true, ak 
     * (1) sú všetky nárokované výstupy {@code tx} v aktuálnom UTXO pool,           //DONE//
     * (2) podpisy na každom vstupe {@code tx} sú platné,                           //DONE//
     * (3) žiadne UTXO nie je nárokované viackrát,                                  //DONE//
     * (4) všetky výstupné hodnoty {@code tx}s sú nezáporné a                       //DONE//
     * (5) súčet vstupných hodnôt {@code tx}s je väčší alebo rovný súčtu jej        //DONE//
     *     výstupných hodnôt; a false inak.
     */
    public boolean txIsValid(Transaction tx) {
        // IMPLEMENTOVAŤ
        int totalOutput = 0;
        int totalInput = 0;

        for (Transaction.Output output: tx.getOutputs()){
            //(4)
            if( output.value < 0) {
                System.out.println("Output value was < 0!");
                System.out.println("Transaction invalid!");
                return false;
            }
            //(5)
            totalOutput += output.value;
        }
        int index = 0; // the index of inputs we are iterating over
        ArrayList<UTXO> transactionInputsUTXOs = new ArrayList<>(); // array for checking if the inputs in transaction have different UTXO pairs
        for (Transaction.Input input: tx.getInputs()){

            UTXO inputsUTXO = new UTXO(input.prevTxHash, input.outputIndex); //UTXO that belongs to this input

            //(5)
            Transaction.Output previousTxOutput = UTXOPoolGet().getTxOutput(inputsUTXO);
            totalInput += previousTxOutput.value;

            //(2)
            if(!previousTxOutput.address.verifySignature(tx.getDataToSign(index), input.signature)){
                System.out.println("Signature could not be verified!");
                System.out.println("Transaction invalid!");
                return false;
            }

            //(1)
            if(!tempUTXOPool.contains(inputsUTXO)){
                System.out.println("Input was not found in the UTXO pool!");
                System.out.println("Transaction invalid!");
                return false;
            }

            //(3a) žiadne UTXO nie je nárokované viackrát,
            if(transactionInputsUTXOs.contains(inputsUTXO)){
                System.out.println("Two inputs in one transaction seem to be using the same UTXO!");
                System.out.println("Transaction invalid!");
                return false;
            }
            transactionInputsUTXOs.add(inputsUTXO);

            //(3b)
            if(blockInputsUTXOs.contains(inputsUTXO)){
                System.out.println("Two inputs in block seem to be using the same UTXO!");
                System.out.println("Transaction invalid!");
                return false;
            }
            blockInputsUTXOs.add(inputsUTXO);
            index++;
        }

        //(5)
        if(totalOutput > totalInput){
            System.out.println("Total output value of tx is higher than the total input value!");
            System.out.println("Transaction invalid!");
            return false;
        }
        // If we have gone through all the hurdles, transaction should be valid
        return true;
    }

    /**
     * Spracováva každú epochu (iteráciu) prijímaním neusporiadaného radu navrhovaných
     * transakcií, kontroluje správnosť každej transakcie, vracia pole vzájomne 
     * platných prijatých transakcií a aktualizuje aktuálny UTXO pool podľa potreby.
     */
    public Transaction[] handler(Transaction[] possibleTxs) {
        // IMPLEMENTOVAŤ
        ArrayList<Transaction> validTransactionList = new ArrayList<>();
        blockInputsUTXOs = new ArrayList<>();
        for (Transaction transaction : possibleTxs){
            if(!txIsValid(transaction)){
                continue;
            }
            //**add to valid Transacions**
            validTransactionList.add(transaction);

            //**update UTXO**
            UTXOPool utxoPool = UTXOPoolGet();

            //remove inputs
            for(Transaction.Input input : transaction.getInputs()){
                utxoPool.removeUTXO(new UTXO(input.prevTxHash,input.outputIndex));
            }
            //add outputs
            int index = 0; // the index of outputs we are iterating over
            for (Transaction.Output output : transaction.getOutputs()){
                utxoPool.addUTXO(new UTXO(transaction.getHash(),index),output);
                index++;
            }
        }
        return validTransactionList.toArray(new Transaction[0]);
    }
}
