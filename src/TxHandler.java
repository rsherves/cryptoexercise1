import java.security.PublicKey;
import java.util.*;
import java.util.function.Function;
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
        Transaction[] acceptedTxs = findLargestValidTxSet(possibleTxs);
        updateUtxoPool(acceptedTxs);
        return acceptedTxs;
    }

    private Transaction[] findLargestValidTxSet(Transaction[] possibleTxs) {
        TxCollection txs = new TxCollection(possibleTxs).withoutDuplicates();

        return null; //TODO
    }

    private void updateUtxoPool(Transaction[] acceptedTxs) {
        //TODO
    }


    private class TxCollection {
        private List<Transaction> transactions = new ArrayList<>();
        private List<List<Transaction>> permutations;

        private TxCollection(Transaction[] txs) {
            if (txs != null) {
                transactions.addAll(Arrays.asList(txs));
            }
            permutations(new ArrayList<Transaction>(transactions), 0, Collections.emptyList());
        }

        private TxCollection withoutDuplicates() {
            Map<byte[], Transaction> txsByHash = transactions.stream().collect(Collectors.toMap(
                    Transaction::getHash,
                    Function.identity(),
                    (t1, t2) -> t1));
            transactions = new ArrayList<>(txsByHash.values());
            permutations(new ArrayList<Transaction>(transactions), 0, Collections.emptyList());
            return this;
        }

        private void permutations(List<Transaction> txs, int index, List<Transaction> permutation) {
            if (index < txs.size()) {
                for (Transaction t : minus(txs, permutation)) {
                    permutations(txs, index + 1, union(permutation, t));
                }
            } else {
                permutations.add(permutation);
            }
        }

        private List<Transaction> minus(List<Transaction> set, List<Transaction> subset) {
            List<Transaction> result = new ArrayList<>(set);
            for (Transaction t : subset) {
                result.remove(t);
            }
            return result;
        }

        private List<Transaction> union(List<Transaction> txs, Transaction t) {
            List<Transaction> result = new ArrayList<>(txs);
            result.add(t);
            return result;
        }

        private List<Transaction> list() {
            return transactions;
        }

        private List<List<Transaction>> permutations() {
            return permutations;
        }
    }
}
