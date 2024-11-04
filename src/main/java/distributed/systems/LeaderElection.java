package distributed.systems;
import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * <p>Class represents a basic implementation of a distributed
 * leader election algorithm. It uses Apache Zookeeper
 * ({@link org.apache.zookeeper}) to deal with distributed
 * co-ordination between different nodes.</p>
 *
 * <p>It provides the following functionality.
 * <ol>
 * <li>Elect a leader among a group of nodes running this process.</li>
 * <li>Re-elect a new leader if the current leader goes down. </li>
 * </ol>
 * </p>
 *
 * <p>The {@link org.apache.zookeeper.Watcher} interface is implemented
 * for the algorithm to keep track of various events in Zookeeeper
 * like Connect / Disconnect to zookeeper and Node deletion. Here
 * Node deletion in Zookeeper corresponds to a rela Node / Process going
 * down.</p>
 *
 * <p>Please refer the README.md for detailed instructions on how
 * to test this implementation.</p>
 *
 * @author Yash Nasery.
 */
public class LeaderElection implements Watcher {

    /**
     * The zookeeper instance.
     */
    private ZooKeeper zookeeper;

    /**
     * The string based name of the current node
     * running the process.
     */
    private String currentZnodeName;

    /**
     * The root znode for our leader election project.
     * This needs to be manually created in the zookeeper.
     * Refer the README.md for further instructions.
     */
    private static final String ELECTION_NAMESPACE = "/election";

    /**
     * The address and port on which zookeeper runs.
     */
    private static final String ZOOKEEPER_ADDRESS = "localhost:2181";

    /**
     * Zookeeper session timeout in milliseconds.
     */
    private static final int ZOOKEEPER_SERVER_TIMEOUT = 3000;

    /**
     * Method provides the implementation for the leader election /
     * re-election of the leader from a group of nodes.
     *
     * @throws IOException {@inheritDoc}
     * @throws InterruptedException {@inheritDoc}
     * @throws KeeperException {@inheritDoc}
     */
    public void leaderElectionAlgorithmImpl() throws IOException, InterruptedException, KeeperException {
        connectToZookeper();
        volunteerForLeadership();
        reelectLeader();
        run();
        close();
        System.out.println("Disconnected from zookeeper, exiting application...");
    }

    /**
     * Method provides implementation for the node to volunteer
     * for leadership in a cluster. By default, each node
     * volunteers for leadership on startup and registers itself
     * with Zookeeper.
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws KeeperException {@inheritDoc}
     */
    private void volunteerForLeadership() throws InterruptedException, KeeperException {
        String znodePrefix = ELECTION_NAMESPACE + "/c_";
        String znodeFullPath = zookeeper.create(znodePrefix, new byte[] {},
                ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);

        System.out.println("znode name " + znodeFullPath);
        this.currentZnodeName = znodeFullPath.replace(ELECTION_NAMESPACE + "/", "");
    }

    /**
     * <p>Method provides implementation to elect / re-elect a
     * leader among a cluster of nodes.</p>
     *
     * <p>Each node watches some previously registered node in Zookeeper.
     * If the node is the first to be registered, it is elected
     * a leader.</p>
     *
     * <p>If node A is watching node B, and node B goes down, node A
     * will automatically choose a new previous node to watch. If
     * node B was the leader, node A will now be the new leader.</p>
     *
     *
     * @throws InterruptedException {@inheritDoc}
     * @throws KeeperException {@inheritDoc}
     */
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

    /**
     * Method provides implementation to connect to Zookeeper.
     *
     * @throws IOException {@inheritDoc}
     */
    private void connectToZookeper() throws IOException {
        this.zookeeper = new ZooKeeper(ZOOKEEPER_ADDRESS, ZOOKEEPER_SERVER_TIMEOUT, this);
    }

    /**
     * Since Zookeeper event thread runs separately. We need to
     * keep the main thread waiting for Zookeeper to be able
     * to notify our application process of any events.
     *
     * <p>This is critical, as we are dependent on events like connection,
     * disconnection, and node deletion for the leader election
     * algorithm to work properly.</p>
     *
     * @throws InterruptedException {@inheritDoc}
     */
    private void run() throws InterruptedException {
        synchronized (zookeeper) {
            zookeeper.wait();
        }
    }

    /**
     * Gracefully closes the resources open on Zookeeper
     * before the application shuts down.
     *
     * @throws InterruptedException {@inheritDoc}
     */
    private void close() throws InterruptedException {
        zookeeper.close();
    }

    /**
     * Processing logic for events like connection, disconnection
     * and node deletion from Zookeeper.
     *
     * @param watchedEvent The event from Zookeeper being processed.
     */
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
