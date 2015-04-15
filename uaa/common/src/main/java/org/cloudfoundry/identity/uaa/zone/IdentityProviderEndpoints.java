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
package org.cloudfoundry.identity.uaa.zone;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
import static org.springframework.web.bind.annotation.RequestMethod.PUT;

@RequestMapping("/identity-providers")
@RestController
public class IdentityProviderEndpoints {

    private final IdentityProviderProvisioning identityProviderProvisioning;

    public IdentityProviderEndpoints(IdentityProviderProvisioning identityProviderProvisioning) {
        this.identityProviderProvisioning = identityProviderProvisioning;
    }

    @RequestMapping(method = POST)
    public ResponseEntity<IdentityProvider> createIdentityProvider(@RequestBody IdentityProvider body) {
        body.setIdentityZoneId(IdentityZoneHolder.get().getId());
        IdentityProvider createdIdp = identityProviderProvisioning.create(body);
        return new ResponseEntity<>(createdIdp, HttpStatus.CREATED);
    }

    @RequestMapping(value = "{id}", method = PUT)
    public ResponseEntity<IdentityProvider> updateIdentityProvider(@PathVariable String id, @RequestBody IdentityProvider body) {
        body.setId(id);
        body.setIdentityZoneId(IdentityZoneHolder.get().getId());
        IdentityProvider updatedIdp = identityProviderProvisioning.update(body);
        return new ResponseEntity<>(updatedIdp, HttpStatus.OK);
    }

    @RequestMapping(method = GET)
    public ResponseEntity<List<IdentityProvider>> retrieveIdentityProviders() {
        List<IdentityProvider> identityProviderList = identityProviderProvisioning.retrieveAll(IdentityZoneHolder.get().getId());
        return new ResponseEntity<>(identityProviderList, HttpStatus.OK);
    }
}
