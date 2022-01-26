package com.paypal.nonce;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class NonceQuestion {

    public static class Transaction {
        private final UUID transactionId;
        private final UUID sourceWalletId;
        private final UnsignedLong nonce;

        public Transaction(UUID transactionId, UUID sourceWalletId, UnsignedLong nonce) {
            this.transactionId = transactionId;
            this.sourceWalletId = sourceWalletId;
            this.nonce = nonce;
        }

        /**
         * I'm the global unique identifier of the transaction across the system.
         * @return Global unique identifier of the transaction
         */
        public UUID getTransactionId() {
            return transactionId;
        }

        /**
         * I'm the global unique identifier of the source wallet across the system.
         * @return Global unique identifier of the source wallet
         */
        public UUID getSourceWalletId() {
            return sourceWalletId;
        }

        /**
         * Nonce determines the order transactions from the same sourceWallet are pushed to the blockchain.
         * Transaction with nonce 0 is pushed first, then nonce 1, etc.
         * Two transactions can have the same nonce if and only if they belong to different wallets.
         * @return The transaction nonce
         */
        public UnsignedLong getNonce() {
            return nonce;
        }
    }

    public static abstract class PayPalPusher {

        private Map<UUID, TransactionsFollow> walletTransactions = new ConcurrentHashMap<UUID, TransactionsFollow>();


        private static class TransactionsFollow {
            UnsignedLong nextNonce;
            PriorityQueue queue = new PriorityQueue( );


            TransactionsFollow(UnsignedLong nonce){
                this.nextNonce = nonce;
            }

            void add(Transaction tx){
                queue.add(tx);
            }

            Transaction peek(){
                return (Transaction) queue.peek();
            }

            Transaction getNext(){
                return (Transaction) queue.poll();
            }
        }

        /**
         * I'm invoked when a user decides to schedule a transaction to be pushed to the blockchain
         * @param tx - The Transaction the user wishes to push to the blockchain
         */
        public void scheduleTransaction(Transaction tx) {

            //1. if not exist-> put with current nonce + publish
            if(!walletTransactions.containsKey(tx.getSourceWalletId())){
                if(firstTransaction(tx.getNonce())) {
                    walletTransactions.put(tx.sourceWalletId, new TransactionsFollow(tx.getNonce()));
                    pushTransaction(tx);
                } else{
                    TransactionsFollow tf = new TransactionsFollow(0);
                    tf.add(tx);
                    walletTransactions.put(tx.sourceWalletId, tf);
                }
            } else {
                //2. if exist -> validate nonce, if true -> publish, else -> add to queue
                TransactionsFollow tf = walletTransactions.get(tx.getSourceWalletId());
                if(validNonce(tx.getNonce(), tf.nextNonce)){
                    setNextNonce(tx, tf);
                    pushTransaction(tx);
                } else {
                    tf.add(tx);
                }
            }

            // Implement
        }

        private boolean firstTransaction(UnsignedLong nonce) {
            return nonce == 0;
        }

        protected boolean validNonce(UnsignedLong nextNonce, UnsignedLong nonce){
            return nonce == nextNonce;
        }

        /**
         * I'm invoked when the blockchain mines the transaction
         * @param tx - The transaction that was mined by the blockchain
         */
        public void onTransactionConfirmedByBlockchain(Transaction tx) {
            // Implement
            TransactionsFollow tf = walletTransactions.get(tx.getSourceWalletId());
            if(tf != null){
                Transaction t = tf.peek();
                if (t != null) {
                    if(validNonce(tf.nextNonce, t.getNonce())){
                        tx = tf.getNext();
                        setNextNonce(tf.getNext(), tf);
                        pushTransaction(tx);
                    }
                }
            }
        }

        private void setNextNonce(Transaction tx, TransactionsFollow tf) {
            tf.nextNonce = tx.getNonce() + 1;
        }

        /**
         * If I'm invoked with transaction that its nonce violates the correct order
         * under the sourceWallet I throw TransactionNonceOutOfOrderException
         * @param tx - The transaction to push to the blockchain
         * @throws TransactionNonceOutOfOrderException
         */
        protected abstract void pushTransaction(Transaction tx);

    }

}