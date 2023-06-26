import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SignatureException;


public class Main {

    public static void main(String[] args) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeyException, SignatureException {

        byte[] key_bob = new byte[32];
        byte[] key_alice = new byte[32];
        byte[] key_cyril = new byte[32];

        for (int i = 0; i < 32; i++) {
            key_bob[i] = (byte) 1;
            key_alice [i] = (byte) 0;
            key_cyril [i] = (byte) 2;
        }

        PRGen prGen_bob = new PRGen(key_bob);
        PRGen prGen_alice = new PRGen(key_alice);
        PRGen prGen_cyril = new PRGen(key_cyril);

        RSAKeyPair pk_bob = new RSAKeyPair(prGen_bob, 265);
        RSAKeyPair pk_alice = new RSAKeyPair(prGen_alice, 265);
        RSAKeyPair pk_cyril = new RSAKeyPair(prGen_cyril, 265);

        Block genesisBlock = new Block(null, pk_bob.getPublicKey());
        genesisBlock.finalize();
        Blockchain blockchain = new Blockchain(genesisBlock);
        HandleBlocks handleBlocks = new HandleBlocks(blockchain);

        printSeparator();

        System.out.println("****Testing Adding first block and multiple inputs****");
        System.out.println("----Expecting: Valid block----");


        Block block1 = new Block(genesisBlock.getHash(), pk_alice.getPublicKey());
        Tx tx1 = new Tx();
        tx1.addInput(genesisBlock.getCoinbase().getHash(), 0);

        tx1.addOutput(2, pk_alice.getPublicKey());
        tx1.addOutput(2, pk_alice.getPublicKey());
        tx1.addOutput(2.25, pk_alice.getPublicKey());

        tx1.signTx(pk_bob.getPrivateKey(), 0);

        block1.transactionAdd(tx1);
        // blockchain.transactionAdd(tx1); TODO should this be here?
        block1.finalize();

        System.out.println("Is Block1 Valid?: " + handleBlocks.blockProcess(block1));

        printSeparator();

        System.out.println("****Testing Adding new branch****");
        System.out.println("----Expecting: Valid block----");
        Block block2 = new Block(genesisBlock.getHash(), pk_bob.getPublicKey());

        Tx tx2 = new Tx();

        // genesis block ma hodnotu 6.25
        tx2.addInput(genesisBlock.getCoinbase().getHash(), 0);

        tx2.addOutput(2, pk_bob.getPublicKey());
        tx2.addOutput(2, pk_bob.getPublicKey());
        tx2.addOutput(2.25, pk_bob.getPublicKey());

        tx2.signTx(pk_bob.getPrivateKey(), 0);

        block2.transactionAdd(tx2);
        block2.finalize();

        System.out.println("Is Block2 Valid?: " + handleBlocks.blockProcess(block2));

        printSeparator();

        System.out.println("****Testing Extending one branch and multiple inputs****");
        System.out.println("----Expecting: Valid block----");
        Block block3 = new Block(block1.getHash(), pk_bob.getPublicKey());

        Tx tx3 = new Tx();

        tx3.addInput(tx1.getHash(), 0); //value: 2
        tx3.addInput(tx1.getHash(), 1); //value: 2

        tx3.addOutput(4, pk_cyril.getPublicKey());

        tx3.signTx(pk_alice.getPrivateKey(), 0);
        tx3.signTx(pk_alice.getPrivateKey(), 1);

        block3.transactionAdd(tx3);
        block3.finalize();

        System.out.println("Is Block3 Valid?: " + handleBlocks.blockProcess(block3));

        printSeparator();

        System.out.println("****Testing Extending branch for testing CUT_OFF_AGE****");
        System.out.println("----Expecting: Valid block----");
        Block block4 = new Block(block3.getHash(), pk_bob.getPublicKey());

        Tx tx4 = new Tx();

        tx4.addInput(tx3.getHash(), 0);

        tx4.addOutput(3, pk_bob.getPublicKey());
        tx4.addOutput(1, pk_bob.getPublicKey());

        tx4.signTx(pk_cyril.getPrivateKey(), 0);

        block4.transactionAdd(tx4);
        block4.finalize();

        System.out.println("Is Block4 Valid?: " + handleBlocks.blockProcess(block4));

        printSeparator();

        System.out.println("****Testing Extending branch for testing CUT_OFF_AGE****");
        System.out.println("----Expecting: Valid block----");
        Block block5 = new Block(block4.getHash(), pk_bob.getPublicKey());

        Tx tx5 = new Tx();

        tx5.addInput(tx4.getHash(), 0);

        tx5.addOutput(3, pk_alice.getPublicKey());

        tx5.signTx(pk_bob.getPrivateKey(), 0);

        block5.transactionAdd(tx5);
        block5.finalize();



        System.out.println("Is Block5 Valid?: " + handleBlocks.blockProcess(block5));

        printSeparator();

        System.out.println("****Testing two txs in one block****");
        System.out.println("----Expecting: Valid block----");

        Block block6 = new Block(block5.getHash(), pk_bob.getPublicKey());
        Tx tx6 = new Tx();
        tx6.addInput(tx5.getHash(), 0);
        tx6.addOutput(3, pk_bob.getPublicKey());
        tx6.signTx(pk_alice.getPrivateKey(), 0);
        block6.transactionAdd(tx6);


        Tx tx7 = new Tx();
        tx7.addInput(tx4.getHash(), 1);
        tx7.addOutput(1, pk_cyril.getPublicKey());
        tx7.signTx(pk_bob.getPrivateKey(), 0);
        block6.transactionAdd(tx7);


        block6.finalize();
        System.out.println("Is Block6 Valid?: " + handleBlocks.blockProcess(block6));

        printSeparator();

        System.out.println("****Testing double spending from a previous block****");
        System.out.println("----Expecting: Not found in UTXOPool----");
        Block block7 = new Block(block6.getHash(), pk_bob.getPublicKey());
        Tx tx8 = new Tx();
        tx8.addInput(tx6.getHash(), 0);
        tx8.addOutput(3, pk_bob.getPublicKey());
        tx8.signTx(pk_bob.getPrivateKey(), 0);
        block7.transactionAdd(tx8);


        Tx tx9 = new Tx();
        tx9.addInput(tx4.getHash(), 1);
        tx9.addOutput(1, pk_cyril.getPublicKey());
        tx9.signTx(pk_bob.getPrivateKey(), 0);
        block7.transactionAdd(tx9);

        block7.finalize();
        System.out.println("Is Block7 Valid?: " + handleBlocks.blockProcess(block7));

        printSeparator();

        System.out.println("****Testing double spending in same block****");
        System.out.println("----Expecting: Not found in UTXOPool----");
        Block block8 = new Block(block5.getHash(), pk_bob.getPublicKey());

        Tx tx10 = new Tx();
        tx10.addInput(tx4.getHash(), 1);
        tx10.addOutput(1, pk_cyril.getPublicKey());
        tx10.signTx(pk_bob.getPrivateKey(), 0);
        block8.transactionAdd(tx10);

        Tx tx11 = new Tx();
        tx11.addInput(tx4.getHash(), 1);
        tx11.addOutput(1, pk_cyril.getPublicKey());
        tx11.signTx(pk_bob.getPrivateKey(), 0);
        block8.transactionAdd(tx11);

        block8.finalize();
        System.out.println("Is Block8 Valid?: " + handleBlocks.blockProcess(block8));

        printSeparator();

        System.out.println("****Testing adding block outside of cut_off (if it is set to 5)****");
        System.out.println("----Expecting: Invalid Block----");
        Block block9 = new Block(genesisBlock.getHash(), pk_bob.getPublicKey());

        Tx tx12 = new Tx();
        tx12.addInput(tx4.getHash(), 1);
        tx12.addOutput(1, pk_cyril.getPublicKey());
        tx12.signTx(pk_bob.getPrivateKey(), 0);
        block9.transactionAdd(tx12);

        block9.finalize();
        System.out.println("Is Block9 Valid?: " + handleBlocks.blockProcess(block9));
    }


    public static class Tx extends Transaction {
        public void signTx(RSAKey sk, int input) throws SignatureException {
            byte[] sig = null;
            try {
                sig = sk.sign(this.getDataToSign(input));
            } catch (NullPointerException e) {
                throw new RuntimeException(e);
            }
            this.addSignature(sig, input);
            // Poznámka: táto funkcia je nevhodne pomenovaná a v skutočnosti
            // by nemala overridovať metódu finalize Java garbage kolektoru.
            this.finalize();
        }
    }

    public static void printSeparator(){
        System.out.println("");
        System.out.println("=============================================================");
        System.out.println("");

    }
}