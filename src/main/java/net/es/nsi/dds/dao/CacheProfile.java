package net.es.nsi.dds.dao;

/**
 *
 * @author hacksaw
 */
public class CacheProfile extends DdsProfile {

    public CacheProfile(DiscoveryConfiguration configReader) {
        super(configReader);
    }

    @Override
    public String getDirectory() {
        return getConfiguration().getCache();
    }
}
