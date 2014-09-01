package net.es.nsi.dds.dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.api.jaxb.PeerURLType;
import net.es.nsi.dds.api.jaxb.DiscoveryConfigurationType;
import net.es.nsi.dds.management.logs.DdsErrors;
import net.es.nsi.dds.management.logs.DdsLogger;
import net.es.nsi.dds.spring.SpringApplicationContext;

/**
 *
 * @author hacksaw
 */
public class DiscoveryConfiguration {
    private final DdsLogger pceLogger = DdsLogger.getLogger();

    public static final long MAX_AUDIT_INTERVAL = 86400L; // 24 hours in seconds
    public static final long DEFAULT_AUDIT_INTERVAL = 1200L; // 20 minutes in seconds
    public static final long MIN_AUDIT_INTERVAL = 300L; // 5 mins in seconds

    public static final long EXPIRE_INTERVAL_MAX = 2592000L; // 30 days in seconds
    public static final long EXPIRE_INTERVAL_DEFAULT = 86400L; // 24 hours in seconds
    public static final long EXPIRE_INTERVAL_MIN = 600L; // 10 minutes in seconds

    public static final int ACTORPOOL_MAX_SIZE = 100;
    public static final int ACTORPOOL_DEFAULT_SIZE = 20;
    public static final int ACTORPOOL_MIN_SIZE = 5;

    private String filename = null;

    private long lastModified = 0;
    private String nsaId = null;
    private String baseURL = null;
    private String documents = null;
    private String cache = null;
    private String repository = null;
    private long auditInterval = DEFAULT_AUDIT_INTERVAL;
    private long expiryInterval = EXPIRE_INTERVAL_DEFAULT;
    private int actorPool = ACTORPOOL_DEFAULT_SIZE;
    private Set<PeerURLType> discoveryURL = new HashSet<>();

    public static DiscoveryConfiguration getInstance() {
        DiscoveryConfiguration configurationReader = SpringApplicationContext.getBean("discoveryConfiguration", DiscoveryConfiguration.class);
        return configurationReader;
    }

