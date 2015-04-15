package org.cloudfoundry.identity.uaa.db;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.cloudfoundry.identity.uaa.authentication.Origin;
import org.cloudfoundry.identity.uaa.zone.IdentityZone;
import org.springframework.jdbc.core.JdbcTemplate;

import com.googlecode.flyway.core.api.migration.spring.SpringJdbcMigration;

public class BootstrapIdentityZones implements SpringJdbcMigration {

    @Override
    public void migrate(JdbcTemplate jdbcTemplate) throws Exception {
        IdentityZone uaa = IdentityZone.getUaa();
        Timestamp t = new Timestamp(uaa.getCreated().getTime());
        jdbcTemplate.update("insert into identity_zone VALUES (?,?,?,?,?,?,?)", uaa.getId(),t,t,uaa.getVersion(),uaa.getSubdomain(),uaa.getName(),uaa.getDescription());
        Map<String,String> originMap = new HashMap<String, String>();
        Set<String> origins = new LinkedHashSet<String>();
        origins.addAll(Arrays.asList(new String[] {Origin.UAA,Origin.LOGIN_SERVER,Origin.LDAP,Origin.KEYSTONE}));
        origins.addAll(jdbcTemplate.queryForList("SELECT DISTINCT origin from users", String.class));
        for (String origin : origins) {
            String identityProviderId = UUID.randomUUID().toString();  
            originMap.put(origin, identityProviderId);
            jdbcTemplate.update("insert into identity_provider VALUES (?,?,?,0,?,?,?,?,null)",identityProviderId, t, t, uaa.getId(),origin,origin,origin);
        }
        jdbcTemplate.update("update oauth_client_details set identity_zone_id = ?",uaa.getId());
        List<String> clientIds = jdbcTemplate.queryForList("SELECT client_id from oauth_client_details", String.class);
        for (String clientId : clientIds) {
            jdbcTemplate.update("insert into client_idp values (?,?) ",clientId,originMap.get(Origin.UAA));
        }
        jdbcTemplate.update("update users set identity_provider_id = (select id from identity_provider where identity_provider.origin_key = users.origin), identity_zone_id = (select identity_zone_id from identity_provider where identity_provider.origin_key = users.origin);");
        jdbcTemplate.update("update group_membership set identity_provider_id = (select id from identity_provider where identity_provider.origin_key = group_membership.origin);");
    }
}
