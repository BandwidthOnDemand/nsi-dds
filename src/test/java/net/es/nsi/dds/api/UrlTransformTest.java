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

import java.util.Arrays;
import java.util.Collection;

import net.es.nsi.dds.util.UrlTransform;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;

/**
 *
 * @author hacksaw
 */
@RunWith(Parameterized.class)
public class UrlTransformTest {
  private static final String LOCAL = "http://localhost:8401/dds/management";
  private static final String PUBLIC = "https://nsi0.snvaca.pacificwave.net/nsi-dds";

  @Parameters(name = "{0}")
  public static Collection<Object[]> cases() {
    return Arrays.asList(new Object[][] {
      { "no transform", null, LOCAL, LOCAL, LOCAL + "/v1/a%20b" },
      { "empty transform", "", LOCAL, LOCAL, LOCAL + "/v1/a%20b" },
      { "prefix replaced", "(http://localhost:8401|" + PUBLIC + ")", LOCAL, PUBLIC + "/dds/management", PUBLIC + "/dds/management/v1/a%20b" },
      { "prefix not matched", "(http://other:8401|" + PUBLIC + ")", LOCAL, LOCAL, LOCAL + "/v1/a%20b" },
      { "prefix added only", "(|" + PUBLIC + ")", "/dds/ping", PUBLIC + "/dds/ping", PUBLIC + "/dds/ping/v1/a%20b" },
      // HttpClient 4 decoded %3A to ':' when segments were rebuilt; HttpClient 5 keeps it.
      { "encoded path kept", "", LOCAL + "/urn%3Aogf", LOCAL + "/urn%3Aogf", LOCAL + "/urn%3Aogf/v1/a%20b" },
      { "query kept", "", LOCAL + "?type=a%2Bb", LOCAL + "?type=a%2Bb", LOCAL + "/v1/a%20b?type=a%2Bb" },
    });
  }

  private final String transform;
  private final String input;
  private final String transformed;
  private final String concatenated;

  public UrlTransformTest(String name, String transform, String input, String transformed, String concatenated) {
    this.transform = transform;
    this.input = input;
    this.transformed = transformed;
    this.concatenated = concatenated;
  }

  @Test
  public void transformsPath() throws Exception {
    assertEquals(transformed, new UrlTransform(transform).getPath(input).build().toASCIIString());
  }

  @Test
  public void concatPathAppendsSegments() throws Exception {
    // How ManagementService builds resource links: leading "/" dropped, each segment encoded.
    assertEquals(concatenated, UrlTransform.concatPath(transformed, "/v1/a b"));
  }
}
