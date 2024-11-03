package distributed.systems;


import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;

import java.io.IOException;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        // Leader election part
        LeaderElection leaderElection = new LeaderElection();
        try {
            leaderElection.leaderElectionAlgorithmImpl();
        } catch (IOException | InterruptedException | KeeperException e) {
            throw new RuntimeException(e);
        }

        // Watcher for various events part
//        ZookeeperWatcher zookeeperWatcher = new ZookeeperWatcher();
//        try {
//            zookeeperWatcher.watcherImpl();
//        } catch (IOException | InterruptedException | KeeperException e) {
//            throw new RuntimeException(e);
//        }

    }
}