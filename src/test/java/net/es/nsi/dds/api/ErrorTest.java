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
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 *
 * @author hacksaw
 */
@Slf4j
public class ErrorTest {

  @Test
  public void testSerialization() {
    Error error = new Error.Builder()
        .code(DiscoveryError.INTERNAL_SERVER_ERROR.getCode())
        .label(DiscoveryError.INTERNAL_SERVER_ERROR.getLabel())
        .description(DiscoveryError.INTERNAL_SERVER_ERROR.getDescription())
        .resource("http://localhost:8801/dds/management/v1/version")
        .build();

    log.error("[ManagementService] getResources returning error:\n{}", error.toString());

    Error error2 = new Error.Builder()
        .code(DiscoveryError.INTERNAL_SERVER_ERROR.getCode())
        .label(DiscoveryError.INTERNAL_SERVER_ERROR.getLabel())
        .description(DiscoveryError.INTERNAL_SERVER_ERROR.getDescription())
        .resource("http://localhost:8801/dds/management/v1/version")
        .build();

    log.error("[ManagementService] getResources returning error:\n{}", error2.toString());

    Assertions.assertNotEquals(error2.getErrorType().getId(), error.getErrorType().getId());
    Assertions.assertEquals(error2.getErrorType().getCode(), error.getErrorType().getCode());
    Assertions.assertEquals(error2.getErrorType().getResource(), error.getErrorType().getResource());
    Assertions.assertEquals(error2.getErrorType().getDescription(), error.getErrorType().getDescription());
  }
}
