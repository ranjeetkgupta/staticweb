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

package org.cloudfoundry.identity.uaa.oauth;

import org.cloudfoundry.identity.uaa.client.ClientConstants;
import org.cloudfoundry.identity.uaa.test.JdbcTestBase;
import org.cloudfoundry.identity.uaa.zone.MultitenantJdbcClientDetailsService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.provider.ClientAlreadyExistsException;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientRegistrationService;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ClientAdminBootstrapTests extends JdbcTestBase {

    private ClientAdminBootstrap bootstrap;

    private JdbcClientDetailsService clientRegistrationService;

    @Before
    public void setUpClientAdminTests() throws Exception {
        bootstrap = new ClientAdminBootstrap();
        clientRegistrationService = new MultitenantJdbcClientDetailsService(dataSource);
        bootstrap.setClientRegistrationService(clientRegistrationService);
    }

    @After
    public void tearDownClientAdminTests() throws Exception {
        flyway.clean();
    }

    @Test
    public void testSimpleAddClient() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "foo");
        map.put("secret", "bar");
        map.put("scope", "openid");
        map.put("authorized-grant-types", "authorization_code");
        map.put("authorities", "uaa.none");
        doSimpleTest(map);
    }

    @Test
    public void testSimpleAddClientWithSignupSuccessRedirectUrl() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "foo");
        map.put("secret", "bar");
        map.put("scope", "openid");
        map.put("authorized-grant-types", "authorization_code");
        map.put("authorities", "uaa.none");
        map.put("signup_redirect_url", "callback_url");
        doSimpleTest(map);
    }

    @Test
    public void testAllowedIdentityProvidersAsAdditionalInformation() throws Exception {
        List<String> idps = Arrays.asList("idp1", "idp1");
        Map<String, Object> map = new HashMap<>();
        map.put("id", "foo");
        map.put("secret", "bar");
        map.put("scope", "openid");
        map.put("authorized-grant-types", "authorization_code");
        map.put("authorities", "uaa.none");
        map.put("signup_redirect_url", "callback_url");
        map.put(ClientConstants.ALLOWED_PROVIDERS, idps);
        ClientDetails created = doSimpleTest(map);
        assertEquals(idps, created.getAdditionalInformation().get(ClientConstants.ALLOWED_PROVIDERS));
    }

    @Test
    public void testSimpleAddClientWithChangeEmailRedirectUrl() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "foo");
        map.put("secret", "bar");
        map.put("scope", "openid");
        map.put("authorized-grant-types", "authorization_code");
        map.put("authorities", "uaa.none");
        map.put("change_email_redirect_url", "change_email_callback_url");
        doSimpleTest(map);
    }

    @Test
    public void testSimpleAddClientWithAutoApprove() throws Exception {
        ClientRegistrationService clientRegistrationService = mock(ClientRegistrationService.class);
        bootstrap.setClientRegistrationService(clientRegistrationService);
        Map<String, Object> map = new HashMap<>();
        map.put("id", "foo");
        map.put("secret", "bar");
        map.put("scope", "openid");
        map.put("authorized-grant-types", "authorization_code");
        map.put("authorities", "uaa.none");
        BaseClientDetails output = new BaseClientDetails("foo", "none", "openid", "authorization_code,refresh_token",
                        "uaa.none");
        output.setClientSecret("bar");
        bootstrap.setAutoApproveClients(Arrays.asList("foo"));
        when(clientRegistrationService.listClientDetails()).thenReturn(Collections.<ClientDetails> emptyList())
                        .thenReturn(Collections.<ClientDetails> singletonList(output));
        bootstrap.setClients(Collections.singletonMap((String) map.get("id"), map));
        bootstrap.afterPropertiesSet();
        verify(clientRegistrationService).addClientDetails(output);
        BaseClientDetails updated = new BaseClientDetails(output);
        updated.setAdditionalInformation(Collections.singletonMap(ClientConstants.AUTO_APPROVE, true));
        verify(clientRegistrationService).updateClientDetails(updated);
    }

    @Test
    public void testOverrideClient() throws Exception {
        ClientRegistrationService clientRegistrationService = mock(ClientRegistrationService.class);
        bootstrap.setClientRegistrationService(clientRegistrationService);
        Map<String, Object> map = new HashMap<>();
        map.put("secret", "bar");
        map.put("override", true);
        bootstrap.setClients(Collections.singletonMap("foo", map));
        doThrow(new ClientAlreadyExistsException("Planned")).when(clientRegistrationService).addClientDetails(
                        any(ClientDetails.class));
        bootstrap.afterPropertiesSet();
        verify(clientRegistrationService, times(1)).addClientDetails(any(ClientDetails.class));
        verify(clientRegistrationService, times(1)).updateClientDetails(any(ClientDetails.class));
        verify(clientRegistrationService, times(1)).updateClientSecret("foo", "bar");
    }

    @Test
    public void testOverrideClientByDefault() throws Exception {
        ClientRegistrationService clientRegistrationService = mock(ClientRegistrationService.class);
        bootstrap.setClientRegistrationService(clientRegistrationService);
        Map<String, Object> map = new HashMap<>();
        map.put("secret", "bar");
        bootstrap.setClients(Collections.singletonMap("foo", map));
        doThrow(new ClientAlreadyExistsException("Planned")).when(clientRegistrationService).addClientDetails(
                        any(ClientDetails.class));
        bootstrap.afterPropertiesSet();
        verify(clientRegistrationService, times(1)).addClientDetails(any(ClientDetails.class));
        verify(clientRegistrationService, times(1)).updateClientDetails(any(ClientDetails.class));
        verify(clientRegistrationService, times(1)).updateClientSecret("foo", "bar");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOverrideClientWithYaml() throws Exception {
        ClientRegistrationService clientRegistrationService = mock(ClientRegistrationService.class);
        bootstrap.setClientRegistrationService(clientRegistrationService);

        @SuppressWarnings("rawtypes")
        Map fooClient = new Yaml().loadAs("id: foo\noverride: true\nsecret: bar\n"
                        + "access-token-validity: 100", Map.class);
        @SuppressWarnings("rawtypes")
        Map barClient = new Yaml().loadAs("id: bar\noverride: true\nsecret: bar\n"
                        + "access-token-validity: 100", Map.class);
        @SuppressWarnings("rawtypes")
        Map clients = new HashMap();
        clients.put("foo", fooClient);
        clients.put("bar", barClient);
        bootstrap.setClients(clients);
        doThrow(new ClientAlreadyExistsException("Planned")).when(clientRegistrationService).addClientDetails(
                        any(ClientDetails.class));
        bootstrap.afterPropertiesSet();
        verify(clientRegistrationService, times(2)).addClientDetails(any(ClientDetails.class));
        verify(clientRegistrationService, times(2)).updateClientDetails(any(ClientDetails.class));
        verify(clientRegistrationService, times(1)).updateClientSecret("foo", "bar");
        verify(clientRegistrationService, times(1)).updateClientSecret("bar", "bar");
    }

    @Test
    public void testHttpsUrlIsAddedIfNotPresent() throws Exception {
        ClientRegistrationService clientRegistrationService = mock(ClientRegistrationService.class);
        bootstrap.setClientRegistrationService(clientRegistrationService);
        bootstrap.setDomain("bar.com");
        BaseClientDetails input = new BaseClientDetails("foo", "password,scim,tokens", "read,write,password",
                        "client_credentials", "uaa.none", "http://foo.bar.com/spam");
        when(clientRegistrationService.listClientDetails()).thenReturn(Arrays.<ClientDetails> asList(input));
        bootstrap.afterPropertiesSet();
        verify(clientRegistrationService, times(1)).updateClientDetails(any(ClientDetails.class));
    }

    @Test
    public void testHttpsUrlIsNotAddedIfAlreadyPresent() throws Exception {
        ClientRegistrationService clientRegistrationService = mock(ClientRegistrationService.class);
        bootstrap.setClientRegistrationService(clientRegistrationService);
        bootstrap.setDomain("bar.com");
        BaseClientDetails input = new BaseClientDetails("foo", "password,scim,tokens", "read,write,password",
                        "client_credentials", "uaa.none", "http://foo.bar.com,https://foo.bar.com");
        when(clientRegistrationService.listClientDetails()).thenReturn(Arrays.<ClientDetails> asList(input));
        bootstrap.afterPropertiesSet();
        verify(clientRegistrationService, times(1)).updateClientDetails(any(ClientDetails.class));
    }

    private ClientDetails doSimpleTest(Map<String, Object> map) throws Exception {
        bootstrap.setClients(Collections.singletonMap((String) map.get("id"), map));
        bootstrap.afterPropertiesSet();

        ClientDetails created = clientRegistrationService.loadClientByClientId((String) map.get("id"));
        assertNotNull(created);
        assertSet((String) map.get("scope"), Collections.singleton("uaa.none"), created.getScope(), String.class);
        assertSet((String) map.get("resource-ids"), new HashSet(Arrays.asList("none")), created.getResourceIds(), String.class);
        assertSet((String) map.get("redirect-uri"), null, created.getRegisteredRedirectUri(), String.class);

        String authTypes = (String) map.get("authorized-grant-types");
        if (authTypes!=null && authTypes.contains("authorization_code")) {
            authTypes+=",refresh_token";
        }
        assertSet(authTypes, Collections.emptySet(), created.getAuthorizedGrantTypes(), String.class);

        Integer validity = (Integer) map.get("access-token-validity");
        assertEquals(validity, created.getAccessTokenValiditySeconds());
        validity = (Integer) map.get("refresh-token-validity");
        assertEquals(validity, created.getRefreshTokenValiditySeconds());

        assertSet((String) map.get("authorities"), Collections.emptySet(), created.getAuthorities(), GrantedAuthority.class);

        Map<String, Object> info = new HashMap<>(map);

        for (String key : Arrays.asList("resource-ids", "scope", "authorized-grant-types", "authorities",
                "redirect-uri", "secret", "id", "override", "access-token-validity",
                "refresh-token-validity")) {
            info.remove(key);
        }
        for (Map.Entry<String,Object> entry : info.entrySet()) {
            assertTrue("Client should contain additional information key:"+ entry.getKey(), created.getAdditionalInformation().containsKey(entry.getKey()));
            if (entry.getValue()!=null) {
                assertEquals(entry.getValue(), created.getAdditionalInformation().get(entry.getKey()));
            }
        }

        return created;

    }

    private void assertSet(String expectedValue, Collection defaultValueIfNull, Collection actualValue, Class<?> type) {
        Collection assertScopes =  defaultValueIfNull;
        if (expectedValue!=null) {
            if (String.class.equals(type)) {
                assertScopes = StringUtils.commaDelimitedListToSet(expectedValue);
            } else {
                assertScopes = AuthorityUtils.commaSeparatedStringToAuthorityList(expectedValue);
            }
        }
        assertEquals(assertScopes, actualValue);
    }

}
