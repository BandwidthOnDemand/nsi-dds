package net.es.nsi.dds.schema;

import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import net.es.nsi.dds.jaxb.ConfigurationParser;
import net.es.nsi.dds.jaxb.configuration.DdsConfigurationType;
import org.junit.Assert;
import org.junit.Test;

public class DdsConfigurationTest {
    private static final String FILE_1 = "src/test/resources/config/dds-schema-test.xml";
    private static final String FILE_2 = "src/test/resources/config/dds-schema-test2.xml";

    @Test
    public void load1() throws JAXBException, IOException {
        // Read the test file.
        DdsConfigurationType dds = ConfigurationParser.getInstance().readConfiguration(FILE_1);

        // Test the defaults.
        Assert.assertNotNull(dds.getClient());
        Assert.assertFalse(dds.getClient().isSecure());
        Assert.assertEquals(5, dds.getClient().getMaxConnPerRoute());
        Assert.assertEquals(60, dds.getClient().getMaxConnTotal());

    }

    @Test
    public void load2() throws JAXBException, IOException {
        // Read the test file.
        DdsConfigurationType dds = ConfigurationParser.getInstance().readConfiguration(FILE_2);

        Assert.assertNotNull(dds.getClient());
        Assert.assertTrue(dds.getClient().isSecure());
        Assert.assertEquals(5, dds.getClient().getMaxConnPerRoute());
        Assert.assertEquals(50, dds.getClient().getMaxConnTotal());

    }
}