// Meno študenta: Marcell Jávorka
// Blockchain by mal na prevádzku funkcií udržiavať iba obmedzené množstvo uzlov
// Nemali by ste mať všetky bloky pridané do blockchainu v pamäti  
// pretože by to mohlo spôsobiť pretečenie pamäte.

import java.util.ArrayList;


//TODO honestly dont know what to do with the txPool (transactionPool)

public class Blockchain {

    // všetky potrebné informácie na spracovanie bloku v reťazi blokov
    private class BlockNode {
        //Blockchain Node info
        public Block b;
        public BlockNode parent;
        public ArrayList<BlockNode> children;
        public int height;
        // utxo pool na vytvorenie nového bloku na vrchu tohto bloku
        private UTXOPool utxoPool;
        private TransactionPool txPool;

        public BlockNode(Block b, BlockNode parent, UTXOPool uPool, TransactionPool txPool) {
            this.b = b;
            this.parent = parent;
            children = new ArrayList<BlockNode>();
            this.utxoPool = uPool;
            this.txPool = txPool;
            if (parent != null) {
                height = parent.height + 1;
                parent.children.add(this);
            } else {
                height = 1;
            }
        }
        public UTXOPool getUTXOPoolCopy() {
            return new UTXOPool(utxoPool);
         }

         public TransactionPool getTransactionPool() {
            return this.txPool;
        }
    }

    // Blockchain info
    public static final int CUT_OFF_AGE = 5;

    private ArrayList<BlockNode> _blockchain = new ArrayList<>();
    private TransactionPool _txPool = new TransactionPool();
    private BlockNode _maxHeightBlock;
    /**
     * vytvor prázdny blockchain iba s prvým (Genesis) blokom. Predpokladajme, že
     * {@code genesisBlock} je platný blok
     */

    public Blockchain(Block genesisBlock) {
        // IMPLEMENTOVAŤ

        UTXOPool utxoPool = new UTXOPool();
        for (Transaction tx : genesisBlock.getTransactions()){
            if (tx == null) // TODO needed ? question mark idk
                break;
            //getTransactionPool().addTransaction(tx); // NO
            int i = 0;
            for(Transaction.Output output : tx.getOutputs()) {
                //add outputs as UTXO
                UTXO utxo = new UTXO(tx.getHash(), i);
                utxoPool.addUTXO(utxo,output);
                i++;
            }
        }
        addCoinbaseOutputsToUTXO(genesisBlock, utxoPool);

        BlockNode genesisNode = new BlockNode(genesisBlock, null, utxoPool, getTransactionPool());
        _maxHeightBlock = genesisNode;
        _blockchain.add(genesisNode);
    }

    /** Získaj najvyšší (maximum height) blok */
    public Block getBlockAtMaxHeight() {
        // IMPLEMENTOVAŤ
        if(_maxHeightBlock == null)
            return null;
        return _maxHeightBlock.b;

    }

    /** Získaj UTXOPool na ťaženie nového bloku na vrchu najvyššieho (max height) bloku */
    public UTXOPool getUTXOPoolAtMaxHeight() {
        // IMPLEMENTOVAŤ
        if(_maxHeightBlock == null)
            return null;
        return _maxHeightBlock.getUTXOPoolCopy();
    }

    /** Získaj pool transakcií na vyťaženie nového bloku */
    public TransactionPool getTransactionPool() {
        // IMPLEMENTOVAŤ
        if (_txPool == null)
            return new TransactionPool();
        return _txPool;
    }

    /**
     * Pridaj {@code block} do blockchainu, ak je platný. Kvôli platnosti by mali
     * byť všetky transakcie platné a blok by mal byť na
     * {@code height > (maxHeight - CUT_OFF_AGE)}.
     * Môžete napríklad vyskúšať vytvoriť nový blok nad blokom Genesis (výška bloku
     * 2), ak height blockchainu je {@code <=
     * CUT_OFF_AGE + 1}. Len čo {@code height > CUT_OFF_AGE + 1}, nemôžete vytvoriť
     * nový blok vo výške 2.
     *
     * @return true, ak je blok úspešne pridaný
     */
    public boolean blockAdd(Block block) {
        // IMPLEMENTOVAŤ
        BlockNode parent = getParentNode(block.getPrevBlockHash());
        if (parent == null)
            return false;
        if(parent.height < _maxHeightBlock.height - CUT_OFF_AGE + 1)
            return false;

        HandleTxs handler = new HandleTxs(parent.getUTXOPoolCopy());
        TransactionPool newTxPool = new TransactionPool(parent.getTransactionPool()); // inherits parents TxPool

        UTXOPool newUTXOPool = handler.UTXOPoolGet();
        //Coinbase to UTXO
        addCoinbaseOutputsToUTXO(block,newUTXOPool);

        /*TODO      what do I remove what do I add....
                    I should probably remove, but then where am I adding them in?
                    cant see any new txs added in anywhere                          */
        for(Transaction tx : handler.handler(block.getTransactions().toArray(new Transaction[0]) ) ){
            //newTxPool.addTransaction(tx);               //TODO remove or add ???
            newTxPool.removeTransaction(tx.getHash());  //TODO remove or add ???
        }



        BlockNode newBlockNode = new BlockNode(block, parent, newUTXOPool, newTxPool);
        // parent.children.add(newBlockNode); this is already done in constructor
        if(parent.height >= _maxHeightBlock.height){
            _txPool = newTxPool;                                                //TODO not sure about this
            _maxHeightBlock = newBlockNode;
        }else{
            for (int i = 0; i < newTxPool.getTransactions().size(); i++) {
                //_txPool.addTransaction(newTxPool.getTransactions().get(i));                 //TODO remove or add ???
                _txPool.removeTransaction(newTxPool.getTransactions().get(i).getHash());    //TODO remove or add ???
            }
        }
        return _blockchain.add(newBlockNode);
    }

    /** Pridaj transakciu do transakčného poolu */
    public void transactionAdd(Transaction tx) {
        // IMPLEMENTOVAŤ
        for (BlockNode block : _blockchain){
            block.txPool.addTransaction(tx);
        }
        _txPool.addTransaction(tx);
    }

    public BlockNode getParentNode(byte[] blockHash) {
        ByteArrayWrapper ogBlockHashWrap = new ByteArrayWrapper(blockHash);
        for (BlockNode blocknode : _blockchain) {
            ByteArrayWrapper parentBlockHashWrap = new ByteArrayWrapper(blocknode.b.getHash());
            if (ogBlockHashWrap.equals(parentBlockHashWrap)) {
                return blocknode;
            }
        }
        return null;
    }

    public UTXOPool addCoinbaseOutputsToUTXO(Block block, UTXOPool utxoPool){
        Transaction coinbase = block.getCoinbase();

        int i = 0;
        for (Transaction.Output coinbaseTxout : coinbase.getOutputs()){ //can be more than one ?
                                                                        // maybe no
                                                                        // this works either way
            UTXO coinbaseUTXO = new UTXO(block.getCoinbase().getHash(), i );
            if(utxoPool.contains(coinbaseUTXO)){
                System.out.println("---------------------WARNING--------------------------");
                System.out.println("---------This Coinbase UTXO already exists------------");

            }
            utxoPool.addUTXO(coinbaseUTXO,coinbaseTxout);
            i++;
        }

        return utxoPool;
    }
}