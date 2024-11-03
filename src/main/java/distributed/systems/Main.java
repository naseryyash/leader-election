package distributed.systems;


import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
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