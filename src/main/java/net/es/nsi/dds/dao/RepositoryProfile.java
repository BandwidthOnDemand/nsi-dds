package net.es.nsi.dds.dao;

/**
 * A simple bean facade to the full DDS profile object used to initialize a
 * document cache object with a document repository profile.
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
