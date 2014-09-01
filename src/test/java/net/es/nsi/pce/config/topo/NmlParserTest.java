/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.config.topo;

import net.es.nsi.pce.schema.NmlParser;
import java.io.FileNotFoundException;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.api.jaxb.NmlNSAType;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;

/**
 *
 * @author hacksaw
 */
public class NmlParserTest {
    @Test
    public void testSuccess() throws Exception {
        // This is a valid load of the file.
        NmlNSAType nsa = NmlParser.getInstance().parseNSAFromFile("src/test/resources/nml/parse/tests/netherlight.xml");
        assertEquals("urn:ogf:network:netherlight.net:2013:nsa:bod", nsa.getId());
    }

    @Test
    public void testFileNotFound() throws Exception {
        // This catches a file not found exception.
        try {
            NmlParser.getInstance().parseNSAFromFile("src/test/resources/nml/parse/tests/invalidfilename.xml");
        }
        catch (FileNotFoundException fnf) {
            // This is a valid error
            assertTrue(fnf != null);
            return;
        }
        catch (Exception ex) {
            // This is illegal.
            fail();
        }

        fail();
    }

    @Test
    public void testJAXBException() throws Exception {
        // Test a parsing error.
        NmlNSAType nsa = null;
        try {
            nsa = NmlParser.getInstance().parseNSAFromFile("src/test/resources/nml/parse/tests/badcontent.xml");
        }
        catch (JAXBException jaxb) {
            // This is a valid error for test.
            assertTrue(jaxb != null);
            return;
        }
        catch (Exception ex) {
            // This is an valid error
            fail();
        }

        if (nsa != null) {
            fail();
        }
    }
}
