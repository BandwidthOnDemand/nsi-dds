package net.es.nsi.dds.authorization;

import com.google.common.base.Strings;
import net.es.nsi.dds.dao.DdsConfiguration;
import net.es.nsi.dds.spring.SpringApplicationContext;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DnAuthorizationProvider implements AuthorizationProvider {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final DdsConfiguration config;

    public DnAuthorizationProvider(DdsConfiguration config) {
        this.config = config;
    }

    public static DnAuthorizationProvider getInstance() {
        DnAuthorizationProvider provider = SpringApplicationContext.getBean("authorizationProvider", DnAuthorizationProvider.class);
        return provider;
    }

    @Override
    public boolean authorize(String dn, String operation, String resource) {
        boolean authorized = config.getAccessControlList().isAuthorized(dn, operation, resource);

        if (log.isInfoEnabled()) {
            StringBuilder sb = new StringBuilder();
            if (authorized) {
                sb.append("Authorized: ");
            }
            else {
                sb.append("Rejected: ");
            }

            if (Strings.isNullOrEmpty(dn)) {
                sb.append("<null DN>");
            }
            else {
                X500NameStyle x500NameStyle = ExtendedRFC4519Style.INSTANCE;
                X500Name principal = new X500Name(dn);
                sb.append(x500NameStyle.toString(principal));
            }
            
            sb.append(", ");
            sb.append(operation);
            sb.append(" ");
            sb.append(resource);

            log.info(sb.toString());
        }
        
        return authorized;
    }
}