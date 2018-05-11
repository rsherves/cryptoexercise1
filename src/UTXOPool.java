import java.util.*;

public class UTXOPool {

    /**
     * The current collection of UTXOs, with each one mapped to its corresponding transaction output
     */
    private Map<UTXO, Transaction.Output> pool;

    /** Creates a new empty UTXOPool */
    public UTXOPool() {
        pool = new HashMap<UTXO, Transaction.Output>();
    }

    /** Creates a new UTXOPool that is a copy of {@code uPool} */
    public UTXOPool(UTXOPool uPool) {
        pool = new HashMap<UTXO, Transaction.Output>(uPool.pool);
    }

    /** Adds a mapping from UTXO {@code utxo} to transaction output @code{txOut} to the pool */
    public void addUTXO(UTXO utxo, Transaction.Output txOut) {
        pool.put(utxo, txOut);
    }

    /** Removes the UTXO {@code utxo} from the pool */
    public void removeUTXO(UTXO utxo) {
        pool.remove(utxo);
    }

    /**
     * @return the transaction output corresponding to UTXO {@code utxo}, or null if {@code utxo} is not in the pool.
     */
    public Transaction.Output getTxOutput(UTXO ut) {
        return pool.get(ut);
    }

    /** @return true if UTXO {@code utxo} is in the pool and false otherwise */
    public boolean contains(UTXO utxo) {
        return pool.containsKey(utxo);
    }

    /** Returns an {@code ArrayList} of all UTXOs in the pool */
    public List<UTXO> getAllUTXO() {
        Set<UTXO> setUTXO = pool.keySet();
        List<UTXO> allUTXO = new ArrayList<UTXO>();
        for (UTXO ut : setUTXO) {
            allUTXO.add(ut);
        }
        return allUTXO;
    }
}
