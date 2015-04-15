/*******************************************************************************
 *     Cloud Foundry 
 *     Copyright (c) [2009-2014] Pivotal Software, Inc. All Rights Reserved.
 *
 *     This product is licensed to you under the Apache License, Version 2.0 (the "License").
 *     You may not use this product except in compliance with the License.
 *
 *     This product includes a number of subcomponents with
 *     separate copyright notices and license terms. Your use of these
 *     subcomponents is subject to the terms and conditions of the
 *     subcomponent's license, as noted in the LICENSE file.
 *******************************************************************************/
package org.cloudfoundry.identity.uaa.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.cloudfoundry.identity.uaa.authentication.UaaAuthenticationTestFactory;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.junit.After;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.AuthorizationRequest;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

/**
 * @author Luke Taylor
 */
public class DefaultSecurityContextAccessorTests {

    @After
    public void clearContext() throws Exception {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void clientIsNotUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("client", "secret", UaaAuthority.ADMIN_AUTHORITIES));

        assertFalse(new DefaultSecurityContextAccessor().isUser());
    }

    @Test
    public void uaaUserIsUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                        UaaAuthenticationTestFactory.getAuthentication("1234", "user", "user@test.org"));

        assertTrue(new DefaultSecurityContextAccessor().isUser());
    }

    @Test
    public void adminUserIsAdmin() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("user", "password", UaaAuthority.ADMIN_AUTHORITIES));

        assertTrue(new DefaultSecurityContextAccessor().isAdmin());
    }

    @Test
    public void adminClientIsAdmin() throws Exception {

        BaseClientDetails client = new BaseClientDetails();
        client.setAuthorities(UaaAuthority.ADMIN_AUTHORITIES);

        AuthorizationRequest authorizationRequest = new AuthorizationRequest("admin", null);
        authorizationRequest.setResourceIdsAndAuthoritiesFromClientDetails(client);
        SecurityContextHolder.getContext().setAuthentication(new OAuth2Authentication(authorizationRequest.createOAuth2Request(), null));

        assertTrue(new DefaultSecurityContextAccessor().isAdmin());

    }

}
