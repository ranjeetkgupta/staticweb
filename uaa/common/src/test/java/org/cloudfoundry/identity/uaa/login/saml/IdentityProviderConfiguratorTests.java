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
package org.cloudfoundry.identity.uaa.login.saml;

import org.apache.commons.httpclient.HttpClient;
import org.cloudfoundry.identity.uaa.client.ClientConstants;
import org.cloudfoundry.identity.uaa.config.YamlMapFactoryBean;
import org.cloudfoundry.identity.uaa.config.YamlProcessor;
import org.cloudfoundry.identity.uaa.zone.IdentityZone;
import org.cloudfoundry.identity.uaa.zone.IdentityZoneHolder;
import org.cloudfoundry.identity.uaa.zone.MultitenancyFixture;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.oauth2.provider.client.BaseClientDetails;
import org.springframework.security.saml.trust.httpclient.TLSProtocolSocketFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class IdentityProviderConfiguratorTests {

    private static String xml =
        "<?xml version=\"1.0\" encoding=\"UTF-8\"?><md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"http://www.okta.com/k2lw4l5bPODCMIIDBRYZ\"><md:IDPSSODescriptor WantAuthnRequestsSigned=\"true\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\"><md:KeyDescriptor use=\"signing\"><ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:X509Data><ds:X509Certificate>MIICmTCCAgKgAwIBAgIGAUPATqmEMA0GCSqGSIb3DQEBBQUAMIGPMQswCQYDVQQGEwJVUzETMBEG\n" +
        "A1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwNU2FuIEZyYW5jaXNjbzENMAsGA1UECgwET2t0YTEU\n" +
        "MBIGA1UECwwLU1NPUHJvdmlkZXIxEDAOBgNVBAMMB1Bpdm90YWwxHDAaBgkqhkiG9w0BCQEWDWlu\n" +
        "Zm9Ab2t0YS5jb20wHhcNMTQwMTIzMTgxMjM3WhcNNDQwMTIzMTgxMzM3WjCBjzELMAkGA1UEBhMC\n" +
        "VVMxEzARBgNVBAgMCkNhbGlmb3JuaWExFjAUBgNVBAcMDVNhbiBGcmFuY2lzY28xDTALBgNVBAoM\n" +
        "BE9rdGExFDASBgNVBAsMC1NTT1Byb3ZpZGVyMRAwDgYDVQQDDAdQaXZvdGFsMRwwGgYJKoZIhvcN\n" +
        "AQkBFg1pbmZvQG9rdGEuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCeil67/TLOiTZU\n" +
        "WWgW2XEGgFZ94bVO90v5J1XmcHMwL8v5Z/8qjdZLpGdwI7Ph0CyXMMNklpaR/Ljb8fsls3amdT5O\n" +
        "Bw92Zo8ulcpjw2wuezTwL0eC0wY/GQDAZiXL59npE6U+fH1lbJIq92hx0HJSru/0O1q3+A/+jjZL\n" +
        "3tL/SwIDAQABMA0GCSqGSIb3DQEBBQUAA4GBAI5BoWZoH6Mz9vhypZPOJCEKa/K+biZQsA4Zqsuk\n" +
        "vvphhSERhqk/Nv76Vkl8uvJwwHbQrR9KJx4L3PRkGCG24rix71jEuXVGZUsDNM3CUKnARx4MEab6\n" +
        "GFHNkZ6DmoT/PFagngecHu+EwmuDtaG0rEkFrARwe+d8Ru0BN558abFb</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor><md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</md:NameIDFormat><md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified</md:NameIDFormat><md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://pivotal.oktapreview.com/app/pivotal_pivotalcfstaging_1/k2lw4l5bPODCMIIDBRYZ/sso/saml\"/><md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://pivotal.oktapreview.com/app/pivotal_pivotalcfstaging_1/k2lw4l5bPODCMIIDBRYZ/sso/saml\"/></md:IDPSSODescriptor></md:EntityDescriptor>\n";


    IdentityProviderConfigurator conf = null;
    Map<String, Map<String, Object>>  data = null;
    IdentityProviderDefinition singleAdd = null;
    private static final String singleAddAlias = "sample-alias";

    String sampleYaml = "  providers:\n" +
        "    okta-local:\n" +
        "      idpMetadata: test-file-metadata.xml\n" +
        "      nameID: urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\n" +
        "      assertionConsumerIndex: 0\n" +
        "      metadataTrustCheck: true\n" +
        "      showSamlLoginLink: true\n" +
        "      linkText: 'Okta Preview 1'\n" +
        "      iconUrl: 'http://link.to/icon.jpg'\n" +
        "    okta-local-2:\n" +
        "      idpMetadata: |\n" +
        "        <?xml version=\"1.0\" encoding=\"UTF-8\"?><md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"http://www.okta.com/k2lw4l5bPODCMIIDBRYZ\"><md:IDPSSODescriptor WantAuthnRequestsSigned=\"true\" protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\"><md:KeyDescriptor use=\"signing\"><ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\"><ds:X509Data><ds:X509Certificate>MIICmTCCAgKgAwIBAgIGAUPATqmEMA0GCSqGSIb3DQEBBQUAMIGPMQswCQYDVQQGEwJVUzETMBEG\n" +
        "        A1UECAwKQ2FsaWZvcm5pYTEWMBQGA1UEBwwNU2FuIEZyYW5jaXNjbzENMAsGA1UECgwET2t0YTEU\n" +
        "        MBIGA1UECwwLU1NPUHJvdmlkZXIxEDAOBgNVBAMMB1Bpdm90YWwxHDAaBgkqhkiG9w0BCQEWDWlu\n" +
        "        Zm9Ab2t0YS5jb20wHhcNMTQwMTIzMTgxMjM3WhcNNDQwMTIzMTgxMzM3WjCBjzELMAkGA1UEBhMC\n" +
        "        VVMxEzARBgNVBAgMCkNhbGlmb3JuaWExFjAUBgNVBAcMDVNhbiBGcmFuY2lzY28xDTALBgNVBAoM\n" +
        "        BE9rdGExFDASBgNVBAsMC1NTT1Byb3ZpZGVyMRAwDgYDVQQDDAdQaXZvdGFsMRwwGgYJKoZIhvcN\n" +
        "        AQkBFg1pbmZvQG9rdGEuY29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCeil67/TLOiTZU\n" +
        "        WWgW2XEGgFZ94bVO90v5J1XmcHMwL8v5Z/8qjdZLpGdwI7Ph0CyXMMNklpaR/Ljb8fsls3amdT5O\n" +
        "        Bw92Zo8ulcpjw2wuezTwL0eC0wY/GQDAZiXL59npE6U+fH1lbJIq92hx0HJSru/0O1q3+A/+jjZL\n" +
        "        3tL/SwIDAQABMA0GCSqGSIb3DQEBBQUAA4GBAI5BoWZoH6Mz9vhypZPOJCEKa/K+biZQsA4Zqsuk\n" +
        "        vvphhSERhqk/Nv76Vkl8uvJwwHbQrR9KJx4L3PRkGCG24rix71jEuXVGZUsDNM3CUKnARx4MEab6\n" +
        "        GFHNkZ6DmoT/PFagngecHu+EwmuDtaG0rEkFrARwe+d8Ru0BN558abFb</ds:X509Certificate></ds:X509Data></ds:KeyInfo></md:KeyDescriptor><md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress</md:NameIDFormat><md:NameIDFormat>urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified</md:NameIDFormat><md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://pivotal.oktapreview.com/app/pivotal_pivotalcfstaging_1/k2lw4l5bPODCMIIDBRYZ/sso/saml\"/><md:SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://pivotal.oktapreview.com/app/pivotal_pivotalcfstaging_1/k2lw4l5bPODCMIIDBRYZ/sso/saml\"/></md:IDPSSODescriptor></md:EntityDescriptor>\n" +
        "      nameID: urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\n" +
        "      assertionConsumerIndex: 0\n" +
        "      metadataTrustCheck: true\n" +
        "      showSamlLoginLink: true\n" +
        "      linkText: 'Okta Preview 2'\n" +
        "    vsphere.local:\n" +
        "      idpMetadata: https://win2012-sso2.localdomain:7444/websso/SAML2/Metadata/vsphere.local\n" +
        "      nameID: urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\n" +
        "      assertionConsumerIndex: 1\n" +
        "      metadataTrustCheck: false\n"+
        "      showSamlLoginLink: false\n" +
        "      linkText: 'Log in with vCenter SSO'\n" +
        "      iconUrl: 'http://vsphere.local/iconurl.jpg'\n" +
        "    openam-local:\n" +
        "      idpMetadata: http://localhost:8081/openam/saml2/jsp/exportmetadata.jsp?entityid=http://localhost:8081/openam\n" +
        "      nameID: urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress\n" +
        "      assertionConsumerIndex: 0\n" +
        "      signMetaData: false\n" +
        "      signRequest: false\n" +
        "      showSamlLoginLink: true\n" +
        "      linkText: 'Log in with OpenAM'\n" +
        "    incomplete-provider:\n" +
        "      idpMetadata: http://localhost:8081/openam/saml2/jsp/exportmetadata.jsp?entityid=http://localhost:8081/openam\n";

    @Before
    public void setUp() throws Exception {
        conf = new IdentityProviderConfigurator();
        parseYaml(sampleYaml);
        singleAdd = new IdentityProviderDefinition(xml, singleAddAlias,"sample-nameID",1,true,true,"sample-link-test","sample-icon-url","uaa");
    }

    private void parseYaml(String sampleYaml) {
        YamlMapFactoryBean factory = new YamlMapFactoryBean();
        factory.setResolutionMethod(YamlProcessor.ResolutionMethod.OVERRIDE_AND_IGNORE);
        List<Resource> resources = new ArrayList<>();
        ByteArrayResource resource = new ByteArrayResource(sampleYaml.getBytes());
        resources.add(resource);
        factory.setResources(resources.toArray(new Resource[resources.size()]));
        Map<String, Object> tmpdata = factory.getObject();
        data = new HashMap<>();
        for (Map.Entry<String, Object> entry : ((Map<String, Object>)tmpdata.get("providers")).entrySet()) {
            data.put(entry.getKey(), (Map<String, Object>)entry.getValue());
        }
    }

    @Test
    public void testCloneIdentityProviderDefinition() throws Exception {
        IdentityProviderDefinition clone = singleAdd.clone();
        assertEquals(singleAdd, clone);
        assertNotSame(singleAdd, clone);
    }

    @Test
    public void testSingleAddProviderDefinition() throws Exception {
        conf.setIdentityProviders(data);
        conf.afterPropertiesSet();
        conf.addIdentityProviderDefinition(singleAdd);
        testGetIdentityProviderDefinitions(6, false);
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullProvider() {
        conf.addIdentityProviderDefinition(null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddNullProviderAlias() {
        singleAdd.setIdpEntityAlias(null);
        conf.addIdentityProviderDefinition(singleAdd);
    }

    @Test
    public void testRefreshProviders() throws Exception {
        conf.setIdentityProviders(data);
        conf.getIdentityProviderDefinitions();
        conf.refreshProviders(Arrays.asList(singleAdd));
        testGetIdentityProviderDefinitions(1, false);
    }

    @Test
    public void testIdentityProviderDefinitionSocketFactoryTest() {
        singleAdd.setMetaDataLocation("http://www.test.org/saml/metadata");
        assertEquals(IdentityProviderDefinition.DEFAULT_HTTP_SOCKET_FACTORY, singleAdd.getSocketFactoryClassName());
        singleAdd.setMetaDataLocation("https://www.test.org/saml/metadata");
        assertEquals(IdentityProviderDefinition.DEFAULT_HTTPS_SOCKET_FACTORY, singleAdd.getSocketFactoryClassName());
        singleAdd.setSocketFactoryClassName(TLSProtocolSocketFactory.class.getName());
        assertEquals(TLSProtocolSocketFactory.class.getName(), singleAdd.getSocketFactoryClassName());
    }

    @Test
    public void testGetIdentityProviderDefinitionsForZone() throws Exception {
        String zoneId = UUID.randomUUID().toString();
        IdentityZone zone = MultitenancyFixture.identityZone(zoneId, "test-zone");

        IdentityProviderDefinition identityProviderDefinition = new IdentityProviderDefinition(xml, "zoneIdpAlias","sample-nameID",1,true,true,"sample-link-test","sample-icon-url",zoneId);
        conf.addIdentityProviderDefinition(identityProviderDefinition);

        List<IdentityProviderDefinition> idps = conf.getIdentityProviderDefinitionsForZone(zone);
        assertEquals(1, idps.size());
        assertEquals("zoneIdpAlias", idps.get(0).getIdpEntityAlias());
    }

    @Test
    public void testGetIdentityProviderDefinititonsForAllowedProviders() throws Exception {
        BaseClientDetails clientDetails = new BaseClientDetails();
        List<String> clientIdpAliases = Arrays.asList("vsphere.local", "okta-local-2");
        clientDetails.addAdditionalInformation(ClientConstants.ALLOWED_PROVIDERS, clientIdpAliases);

        conf.setIdentityProviders(data);
        List<IdentityProviderDefinition> clientIdps = conf.getIdentityProviderDefinitions(clientIdpAliases, IdentityZoneHolder.get(), false);
        assertEquals(2, clientIdps.size());
        assertTrue(clientIdpAliases.contains(clientIdps.get(0).getIdpEntityAlias()));
        assertTrue(clientIdpAliases.contains(clientIdps.get(1).getIdpEntityAlias()));
    }

    @Test
    public void testReturnAllIdpsInZoneForClientWithNoAllowedProviders() throws Exception {
        conf.setIdentityProviders(data);
        IdentityProviderDefinition identityProviderDefinitionInOtherZone = new IdentityProviderDefinition(xml, "zoneIdpAlias","sample-nameID",1,true,true,"sample-link-test","sample-icon-url","other-zone-id");
        conf.addIdentityProviderDefinition(identityProviderDefinitionInOtherZone);

        List<IdentityProviderDefinition> clientIdps = conf.getIdentityProviderDefinitions(null, IdentityZoneHolder.get(), false);
        assertEquals(5, clientIdps.size());
    }

    @Test
    public void testReturnNoIdpsInZoneForClientWithNoAllowedProviders() throws Exception {
        conf.setIdentityProviders(data);
        IdentityProviderDefinition identityProviderDefinitionInOtherZone = new IdentityProviderDefinition(xml, "zoneIdpAlias","sample-nameID",1,true,true,"sample-link-test","sample-icon-url","other-zone-id");
        conf.addIdentityProviderDefinition(identityProviderDefinitionInOtherZone);

        List<IdentityProviderDefinition> clientIdps = conf.getIdentityProviderDefinitions(null, IdentityZoneHolder.get(), true);
        assertTrue(clientIdps.isEmpty());
    }

    @Test
    public void testGetIdentityProviderDefinitions() throws Exception {
        testGetIdentityProviderDefinitions(5);
    }

    protected void testGetIdentityProviderDefinitions(int count) throws Exception {
        testGetIdentityProviderDefinitions(count, true);
    }
    protected void testGetIdentityProviderDefinitions(int count, boolean addData) throws Exception {
        if (addData) {
            conf.setIdentityProviders(data);
            conf.afterPropertiesSet();
        }
        List<IdentityProviderDefinition> idps = conf.getIdentityProviderDefinitions();
        assertEquals(count, idps.size());
        for (IdentityProviderDefinition idp : idps) {
            switch (idp.getIdpEntityAlias()) {
                case "vsphere.local" : {
                    assertEquals(IdentityProviderDefinition.MetadataLocation.URL, idp.getType());
                    assertEquals("https://win2012-sso2.localdomain:7444/websso/SAML2/Metadata/vsphere.local", idp.getMetaDataLocation());
                    assertEquals("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent", idp.getNameID());
                    assertEquals(1, idp.getAssertionConsumerIndex());
                    assertEquals("Log in with vCenter SSO", idp.getLinkText());
                    assertEquals("http://vsphere.local/iconurl.jpg", idp.getIconUrl());
                    assertFalse(idp.isShowSamlLink());
                    assertFalse(idp.isMetadataTrustCheck());
                    assertEquals("org.apache.commons.httpclient.contrib.ssl.EasySSLProtocolSocketFactory", idp.getSocketFactoryClassName());
                    break;
                }
                case "okta-local" : {
                    assertEquals(IdentityProviderDefinition.MetadataLocation.FILE, idp.getType());
                    assertEquals("test-file-metadata.xml", idp.getMetaDataLocation());
                    assertEquals("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress", idp.getNameID());
                    assertEquals(0, idp.getAssertionConsumerIndex());
                    assertEquals("Okta Preview 1", idp.getLinkText());
                    assertEquals("http://link.to/icon.jpg", idp.getIconUrl());
                    assertTrue(idp.isShowSamlLink());
                    assertTrue(idp.isMetadataTrustCheck());
                    break;
                }
                case "okta-local-2" : {
                    assertEquals(IdentityProviderDefinition.MetadataLocation.DATA, idp.getType());
                    assertEquals("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress", idp.getNameID());
                    assertEquals(0, idp.getAssertionConsumerIndex());
                    assertEquals("Okta Preview 2", idp.getLinkText());
                    assertNull(idp.getIconUrl());
                    assertTrue(idp.isShowSamlLink());
                    assertTrue(idp.isMetadataTrustCheck());
                    break;
                }
                case "openam-local" : {
                    assertEquals(IdentityProviderDefinition.MetadataLocation.URL, idp.getType());
                    assertEquals("urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress", idp.getNameID());
                    assertEquals(0, idp.getAssertionConsumerIndex());
                    assertEquals("Log in with OpenAM", idp.getLinkText());
                    assertNull(idp.getIconUrl());
                    assertTrue(idp.isShowSamlLink());
                    assertTrue(idp.isMetadataTrustCheck());
                    assertEquals("org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory", idp.getSocketFactoryClassName());
                    break;
                }
                case "vsphere.local.legacy" : {
                    assertEquals(IdentityProviderDefinition.MetadataLocation.URL, idp.getType());
                    assertEquals("http://win2012-sso2.localdomain:7444/websso/SAML2/Metadata/vsphere.local", idp.getMetaDataLocation());
                    assertEquals("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent", idp.getNameID());
                    assertEquals(0, idp.getAssertionConsumerIndex());
                    assertEquals("Use your corporate credentials", idp.getLinkText());
                    assertNull(idp.getIconUrl());
                    assertTrue(idp.isShowSamlLink());
                    assertTrue(idp.isMetadataTrustCheck());
                    assertEquals("org.apache.commons.httpclient.protocol.DefaultProtocolSocketFactory", idp.getSocketFactoryClassName());
                    break;
                }
                case singleAddAlias : {
                    assertEquals(singleAdd, idp);
                    assertNotSame(singleAdd, idp);
                    break;
                }
                case "incomplete-provider" : {
                    assertTrue(idp.isShowSamlLink());
                    break;
                }
                default:
                    fail();
            }
        }
    }

    @Test
    public void testGetIdentityProvidersWithLegacyProvider() throws Exception {
        conf.setLegacyIdpMetaData("http://win2012-sso2.localdomain:7444/websso/SAML2/Metadata/vsphere.local");
        conf.setLegacyIdpIdentityAlias("vsphere.local.legacy");
        conf.setLegacyNameId("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        testGetIdentityProviderDefinitions(6);
    }

    @Test
    public void testGetIdentityProviders() throws Exception {
        conf.setLegacyIdpMetaData("http://win2012-sso2.localdomain:7444/websso/SAML2/Metadata/vsphere.local");
        conf.setLegacyIdpIdentityAlias("vsphere.local.legacy");
        conf.setLegacyNameId("urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
        conf.setMetadataFetchingHttpClientTimer(new Timer());
        conf.setHttpClient(new HttpClient());
        testGetIdentityProviderDefinitions(6);
        conf.getIdentityProviders();
    }


    @Test(expected = IllegalStateException.class)
    public void testDuplicateAlias() throws Exception {
        conf.setLegacyIdpMetaData("https://win2012-sso2.localdomain:7444/websso/SAML2/Metadata/vsphere.local");
        conf.setLegacyIdpIdentityAlias("vsphere.local");
        conf.setIdentityProviders(data);
        conf.afterPropertiesSet();
    }

}
