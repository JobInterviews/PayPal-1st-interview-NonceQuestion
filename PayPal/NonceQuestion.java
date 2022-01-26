package nonce;

import java.util.UUID;

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
        /**
         * I'm invoked when a user decides to schedule a transaction to be pushed to the blockchain
         * @param tx - The Transaction the user wishes to push to the blockchain
         */
        public void scheduleTransaction(Transaction tx) {
            // Implement
        }

        /**
         * I'm invoked when the blockchain mines the transaction
         * @param tx - The transaction that was mined by the blockchain
         */
        public void onTransactionConfirmedByBlockchain(Transaction tx) {
            // Implement
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
