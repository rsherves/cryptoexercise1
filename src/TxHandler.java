import java.security.PublicKey;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TxHandler {

    private final UTXOPool utxoPool;

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
        return isValidTx(tx, new TxValidation());
    }

    private boolean isValidTx(Transaction tx, TxValidation validation) {
        return validation.isValid(tx);
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        Transaction[] acceptedTxs = findMaximalValidTxSet(possibleTxs);
        updateUtxoPool(acceptedTxs);
        return acceptedTxs;
    }

    private Transaction[] findMaximalValidTxSet(Transaction[] possibleTxs) {
        List<Transaction> unprocessed = new UniqueTxCollection(possibleTxs).list();
        List<Transaction> validTxs = new ArrayList<>();
        TxValidation validation = new TxValidation();
        int txsAdded;
        do {
            txsAdded = 0;
            ListIterator<Transaction> iterator = unprocessed.listIterator();
            while (iterator.hasNext()) {
                Transaction tx = iterator.next();
                if (validation.isValid(tx)) {
                    validation.process(tx);
                    validTxs.add(tx);
                    iterator.remove();
                    txsAdded++;
                }
            }
        } while (unprocessed.size() > 0 && txsAdded > 0);
        return toArray(validTxs);
    }

    private Transaction[] toArray (List<Transaction> txs) {
        if (txs == null || txs.isEmpty()) {
            return new Transaction[0];
        }
        Transaction[] result = new Transaction[txs.size()];
        for (int i=0; i<txs.size(); i++) {
            result[i] = txs.get(i);
        }
        return result;
    }

    private void updateUtxoPool(Transaction[] acceptedTxs) {
        if (acceptedTxs != null) {
            for (Transaction tx : acceptedTxs) {
                if (isValidTx(tx)) {
                    tx.getInputs().stream()
                            .map(i -> new UTXO(i.prevTxHash, i.outputIndex))
                            .forEach(utxoPool::removeUTXO);

                    List<Transaction.Output> outputs = tx.getOutputs();
                    for (int i=0; i<outputs.size(); i++) {
                        utxoPool.addUTXO(new UTXO(tx.getHash(), i), outputs.get(i));
                    }
                }
            }
        }
    }


    private class UniqueTxCollection {
        private final List<Transaction> transactions;

        private UniqueTxCollection(Transaction[] txs) {
            if (txs == null) {
                transactions = Collections.emptyList();
            } else {
                Map<byte[], Transaction> txsByHash = Arrays.stream(txs)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toMap(
                                Transaction::getHash,
                                Function.identity(),
                                (t1, t2) -> t1));
                transactions = Collections.unmodifiableList(new ArrayList<>(txsByHash.values()));
            }
        }

        private List<Transaction> list() {
            return new ArrayList<>(transactions);
        }

        private int size() {
            return transactions.size();
        }
    }


    private class TxValidation {
        private final UTXOPool validationUtxoPool;

        private TxValidation() {
            this.validationUtxoPool = (utxoPool == null) ? new UTXOPool() : new UTXOPool(utxoPool);
        }

        private boolean isValid(Transaction tx) {
            return tx != null
                    && hasValidInputs(tx)
                    && haveValidSignatures(tx)
                    && !hasDoubleSpend(tx.getInputs())
                    && !hasNegativeOutputValues(tx.getOutputs())
                    && !createsValue(tx);
        }

        private boolean hasValidInputs(Transaction tx) {
            return tx.getInputs().stream()
                    .allMatch(i -> i != null
                            && i.prevTxHash != null
                            && validationUtxoPool.contains(new UTXO(i.prevTxHash, i.outputIndex)));
        }

        private boolean haveValidSignatures(Transaction tx) {
            List<Transaction.Input> inputs = tx.getInputs();
            for (int i = 0; i < inputs.size(); i++) {
                Transaction.Input input = inputs.get(i);
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                if (!validationUtxoPool.contains(utxo)) {
                    return false;
                }
                Transaction.Output output = validationUtxoPool.getTxOutput(utxo);
                PublicKey publicKey = output.address;
                byte[] signature = input.signature;
                byte[] message = tx.getRawDataToSign(i);

                if (signature == null || !Crypto.verifySignature(publicKey, message, signature)) {
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
            return outputs.stream().anyMatch(o -> o.value < 0);
        }

        private boolean createsValue(Transaction tx) {
            double inputsValue = tx.getInputs().stream()
                    .map(i -> new UTXO(i.prevTxHash, i.outputIndex))
                    .filter(validationUtxoPool::contains)
                    .map(validationUtxoPool::getTxOutput)
                    .mapToDouble(output -> output.value)
                    .sum();
            double outputsValue = tx.getOutputs().stream()
                    .mapToDouble(output -> output.value)
                    .sum();
            return outputsValue > inputsValue;
        }

        private void process(Transaction tx) {
            if (isValid(tx)) {
                tx.getInputs().stream()
                        .map(i -> new UTXO(i.prevTxHash, i.outputIndex))
                        .forEach(validationUtxoPool::removeUTXO);

                List<Transaction.Output> outputs = tx.getOutputs();
                for (int i=0; i<outputs.size(); i++) {
                    validationUtxoPool.addUTXO(new UTXO(tx.getHash(), i), outputs.get(i));
                }
            }
        }
    }
}
