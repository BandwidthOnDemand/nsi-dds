package net.es.nsi.dds.dao;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.authorization.AccessControlList;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.config.http.HttpConfig;
import net.es.nsi.dds.config.http.HttpsConfig;
import net.es.nsi.dds.jaxb.ConfigurationParser;
import net.es.nsi.dds.jaxb.configuration.AccessControlType;
import net.es.nsi.dds.jaxb.configuration.DdsConfigurationType;
import net.es.nsi.dds.jaxb.configuration.ObjectFactory;
import net.es.nsi.dds.jaxb.configuration.PeerURLType;
import net.es.nsi.dds.jaxb.configuration.SignatureStoreType;
import net.es.nsi.dds.jaxb.management.LogType;
import net.es.nsi.dds.management.logs.DdsErrors;
import net.es.nsi.dds.management.logs.DdsLogger;
import net.es.nsi.dds.signing.KeyStoreHandler;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
public class DdsConfiguration {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DdsLogger ddsLogger = DdsLogger.getLogger();
    private final ObjectFactory factory = new ObjectFactory();

    public static final long MAX_AUDIT_INTERVAL = 86400L; // 24 hours in seconds
    public static final long DEFAULT_AUDIT_INTERVAL = 1200L; // 20 minutes in seconds
    public static final long MIN_AUDIT_INTERVAL = 300L; // 5 mins in seconds

    public static final long EXPIRE_INTERVAL_MAX = 2592000L; // 30 days in seconds
    public static final long EXPIRE_INTERVAL_DEFAULT = 86400L; // 24 hours in seconds
    public static final long EXPIRE_INTERVAL_MIN = 600L; // 10 minutes in seconds

    public static final int ACTORPOOL_MAX_SIZE = 100;
    public static final int ACTORPOOL_DEFAULT_SIZE = 20;
    public static final int ACTORPOOL_MIN_SIZE = 5;

    public static final int NOTIFICATIONSIZE_MAX_SIZE = 40;
    public static final int NOTIFICATIONSIZE_DEFAULT_SIZE = 10;
    public static final int NOTIFICATIONSIZE_MIN_SIZE = 1;

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
    private int notificationSize;
    private HttpConfig httpConfig = null;
    private Optional<HttpsConfig> clientConfig = Optional.empty();
    private AccessControlList accessControlList;
    private Map<String, PeerURLType> discoveryURL = new HashMap<>();

    // Configuration for digital signing of documents populated through Gof3 agents.
    private boolean sign = false;
    private KeyStoreHandler signingStore = null;
    private String signingAlias = null;

