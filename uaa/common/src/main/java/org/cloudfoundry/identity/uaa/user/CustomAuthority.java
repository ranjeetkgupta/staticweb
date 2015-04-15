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
package org.cloudfoundry.identity.uaa.user;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.annotate.JsonCreator;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * The UAA only distinguishes 2 types of user for internal usage, denoted
 * <code>uaa.admin</code> and <code>uaa.user</code>. Other authorities might be
 * stored in the back end for the purposes of other resource servers,
 * so this enumeration has convenient methods for extracting the UAA user types
 * from authorities lists.
 * 
 * @author Luke Taylor
 * @author Dave Syer
 */
public class CustomAuthority implements GrantedAuthority {



 

    private final String userType;

    public CustomAuthority(String userType) {
        this.userType = userType;
    }
       

    /**
     * The authority granted by this value (same as user type).
     * 
     * @return the name of the value (uaa.user, etc.)
     * @see org.springframework.security.core.GrantedAuthority#getAuthority()
     */
    @Override
    public String getAuthority() {
        return userType;
    }

    @Override
    public String toString() {
        return userType;
    }


    public static List<CustomAuthority> getAuthorityList(CustomAuthority authority) {
    	List<CustomAuthority> list = new ArrayList<CustomAuthority>();
    	list.add(authority);
    	List<CustomAuthority> authList = Collections.unmodifiableList(list);
    	return authList;
    }
}
