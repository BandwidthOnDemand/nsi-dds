package net.es.nsi.dds.client;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import net.es.nsi.dds.api.jaxb.NotificationListType;
import net.es.nsi.dds.api.jaxb.NotificationType;
import net.es.nsi.dds.schema.NsiConstants;

/**
 *
 * @author hacksaw
 */
@Path("/dds/")
public class DdsNotificationCallback {

    @POST
    @Path("/callback")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, NsiConstants.NSI_DDS_V1_JSON, NsiConstants.NSI_DDS_V1_XML })
    public Response notification(NotificationListType notify) {
        System.out.println("DdsNotificationCallback: id=" + notify.getId() + ", href=" + notify.getHref() + ", providerId=" + notify.getProviderId());
        for (NotificationType notification : notify.getNotification()) {
            System.out.println("DdsNotificationCallback: event=" + notification.getEvent().value() + ", documentId=" + notification.getDocument().getId());
        }
        TestServer.INSTANCE.pushDdsNotification(notify);
        return Response.accepted().build();
    }
}
