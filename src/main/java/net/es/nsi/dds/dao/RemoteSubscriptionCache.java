package net.es.nsi.dds.dao;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class RemoteSubscriptionCache {

    private final Logger log = LoggerFactory.getLogger(getClass());
    // In-memory subscription cache indexed by subscriptionId.
    private final Map<String, RemoteSubscription> remoteSubscriptions = new ConcurrentHashMap<>();

    public RemoteSubscriptionCache() {
        log.debug("RemoteSubscriptionCache: creating.");
    }

    public static RemoteSubscriptionCache getInstance() {
        return SpringApplicationContext.getBean("remoteSubscriptionCache", RemoteSubscriptionCache.class);
    }

    public RemoteSubscription get(String url) {
        return remoteSubscriptions.get(url);
    }

    public RemoteSubscription add(RemoteSubscription subscription) {
        return remoteSubscriptions.put(subscription.getDdsURL(), subscription);
    }

    public RemoteSubscription remove(String url) {
        return remoteSubscriptions.remove(url);
    }

    public Collection<RemoteSubscription> values() {
        return remoteSubscriptions.values();
    }

    public Set<String> keySet() {
        return remoteSubscriptions.keySet();
    }
}
