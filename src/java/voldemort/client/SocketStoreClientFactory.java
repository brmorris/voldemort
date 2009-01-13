package voldemort.client;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import voldemort.cluster.Node;
import voldemort.serialization.DefaultSerializerFactory;
import voldemort.serialization.SerializerFactory;
import voldemort.store.Store;
import voldemort.store.socket.SocketPool;
import voldemort.store.socket.SocketStore;

import com.google.common.base.Objects;

/**
 * @author jay
 * 
 */
public class SocketStoreClientFactory extends AbstractStoreClientFactory {

    public static final String URL_SCHEME = "tcp";
    public static final int DEFAULT_SOCKET_TIMEOUT_MS = 5000;
    public static final int DEFAULT_NUM_THREADS = 5;
    public static final int DEFAULT_MAX_QUEUED_REQUESTS = 1000;
    public static final int DEFAULT_MAX_CONNECTIONS_PER_NODE = 10;
    public static final int DEFAULT_MAX_CONNECTIONS = 50;

    private SocketPool socketPool;

    public SocketStoreClientFactory(String... bootstrapUrls) {
        this(DEFAULT_NUM_THREADS,
             DEFAULT_NUM_THREADS,
             DEFAULT_MAX_QUEUED_REQUESTS,
             DEFAULT_MAX_CONNECTIONS_PER_NODE,
             DEFAULT_MAX_CONNECTIONS,
             bootstrapUrls);
    }

    public SocketStoreClientFactory(int coreThreads,
                                    int maxThreads,
                                    int maxQueuedRequests,
                                    int maxConnectionsPerNode,
                                    int maxTotalConnections,
                                    String... bootstrapUrls) {
        this(coreThreads,
             maxThreads,
             maxQueuedRequests,
             maxConnectionsPerNode,
             maxTotalConnections,
             DEFAULT_SOCKET_TIMEOUT_MS,
             AbstractStoreClientFactory.DEFAULT_ROUTING_TIMEOUT_MS,
             bootstrapUrls);
    }

    public SocketStoreClientFactory(int coreThreads,
                                    int maxThreads,
                                    int maxQueuedRequests,
                                    int maxConnectionsPerNode,
                                    int maxTotalConnections,
                                    int socketTimeoutMs,
                                    int routingTimeoutMs,
                                    String... bootstrapUrls) {
        this(new ThreadPoolExecutor(coreThreads,
                                    maxThreads,
                                    10000L,
                                    TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>(maxQueuedRequests),
                                    Executors.defaultThreadFactory(),
                                    new ThreadPoolExecutor.CallerRunsPolicy()),
             maxConnectionsPerNode,
             maxTotalConnections,
             socketTimeoutMs,
             routingTimeoutMs,
             AbstractStoreClientFactory.DEFAULT_NODE_BANNAGE_MS,
             new DefaultSerializerFactory(),
             bootstrapUrls);
    }

    public SocketStoreClientFactory(ExecutorService service,
                                    int maxConnectionsPerNode,
                                    int maxTotalConnections,
                                    int socketTimeoutMs,
                                    int routingTimeoutMs,
                                    int defaultNodeBannageMs,
                                    String... bootstrapUrls) {
        this(service,
             maxConnectionsPerNode,
             maxTotalConnections,
             socketTimeoutMs,
             routingTimeoutMs,
             defaultNodeBannageMs,
             new DefaultSerializerFactory(),
             bootstrapUrls);
    }

    public SocketStoreClientFactory(ExecutorService service,
                                    int maxConnectionsPerNode,
                                    int maxTotalConnections,
                                    int socketTimeoutMs,
                                    int routingTimeoutMs,
                                    int defaultNodeBannageMs,
                                    SerializerFactory serializerFactory,
                                    String... boostrapUrls) {
        super(service, serializerFactory, routingTimeoutMs, defaultNodeBannageMs, boostrapUrls);
        this.socketPool = new SocketPool(maxConnectionsPerNode,
                                         maxTotalConnections,
                                         socketTimeoutMs);
    }

    @Override
    protected Store<byte[], byte[]> getStore(String storeName, String host, int port) {
        return new SocketStore(Objects.nonNull(storeName), Objects.nonNull(host), port, socketPool);
    }

    @Override
    protected int getPort(Node node) {
        return node.getSocketPort();
    }

    @Override
    protected void validateUrl(URI url) {
        if (!URL_SCHEME.equals(url.getScheme()))
            throw new IllegalArgumentException("Illegal scheme in bootstrap URL for SocketStoreClientFactory:"
                    + " expected '" + URL_SCHEME + "' but found '" + url.getScheme() + "'.");
    }

}