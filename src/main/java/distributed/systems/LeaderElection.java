package distributed.systems;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class LeaderElection implements Watcher {

    private ZooKeeper zookeeper;

    private String currentZnodeName;

    private static final String ELECTION_NAMESPACE = "/election";

    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";

    private static final int ZOOKEEPER_SERVER_TIMEOUT = 3000;

    public void leaderElectionAlgorithmImpl() throws IOException, InterruptedException, KeeperException {
        connectToZookeper();
        volunteerForLeadership();
        reelectLeader();
        run();
        close();
        System.out.println("Disconnected from zookeeper, exiting application...");
    }

    private void volunteerForLeadership() throws InterruptedException, KeeperException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        String znodeFullPath = zookeeper.create(znodePrefix, new byte[] {},
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("znode name " + znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    private void reelectLeader() throws InterruptedException, KeeperException {
        Stat predecessorStat = null;
        String predecessorZnodeName = "";

        // This while loop ensures that either the node is elected a leader,
        // or that it has a predecessor to watch. This is to prevent the race
        // condition where the predecessor node dies between the binary search
        // to find its name and registering a watcher on it.
        while (null == predecessorStat) {
            List<String> children = zookeeper.getChildren(ELECTION_NAMESPACE, false);
            Collections.sort(children);

            String smallestChild = children.get(0);

            if (smallestChild.equals(this.currentZnodeName)) {
                System.out.println("I am the leader!");
                return;
            }

            System.out.println("I am not the leader!");
            int predecessorIndex = Collections.binarySearch(children, currentZnodeName) - 1;
            predecessorZnodeName = children.get(predecessorIndex);
            predecessorStat = zookeeper.exists(ELECTION_NAMESPACE + "/" + predecessorZnodeName, this);
        }

        System.out.println("Watching znode " + predecessorZnodeName);
        System.out.println();
    }

    private void connectToZookeper() throws IOException {
        this.zookeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, ZOOKEEPER_SERVER_TIMEOUT, this);
    }

    private void run() throws InterruptedException {
        synchronized (zookeeper) {
            zookeeper.wait();
        }
    }

    private void close() throws InterruptedException {
        zookeeper.close();
    }

    @Override
    public void process(WatchedEvent watchedEvent) {
        switch (watchedEvent.getType()) {
            case None:
                if (watchedEvent.getState() == Event.KeeperState.SyncConnected) {
                    System.out.println("Successfully connected to zookeeper");
                } else {
                    synchronized (zookeeper) {
                        System.out.println("Disconnected from zookeeper event");
                        zookeeper.notifyAll();
                    }
                }
                break;
            case NodeDeleted:
                try {
                    reelectLeader();
                } catch (InterruptedException | KeeperException ignored) {
                }
        }
    }
}
