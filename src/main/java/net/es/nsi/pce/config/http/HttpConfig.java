package net.es.nsi.pce.config.http;

public class HttpConfig {
    private String url;
    private String packageName;
    private String staticPath;
    private String wwwPath;

    /**
     * @return the url
     */
    public String getUrl() {
        return url;
    }

    /**
     * @param url the url to set
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * @return the packageName
     */
    public String getPackageName() {
        return packageName;
    }

    /**
     * @param packageName the packageName to set
     */
    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    /**
     * @return the staticPath
     */
    public String getStaticPath() {
        return staticPath;
    }

    /**
     * @param staticPath the staticPath to set
     */
    public void setStaticPath(String staticPath) {
        this.staticPath = staticPath;
    }

    /**
     * @return the wwwPath
     */
    public String getWwwPath() {
        return wwwPath;
    }

    /**
     * @param wwwPath the wwwPath to set
     */
    public void setWwwPath(String wwwPath) {
        this.wwwPath = wwwPath;
    }
}
