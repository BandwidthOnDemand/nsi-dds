/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.discovery;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hacksaw
 */
public class FileUtilities {

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    public static List<String> getXmlFileList(String directory) throws NullPointerException {
        File folder = null;
        try {
            folder = new File(directory);
        }
        catch (NullPointerException ex) {
            System.err.println("Failed to load directory " + directory + ", " + ex.getMessage());
            throw ex;
        }

        // We will grab all XML files from the target directory.
        File[] listOfFiles = folder.listFiles();

        // For each document file in the document directory load into discovery service.
        List<String> files = new ArrayList<>();
        String file;
        for (int i = 0; i < listOfFiles.length; i++) {
            if (listOfFiles[i].isFile()) {
                file = listOfFiles[i].getAbsolutePath();
                if (file.endsWith(".xml") || file.endsWith(".xml")) {
                    files.add(file);
                }
            }
        }

        return files;
    }
}
