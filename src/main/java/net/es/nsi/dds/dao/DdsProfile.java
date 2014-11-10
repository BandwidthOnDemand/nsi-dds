/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.dao;

/**
 *
 * @author hacksaw
 */
public abstract class DdsProfile {
    // The holder of our configuration.
    private DdsConfiguration configReader;

    public DdsProfile(DdsConfiguration configReader) {
        this.configReader = configReader;
    }

    public abstract String getDirectory();

    public DdsConfiguration getConfiguration() {
        return configReader;
    }

    public long getExpiryInterval() {
        return configReader.getExpiryInterval();
    }

    public String getBaseURL() {
        return configReader.getBaseURL();
    }
}
