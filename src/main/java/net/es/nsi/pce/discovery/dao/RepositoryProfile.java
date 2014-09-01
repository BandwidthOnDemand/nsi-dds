/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery.dao;

/**
 *
 * @author hacksaw
 */
public class RepositoryProfile extends DdsProfile {

    public RepositoryProfile(DiscoveryConfiguration configReader) {
        super(configReader);
    }

    @Override
    public String getDirectory() {
        return getConfiguration().getRepository();
    }
}
