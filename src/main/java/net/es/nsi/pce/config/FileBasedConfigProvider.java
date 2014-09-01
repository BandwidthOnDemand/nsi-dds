package net.es.nsi.pce.config;

import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author hacksaw
 */
public abstract class FileBasedConfigProvider implements ConfigProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private String filename;

    private long timeStamp = 0;

    protected boolean isFileUpdated() {
        File file = new File(this.getFilename());
        
        long lastModified = file.lastModified();
        
        log.debug("isFileUpdated: filename = " + this.getFilename()  + ", timeStamp = " + timeStamp + ", lastModified = " + lastModified);
        
        // Did we have an update? 
        if (this.timeStamp != lastModified) {
            timeStamp = file.lastModified();
            return true;
        }

        // No, file is not updated.
        return false;
    }

    @Override
    public String getFilename() {
        return filename;
    }

    @Override
    public void setFilename(String filename) {
        this.filename = filename;
    }


}
