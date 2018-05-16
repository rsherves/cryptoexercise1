import java.security.PublicKey;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
        return tx != null
                && areUnspent(tx.getInputs())
                && haveValidSignatures(tx)
                && !hasDoubleSpend(tx.getInputs())
                && !hasNegativeOutputValues(tx.getOutputs())
                && !createsValue(tx);
    }

    private boolean areUnspent(List<Transaction.Input> inputs) {
        return inputs.stream()
                .map(i -> new UTXO(i.prevTxHash, i.outputIndex))
                .allMatch(u -> utxoPool.contains(u));
    }

    private boolean haveValidSignatures(Transaction tx) {
        List<Transaction.Input> inputs = tx.getInputs();
        for (int i = 0; i < inputs.size(); i++) {
            Transaction.Input input = inputs.get(i);
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            if (!utxoPool.contains(utxo)) {
                return false;
            }
            Transaction.Output output = utxoPool.getTxOutput(utxo);
            PublicKey publicKey = output.address;
            byte[] signature = input.signature;
            byte[] message = tx.getRawDataToSign(i);

            if (!Crypto.verifySignature(publicKey, message, signature)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasDoubleSpend(List<Transaction.Input> inputs) {
        Set<UTXO> uniqueInputs = inputs.stream()
                .map(i -> new UTXO(i.prevTxHash, i.outputIndex))
                .collect(Collectors.toSet());
        return inputs.size() > uniqueInputs.size();
    }

    private boolean hasNegativeOutputValues(List<Transaction.Output> outputs) {
        return outputs.stream().allMatch(o -> o.value >= 0);
    }

    private boolean createsValue(Transaction tx) {
        double inputsValue = tx.getInputs().stream()
                .map(i -> new UTXO(i.prevTxHash, i.outputIndex))
                .filter(utxo -> utxoPool.contains(utxo))
                .map(utxo -> utxoPool.getTxOutput(utxo))
                .mapToDouble(output -> output.value)
                .sum();
        double outputsValue = tx.getOutputs().stream()
                .mapToDouble(output -> output.value)
                .sum();
        return outputsValue > inputsValue;
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
