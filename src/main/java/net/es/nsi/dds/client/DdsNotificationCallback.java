package net.es.nsi.dds.client;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.xml.bind.JAXBException;
import java.io.IOException;
import java.io.InputStream;
import net.es.nsi.dds.api.Exceptions;
import net.es.nsi.dds.jaxb.dds.NotificationListType;
import net.es.nsi.dds.jaxb.dds.NotificationType;
import net.es.nsi.dds.util.NsiConstants;
import net.es.nsi.dds.util.XmlUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hacksaw
 */
@Path("/dds/")
public class DdsNotificationCallback {
    private final Logger log = LoggerFactory.getLogger(getClass());

    @POST
    @Path("/callback")
    @Produces({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_XML })
    public Response notification(InputStream request) {
        NotificationListType notify;
        try {
            Object object = XmlUtilities.xmlToJaxb(NotificationListType.class, request);
            if (object instanceof NotificationListType) {
                notify = (NotificationListType) object;
            }
            else {
                log.error("notification: Expected NotificationListType but found " + object.getClass().getCanonicalName());
                throw Exceptions.invalidXmlException("callback", "Expected NotificationListType but found " + object.getClass().getCanonicalName());
            }
        } catch (IOException | JAXBException ex) {
            log.error("notification: Unable to process XML ", ex);
            throw Exceptions.invalidXmlException("callback", "Unable to process XML " + ex.getMessage());
        }

        System.out.println("DdsNotificationCallback: id=" + notify.getId() + ", href=" + notify.getHref() + ", providerId=" + notify.getProviderId());
        for (NotificationType notification : notify.getNotification()) {
            log.debug("DdsNotificationCallback: event=" + notification.getEvent().value() + ", documentId=" + notification.getDocument().getId());
        }
        TestServer.INSTANCE.pushDdsNotification(notify);
        return Response.accepted().build();
    }
}
