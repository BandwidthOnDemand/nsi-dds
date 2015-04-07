/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.util;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author hacksaw
 */
public class Log4jHelper {
    public static String getLog4jConfig(String configPath) throws IOException {
        String log4jConfig = System.getProperty("log4j.configuration");
        if (log4jConfig == null) {
            Path realPath = Paths.get(configPath, "log4j.xml").toRealPath();
            log4jConfig = realPath.toString();
        }
        return log4jConfig;
    }
}
