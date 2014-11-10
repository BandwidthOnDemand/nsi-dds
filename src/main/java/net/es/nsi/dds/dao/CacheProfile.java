package net.es.nsi.dds.dao;

/**
 * A simple bean facade to the full DDS profile object used to initialize a
 * document cache object with a runtime cache profile.
 *
 * @author hacksaw
 */
public class CacheProfile extends DdsProfile {

    public CacheProfile(DdsConfiguration configReader) {
        super(configReader);
    }

    @Override
    public String getDirectory() {
        return getConfiguration().getCache();
    }
}
