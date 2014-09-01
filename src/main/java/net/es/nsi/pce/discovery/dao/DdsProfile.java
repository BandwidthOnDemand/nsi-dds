/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.dao;

/**
 *
 * @author hacksaw
 */
public abstract class DdsProfile {
    // The holder of our configuration.
    private DiscoveryConfiguration configReader;

    public DdsProfile(DiscoveryConfiguration configReader) {
        this.configReader = configReader;
    }

    public abstract String getDirectory();

    public DiscoveryConfiguration getConfiguration() {
        return configReader;
    }

    public long getExpiryInterval() {
        return configReader.getExpiryInterval();
    }

    public String getBaseURL() {
        return configReader.getBaseURL();
    }
}
