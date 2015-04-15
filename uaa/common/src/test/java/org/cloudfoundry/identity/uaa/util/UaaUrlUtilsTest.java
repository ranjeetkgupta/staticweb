/*******************************************************************************
 *     Cloud Foundry
 *     Copyright (c) [2009-2015] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.util;

import java.net.URLDecoder;
import java.net.URLEncoder;

import org.apache.commons.httpclient.util.URIUtil;
import org.cloudfoundry.identity.uaa.zone.IdentityZone;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneHolder;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.util.UriUtils;

import static org.junit.Assert.*;

public class UaaUrlUtilsTest {

    private UaaUrlUtils uaaURLUtils;

    @Before
    public void setUp() throws Exception {
        uaaURLUtils = new UaaUrlUtils("http://uaa.example.com");
    }

    @After
    public void tearDown() throws Exception {
        IdentityZoneHolder.clear();
    }

    @Test
    public void testGetUaaUrl() throws Exception {
        assertEquals("http://uaa.example.com", uaaURLUtils.getUaaUrl());
    }

    @Test
    public void testGetUaaUrlWithPath() throws Exception {
        assertEquals("http://uaa.example.com/login", uaaURLUtils.getUaaUrl("/login"));
        assertEquals("http://uaa.example.com/login", uaaURLUtils.getUaaUrl("login"));
    }

    @Test
    public void testGetUaaUrlWithZone() throws Exception {
        setIdentityZone("zone1");

        assertEquals("http://zone1.uaa.example.com", uaaURLUtils.getUaaUrl());
    }

    @Test
    public void testGetUaaUrlWithZoneAndPath() throws Exception {
        setIdentityZone("zone1");

        assertEquals("http://zone1.uaa.example.com/login", uaaURLUtils.getUaaUrl("/login"));
    }

    @Test
    public void testGetHost() throws Exception {
        assertEquals("uaa.example.com", uaaURLUtils.getUaaHost());
    }

    @Test
    public void testGetHostWithZone() throws Exception {
        setIdentityZone("zone1");

        assertEquals("zone1.uaa.example.com", uaaURLUtils.getUaaHost());
    }

    @Test
    public void testDecodeScopes() throws Exception {
        String xWWWFormEncodedscopes = "scim.userids+password.write+openid+cloud_controller.write+cloud_controller.read";
        System.out.println(URLDecoder.decode(xWWWFormEncodedscopes));
        System.out.println(URIUtil.decode(xWWWFormEncodedscopes,"UTF-8"));
        System.out.println(UriUtils.decode(xWWWFormEncodedscopes,"UTF-8"));

        //Assert.assertEquals(URLDecoder.decode(xWWWFormEncodedscopes), UriUtils.decode(xWWWFormEncodedscopes, "UTF-8"));
    }

    private void setIdentityZone(String subdomain) {
        IdentityZone zone = new IdentityZone();
        zone.setSubdomain(subdomain);
        IdentityZoneHolder.set(zone);
    }
}