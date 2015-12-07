/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.dds.schema;

import net.es.nsi.dds.util.XmlUtilities;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.UUID;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.jaxb.DdsParser;
import net.es.nsi.dds.jaxb.dds.DocumentType;

/**
 *
 * @author hacksaw
 */
public class FileHandling {
    private static final String cacheDir = "config/cache";
    private static final String tmpDir = "cache.test";

    public void cacheReadWrite() throws FileNotFoundException {
        String testDir = Paths.get(System.getProperty("user.dir"), tmpDir).toString();
        File dir = new File(testDir);
        if (!dir.exists()) {
            if (!dir.mkdir()) {
                throw new FileNotFoundException("Cannot create directory: " + testDir);
            }
        }

        Collection<String> xmlFilenames = XmlUtilities.getXmlFilenames(cacheDir);

        for (String filename : xmlFilenames) {
            System.out.println("cacheReadWrite: filename " + filename);
            DocumentType document;
            try {
                document = DdsParser.getInstance().readDocument(filename);
                if (document == null) {
                    System.err.println("cacheReadWrite: Loaded empty document from " + filename);
                    continue;
                }
            }
            catch (JAXBException | IOException ex) {
                System.err.println("cacheReadWrite: Failed to load file " + filename);
                ex.printStackTrace();
                continue;
            }

            System.out.println("cacheReadWrite: documentId=" + document.getId());

            String newFile = Paths.get(testDir, UUID.randomUUID().toString() + ".xml").toString();

            System.out.println("cacheReadWrite: adding new file " + newFile);

            try {
                DdsParser.getInstance().writeDocument(newFile, document);
            }
            catch (JAXBException | IOException ex) {
                System.err.println("cacheReadWrite: Failed to write file " + newFile);
                ex.printStackTrace();
            }
        }
    }


    @SuppressWarnings({"ResultOfMethodCallIgnored"})
    public static void main(String[] args) throws Exception {
        FileHandling fh = new FileHandling();
        fh.cacheReadWrite();
    }

}