    /**
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public synchronized void load() throws IllegalArgumentException, JAXBException, IOException, NullPointerException {
        // Make sure the condifuration file is set.
        if (getFilename() == null || getFilename().isEmpty()) {
            pceLogger.errorAudit(DdsErrors.DISCOVERY_CONFIGURATION_INVALID_FILENAME, "filename", getFilename());
            throw new IllegalArgumentException();
        }

        File file = null;
        try {
            file = new File(getFilename());
        }
        catch (NullPointerException ex) {
            pceLogger.errorAudit(DdsErrors.DISCOVERY_CONFIGURATION_INVALID_FILENAME, "filename", getFilename());
            throw ex;
        }

        long lastMod = file.lastModified();

        // If file was not modified since out last load then return.
        if (lastMod <= lastModified) {
            return;
        }

        DiscoveryConfigurationType config;

        try {
            config = DiscoveryParser.getInstance().parse(getFilename());
        }
        catch (IOException io) {
            pceLogger.errorAudit(DdsErrors.DISCOVERY_CONFIGURATION_INVALID_FILENAME, "filename", getFilename());
            throw io;
        }
        catch (JAXBException jaxb) {
            pceLogger.errorAudit(DdsErrors.DISCOVERY_CONFIGURATION_INVALID_XML, "filename", getFilename());
            throw jaxb;
        }

        if (config.getNsaId() == null || config.getNsaId().isEmpty()) {
            pceLogger.errorAudit(DdsErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "nsaId", config.getNsaId());
            throw new FileNotFoundException("Invalid nsaId: " + config.getNsaId());
        }

        setNsaId(config.getNsaId());

        if (config.getBaseURL() == null || config.getBaseURL().isEmpty()) {
            pceLogger.errorAudit(DdsErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "baseURL", config.getBaseURL());
            throw new FileNotFoundException("Invalid baseURL: " + config.getBaseURL());
        }

        setBaseURL(config.getBaseURL());

        // The DocumentCache will created the directoy if not present.
        if (config.getDocuments() != null && !config.getDocuments().isEmpty()) {
            setDocuments(config.getDocuments());
        }

        // The DocumentCache will created the directoy if not present.
        if (config.getCache() != null && !config.getCache().isEmpty()) {
            setCache(config.getCache());
        }

        // The RepositoryCache will created the directoy if not present.
        if (config.getRepository() != null && !config.getRepository().isEmpty()) {
            setRepository(config.getRepository());
        }

        if (config.getAuditInterval() < MIN_AUDIT_INTERVAL || config.getAuditInterval() > MAX_AUDIT_INTERVAL) {
            pceLogger.errorAudit(DdsErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "auditInterval", Long.toString(config.getAuditInterval()));
            setAuditInterval(DEFAULT_AUDIT_INTERVAL);
        }

        setAuditInterval(config.getAuditInterval());

        if (config.getExpiryInterval() < EXPIRE_INTERVAL_MIN || config.getExpiryInterval() > EXPIRE_INTERVAL_MAX) {
            pceLogger.errorAudit(DdsErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "expiryInterval", Long.toString(config.getExpiryInterval()));
            setExpiryInterval(EXPIRE_INTERVAL_DEFAULT);
        }

        setExpiryInterval(config.getExpiryInterval());

        if (config.getActorPool() < ACTORPOOL_MIN_SIZE || config.getActorPool() > ACTORPOOL_MAX_SIZE) {
            pceLogger.errorAudit(DdsErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "actorPool", Integer.toString(config.getActorPool()));
            setActorPool(ACTORPOOL_DEFAULT_SIZE);
        }

        setActorPool(config.getActorPool());

        if (config.getBaseURL() == null || config.getBaseURL().isEmpty()) {
            pceLogger.errorAudit(DdsErrors.DISCOVERY_CONFIGURATION_INVALID_PARAMETER, "baseURL=" + config.getBaseURL());
            throw new FileNotFoundException("Invalid base URL: " + config.getBaseURL());
        }

        for (PeerURLType url : config.getPeerURL()) {
            discoveryURL.add(url);
        }

        lastModified = lastMod;
    }

    public boolean isDocumentsConfigured() {
        if (documents == null || documents.isEmpty()) {
            return false;
        }

        return true;
    }

    public boolean isCacheConfigured() {
        if (cache == null || cache.isEmpty()) {
            return false;
        }

        return true;
    }

    public boolean isRepositoryConfigured() {
        if (repository == null || repository.isEmpty()) {
            return false;
        }

        return true;
    }

    /**
     * @return the auditInterval
     */
    public long getAuditInterval() {
        return auditInterval;
    }

    /**
     * @param auditInterval the auditInterval to set
     */
    public void setAuditInterval(long auditInterval) {
        this.auditInterval = auditInterval;
    }

    /**
     * @return the expiryInterval
     */
    public long getExpiryInterval() {
        return expiryInterval;
    }

    /**
     * @param expiryInterval the expiryInterval to set
     */
    public void setExpiryInterval(long expiryInterval) {
        this.expiryInterval = expiryInterval;
    }

    /**
     * @return the discoveryURL
     */
    public Set<PeerURLType> getDiscoveryURL() {
        return Collections.unmodifiableSet(discoveryURL);
    }

    /**
     * @return the agentPool
     */
    public int getActorPool() {
        return actorPool;
    }

    /**
     * @param agentPool the agentPool to set
     */
    public void setActorPool(int agentPool) {
        this.actorPool = agentPool;
    }

    /**
     * @return the nsaId
     */
    public String getNsaId() {
        return nsaId;
    }

    /**
     * @param nsaId the nsaId to set
     */
    public void setNsaId(String nsaId) {
        this.nsaId = nsaId;
    }

    /**
     * @return the documents
     */
    public String getDocuments() {
        return documents;
    }

    /**
     * @param documents the documents to set
     */
    public void setDocuments(String documents) {
        this.documents = documents;
    }

    /**
     * @return the cache
     */
    public String getCache() {
        return cache;
    }

    /**
     * @param cache the cache to set
     */
    public void setCache(String cache) {
        this.cache = cache;
    }

    /**
     * @return the baseURL
     */
    public String getBaseURL() {
        return baseURL;
    }

    /**
     * @param baseURL the baseURL to set
     */
    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    /**
     * @return the repository
     */
    public String getRepository() {
        return repository;
    }

    /**
     * @param repository the repository to set
     */
    public void setRepository(String repository) {
        this.repository = repository;
    }
}
