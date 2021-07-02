package net.es.nsi.dds.dao;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 *
 * @author hacksaw
 */
public class RemoteSubscriptionCache {

    private final Logger log = LogManager.getLogger(getClass());
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
        return Collections.unmodifiableCollection(new ArrayList<>(remoteSubscriptions.values()));
    }

    public Set<String> keySet() {
        return Collections.unmodifiableSet(new ConcurrentSkipListSet<>(remoteSubscriptions.keySet()));
    }
}
