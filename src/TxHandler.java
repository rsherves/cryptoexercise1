import java.util.List;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = (utxoPool == null) ? new UTXOPool() : new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all inputs claimed by {@code tx} are in the current UTXO pool,
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        return tx != null
                && areUnspent(tx.getInputs())
                && haveValidSignatures(tx.getInputs())
                // && !hasDoubleSpend(tx.getInputs())
                // && !haveNegativeValues(tx.getOutputs())
                // && sumValue(tx.getInputs()) >= sumValue(tx.getOutputs())
                ;
    }

    private boolean areUnspent(List<Transaction.Input> inputs) {
        return inputs.stream()
                .map(i -> new UTXO(i.prevTxHash, i.outputIndex))
                .allMatch(u -> utxoPool.contains(u));
    }

    private boolean haveValidSignatures(List<Transaction.Input> inputs) {
        // TODO
        return false;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        return null;
    }

}