    public static DdsConfiguration getInstance() {
        DdsConfiguration configurationReader = SpringApplicationContext.getBean("ddsConfiguration", DdsConfiguration.class);
        return configurationReader;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public synchronized void load() throws IllegalArgumentException, JAXBException, IOException, FileNotFoundException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        LogType errorAudit;

        // Make sure the condifuration file is set.
        if (getFilename() == null || getFilename().isEmpty()) {
            errorAudit = ddsLogger.errorAudit(DdsErrors.DDS_CONFIGURATION_INVALID_FILENAME, "filename", getFilename());
            throw new IllegalArgumentException(ddsLogger.logToString(errorAudit));
        }

        File file = null;
        try {
            file = new File(getFilename());
        }
        catch (NullPointerException ex) {
            ddsLogger.errorAudit(DdsErrors.DDS_CONFIGURATION_INVALID_FILENAME, "filename", getFilename());
            throw ex;
        }

        long lastMod = file.lastModified();

        // If file was not modified since out last load then return.
        if (lastMod <= lastModified) {
            return;
        }

        DdsConfigurationType config;

        try {
            config = ConfigurationParser.getInstance().readConfiguration(getFilename());
        }
        catch (IOException io) {
            ddsLogger.errorAudit(DdsErrors.DDS_CONFIGURATION_INVALID_FILENAME, "filename", getFilename());
            throw io;
        }
        catch (JAXBException jaxb) {
            ddsLogger.errorAudit(DdsErrors.DDS_CONFIGURATION_INVALID_XML, "filename", getFilename());
            throw jaxb;
        }

        if (config.getNsaId() == null || config.getNsaId().isEmpty()) {
            errorAudit = ddsLogger.errorAudit(DdsErrors.DDS_CONFIGURATION_INVALID_PARAMETER, "nsaId", config.getNsaId());
            throw new FileNotFoundException(ddsLogger.logToString(errorAudit));
        }

        setNsaId(config.getNsaId());

        if (config.getBaseURL() == null || config.getBaseURL().isEmpty()) {
            errorAudit = ddsLogger.errorAudit(DdsErrors.DDS_CONFIGURATION_INVALID_PARAMETER, "baseURL", config.getBaseURL());
            throw new FileNotFoundException(ddsLogger.logToString(errorAudit));
        }

        setBaseURL(config.getBaseURL());

        // Local document directory option from which we dynamically load files.
        if (config.getDocuments() != null && !config.getDocuments().isEmpty()) {
            // See if the user provided a fully qualified path.
            setDocuments(fullyQualifyPath(config.getDocuments()));
        }

        // The DocumentCache will created the directoy if not present.
        if (config.getCache() != null && !config.getCache().isEmpty()) {
            setCache(fullyQualifyPath(config.getCache()));
        }

        // The RepositoryCache will created the directoy if not present.
        if (config.getRepository() != null && !config.getRepository().isEmpty()) {
            setRepository(fullyQualifyPath(config.getRepository()));
        }

        if (config.getAuditInterval() == null) {
            setAuditInterval(DEFAULT_AUDIT_INTERVAL);
        }
        else if (config.getAuditInterval() < MIN_AUDIT_INTERVAL || config.getAuditInterval() > MAX_AUDIT_INTERVAL) {
            ddsLogger.errorAudit(DdsErrors.DDS_CONFIGURATION_INVALID_PARAMETER, "auditInterval", Long.toString(config.getAuditInterval()));
            setAuditInterval(DEFAULT_AUDIT_INTERVAL);
        }
        else {
            setAuditInterval(config.getAuditInterval());
        }

        if (config.getExpiryInterval() < EXPIRE_INTERVAL_MIN || config.getExpiryInterval() > EXPIRE_INTERVAL_MAX) {
            ddsLogger.errorAudit(DdsErrors.DDS_CONFIGURATION_INVALID_PARAMETER, "expiryInterval", Long.toString(config.getExpiryInterval()));
            setExpiryInterval(EXPIRE_INTERVAL_DEFAULT);
        }

        setExpiryInterval(config.getExpiryInterval());

        if (config.getActorPool() == null) {
            setActorPool(ACTORPOOL_DEFAULT_SIZE);
        }
        else if (config.getActorPool() < ACTORPOOL_MIN_SIZE || config.getActorPool() > ACTORPOOL_MAX_SIZE) {
            ddsLogger.errorAudit(DdsErrors.DDS_CONFIGURATION_INVALID_PARAMETER, "actorPool", Integer.toString(config.getActorPool()));
            setActorPool(ACTORPOOL_DEFAULT_SIZE);
        }
        else {
            setActorPool(config.getActorPool());
        }

        if (config.getBaseURL() == null || config.getBaseURL().isEmpty()) {
            ddsLogger.errorAudit(DdsErrors.DDS_CONFIGURATION_INVALID_PARAMETER, "baseURL=" + config.getBaseURL());
            throw new FileNotFoundException("Invalid base URL: " + config.getBaseURL());
        }

        if (config.getNotificationSize() < NOTIFICATIONSIZE_MIN_SIZE ||
                config.getNotificationSize() > NOTIFICATIONSIZE_MAX_SIZE) {
            setNotificationSize(NOTIFICATIONSIZE_DEFAULT_SIZE);
        }
        else {
            setNotificationSize(config.getNotificationSize());
        }

        httpConfig = new HttpConfig(config.getServer());
        if (config.getClient() != null) {
            clientConfig = Optional.of(new HttpsConfig(config.getClient()));
        }

        Optional<AccessControlType> accessControl = Optional.ofNullable(config.getAccessControl());
        if (!accessControl.isPresent()) {
            AccessControlType ac = factory.createAccessControlType();
            accessControl = Optional.of(ac);
        }

        accessControlList = new AccessControlList(accessControl.get());

        // Load the peer discovery URL.
        discoveryURL = config.getPeerURL().stream().collect(
            Collectors.toMap(peer -> peer.getType() + "/" + peer.getValue(), peer -> peer)
        );

        // Populate digital signing information for Gof3 documents.
        Optional<SignatureStoreType> signature = Optional.ofNullable(config.getSignature());
        if (signature.isPresent()) {
            if (signature.get().isSign()) {
                signingAlias = Optional.ofNullable(signature.get().getAlias()).orElseThrow(new IllegalArgumentExceptionSupplier("signing alias is required"));
                signingStore = new KeyStoreHandler(
                        fullyQualifyPath(Optional.ofNullable(signature.get().getFile()).orElseThrow(new IllegalArgumentExceptionSupplier("signing keystore filename is required"))),
                        signature.get().getPassword(),
                        signature.get().getType());
                sign = signature.get().isSign();
            }
        }

        lastModified = lastMod;
    }

    private String fullyQualifyPath(String file) {
        Path path = Paths.get(file);
        if (!path.isAbsolute()) {
            String basedir = System.getProperty(Properties.SYSTEM_PROPERTY_BASEDIR);
            path = Paths.get(basedir, file);
        }

        return path.toString();
    }

    public boolean isDocumentsConfigured() {
        return !(documents == null || documents.isEmpty());
    }

    public boolean isCacheConfigured() {
        return !(cache == null || cache.isEmpty());
    }

    public boolean isRepositoryConfigured() {
        return !(repository == null || repository.isEmpty());
    }

    /**
     * @return the auditInterval
     */
    @Deprecated
    public long getAuditInterval() {
        return auditInterval;
    }

    /**
     * @param auditInterval the auditInterval to set
     */
    @Deprecated
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
    public Collection<PeerURLType> getDiscoveryURL() {
        return Collections.unmodifiableCollection(discoveryURL.values());
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

    /**
     * @return the notificationSize
     */
    @Deprecated
    public int getNotificationSize() {
        return notificationSize;
    }

    /**
     * @param notificationSize the notificationSize to set
     */
    @Deprecated
    public void setNotificationSize(int notificationSize) {
        this.notificationSize = notificationSize;
    }

    public void setHttpConfig(HttpConfig httpConfig) {
        this.httpConfig = httpConfig;
    }

    public HttpConfig getHttpConfig() {
        return httpConfig;
    }

    public HttpsConfig getClientConfig() {
        return clientConfig.orElse(null);
    }

    public AccessControlList getAccessControlList() {
        return accessControlList;
    }

    public boolean isSign() {
        return sign;
    }

    public KeyStoreHandler getSigningStore() {
        return signingStore;
    }

    public String getSigningAlias() {
        return signingAlias;
    }
}
