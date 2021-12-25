package net.es.nsi.dds.config.http;

import com.google.common.base.Strings;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Optional;
import net.es.nsi.dds.config.Properties;
import net.es.nsi.dds.jaxb.configuration.ServerType;
import net.es.nsi.dds.jaxb.configuration.StaticType;

public class HttpConfig {
    private final static String DEFAULT_ADDRESS = "localhost";
    private final static String DEFAULT_PORT = "8401";
    private final static String DEFAULT_PACKAGENAME = "net.es.nsi.dds";

    private Optional<String> address;
    private Optional<String> port;
    private Optional<String> packageName;
    private Optional<String> staticPath = Optional.empty();
    private Optional<String> relativePath = Optional.empty();
    private boolean secure = false;
    private String basedir;

    public HttpConfig(String address, String port, String packageName) {
        this.address = Optional.ofNullable(Strings.emptyToNull(address));
        this.port = Optional.ofNullable(Strings.emptyToNull(port));
        this.packageName = Optional.ofNullable(Strings.emptyToNull(packageName));
    }

    public HttpConfig(ServerType config) throws IOException, KeyManagementException,
            NoSuchAlgorithmException, NoSuchProviderException, KeyStoreException, CertificateException,
            UnrecoverableKeyException {
        if (config == null) {
            throw new IllegalArgumentException("HttpConfig: server configuration not provided");
        }

        // We will use the application basedir to fully qualify any relative paths.
        basedir = System.getProperty(Properties.SYSTEM_PROPERTY_BASEDIR);

        // These two parameters must be present.
        address = Optional.ofNullable(Strings.emptyToNull(config.getAddress()));
        port = Optional.ofNullable(Strings.emptyToNull(config.getPort()));
        packageName = Optional.ofNullable(Strings.emptyToNull(config.getPackageName()));

        // staticPath is optional, and only provided if an HTTP server should
        // also be started for static content.
        Optional<StaticType> aStatic = Optional.ofNullable(config.getStatic());
        if (aStatic.isPresent()) {
            staticPath = Optional.ofNullable(Strings.emptyToNull(aStatic.get().getPath()));
            relativePath = Optional.ofNullable(Strings.emptyToNull(aStatic.get().getRelative()));
            if(staticPath.isPresent()) {
                staticPath = Optional.ofNullable(getAbsolutePath(staticPath.get()));
            }
        }

        // Server secure settings are optional, and only provided if standalone
        // HTTPS capabilities are needed.
        if (config.isSecure()) {
            secure = true;
        }
    }

    private String getAbsolutePath(String inPath) throws IOException {
        Path outPath = Paths.get(inPath);
        if (!outPath.isAbsolute()) {
            outPath = Paths.get(basedir, inPath);
        }

        return outPath.toRealPath().toString();
    }

    public URI getURI() {
        if (secure) {
            return URI.create("https://" + address.orElse(DEFAULT_ADDRESS) + ":" + port.orElse(DEFAULT_PORT));
        }

        return URI.create("http://" + address.orElse(DEFAULT_ADDRESS) + ":" + port.orElse(DEFAULT_PORT));
    }

    public URL getURL() throws MalformedURLException {
        return getURI().toURL();
    }

    public String getAddress() {
        return address.orElse(DEFAULT_ADDRESS);
    }

    public String getPort() {
        return port.orElse(DEFAULT_PORT);
    }

    /**
     * @return the url
     */
    public String getUrl() {
        return getURI().toString();
    }

    /**
     * @return the packageName
     */
    public String getPackageName() {
        return packageName.orElse(DEFAULT_PACKAGENAME);
    }

    /**
     * @return the staticPath
     */
    public String getStaticPath() {
        return staticPath.orElse(null);
    }

    /**
     * @return the wwwPath
     */
    public String getRelativePath() {
        return relativePath.orElse(null);
    }

    public boolean isSecure() {
        return secure;
    }

    public HttpsContext getHttpsContext() {
        return HttpsContext.getInstance();
    }
}
