package net.es.nsi.dds.schema;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import net.es.nsi.dds.jaxb.ConfigurationParser;
import net.es.nsi.dds.jaxb.configuration.DdsConfigurationType;
import org.junit.Assert;
import org.junit.Test;

public class DdsConfigurationTest {
    private static final String file1 = "src/test/resources/config/dds-schema-test.xml";
    private static final String file2 = "src/test/resources/config/dds-schema-test2.xml";

    @Test
    public void load1() throws JAXBException, IOException {
        // Read the test file.
        DdsConfigurationType dds = ConfigurationParser.getInstance().readConfiguration(file1);

        // Test the defaults.
        Assert.assertNotNull(dds.getClient());
        Assert.assertTrue(dds.getClient().isProduction());
        Assert.assertEquals(20, dds.getClient().getMaxConnPerRoute());
        Assert.assertEquals(80, dds.getClient().getMaxConnTotal());

    }

    public void load2() throws JAXBException, IOException {
        // Read the test file.
        DdsConfigurationType dds = ConfigurationParser.getInstance().readConfiguration(file2);

        Assert.assertNotNull(dds.getClient());
        Assert.assertFalse(dds.getClient().isProduction());
        Assert.assertEquals(5, dds.getClient().getMaxConnPerRoute());
        Assert.assertEquals(50, dds.getClient().getMaxConnTotal());

    }
}