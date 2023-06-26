// Meno študenta:
import java.util.*;

/* TrustedNode označuje uzol, ktorý dodržuje pravidlá (nie je byzantský) */
public class TrustedNode implements Node {
    boolean[] followees;
    boolean[] trustedFollowees;
    double p_graph;
    double p_byzantine;
    double p_txDistribution;
    int numRounds;
    int currentRoundSend = 0;
    int currentRoundRecieve = 0;
    Set<Transaction> pendingTransactions;

    public TrustedNode(double p_graph, double p_byzantine, double p_txDistribution, int numRounds) {
        // IMPLEMENTOVAŤ
        this.p_graph = p_graph;
        this.p_byzantine = p_byzantine;
        this.p_txDistribution = p_txDistribution;
        this.numRounds = numRounds;
    }

    public void followeesSet(boolean[] followees) {
        // IMPLEMENTOVAŤ
        this.followees = followees;
        this.trustedFollowees = followees;
    }

    public void pendingTransactionSet(Set<Transaction> pendingTransactions) {
        // IMPLEMENTOVAŤ
        this.pendingTransactions = pendingTransactions;
    }

    public Set<Transaction> followersSend() {
        Set<Transaction> proposals = new HashSet<Transaction>(this.pendingTransactions);
        this.pendingTransactions.clear();
        return proposals;
    }

    public void followeesReceive(ArrayList<Integer[]> candidates) {
        // IMPLEMENTOVAŤ
        for (Integer[] candidate : candidates) {
            if (this.followees[candidate[1]] && !this.pendingTransactions.contains(candidate[0])) {
                this.pendingTransactions.add(new Transaction(candidate[0]));
            }
        }
        currentRoundRecieve++;
    }
}
