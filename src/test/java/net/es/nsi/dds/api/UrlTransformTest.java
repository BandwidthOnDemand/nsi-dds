/*
 * SENSE Resource Manager (SENSE-RM) Copyright (c) 2016, The Regents
 * of the University of California, through Lawrence Berkeley National
 * Laboratory (subject to receipt of any required approvals from the
 * U.S. Dept. of Energy).  All rights reserved.
 *
 * If you have questions about your rights to use or distribute this
 * software, please contact Berkeley Lab's Innovation & Partnerships
 * Office at IPO@lbl.gov.
 *
 * NOTICE.  This Software was developed under funding from the
 * U.S. Department of Energy and the U.S. Government consequently retains
 * certain rights. As such, the U.S. Government has been granted for
 * itself and others acting on its behalf a paid-up, nonexclusive,
 * irrevocable, worldwide license in the Software to reproduce,
 * distribute copies to the public, prepare derivative works, and perform
 * publicly and display publicly, and to permit other to do so.
 *
 */
package net.es.nsi.dds.api;

import lombok.extern.slf4j.Slf4j;
import net.es.nsi.dds.util.UrlTransform;
import org.apache.http.client.utils.URIBuilder;
import org.junit.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class UrlTransformTest {

  @Test
  public void testTransform() throws Exception {
    UrlTransform utilities = new UrlTransform("(http://localhost:8401|https://nsi0.snvaca.pacificwave.net/nsi-dds)");
    URIBuilder path = utilities.getPath("http://localhost:8401/dds/management/v1");
    log.debug("[UrlTransformTest] testTransform path = {}", path.build().toASCIIString());
    assertEquals(path.build().toASCIIString(), "https://nsi0.snvaca.pacificwave.net/nsi-dds/dds/management/v1");
  }

  @Test
  public void testBuilder() throws Exception {
    UrlTransform utilities = new UrlTransform("");
    URIBuilder path = utilities.getPath("http://localhost:8401/dds/management/v1");
    List<String> pathSegments = path.getPathSegments();
    URIBuilder append = new URIBuilder("/ping");
    pathSegments.addAll(append.getPathSegments());
    path.setPathSegments(pathSegments);
    log.debug("[UrlTransformTest] testBuilder path = {}", path.build().toASCIIString());
  }
}
