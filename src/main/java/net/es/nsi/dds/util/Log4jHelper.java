package net.es.nsi.dds.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.es.nsi.dds.config.Properties;

/**
 *
 * @author hacksaw
 */
public class Log4jHelper {
    public static String getLog4jConfig(String configPath) throws IOException {
        String log4jConfig = System.getProperty(Properties.SYSTEM_PROPERTY_LOG4J);
        if (log4jConfig == null) {
            Path realPath = Paths.get(configPath, "log4j.xml").toRealPath();
            log4jConfig = realPath.toString();
        }
        return log4jConfig;
    }
}
