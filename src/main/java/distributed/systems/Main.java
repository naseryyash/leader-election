package distributed.systems;


import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;

import java.io.IOException;

/**
 * Caller main method for the distributed leader election algorithm.
 * Refer {@link distributed.systems.LeaderElection} for the
 * implementation of this algorithm.
 *
 * <p>Please refer the README.md for instructions on how to set
 * this project up.</p>
 *
 * @author Yash Nasery
 */
public class Main {
    public static void main(String[] args) {
        LeaderElection leaderElection = new LeaderElection();
        try {
            leaderElection.leaderElectionAlgorithmImpl();
        } catch (IOException | InterruptedException | KeeperException e) {
            throw new RuntimeException(e);
        }

    }
}