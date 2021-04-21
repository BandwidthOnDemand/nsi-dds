package net.es.nsi.dds.jaxb;

import java.io.IOException;
import java.io.InputStream;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import net.es.nsi.dds.jaxb.dds.CollectionType;
import net.es.nsi.dds.jaxb.dds.DocumentListType;
import net.es.nsi.dds.jaxb.dds.DocumentType;
import net.es.nsi.dds.jaxb.dds.ErrorType;
import net.es.nsi.dds.jaxb.dds.NotificationListType;
import net.es.nsi.dds.jaxb.dds.NotificationType;
import net.es.nsi.dds.jaxb.dds.ObjectFactory;
import net.es.nsi.dds.jaxb.dds.SubscriptionListType;
import net.es.nsi.dds.jaxb.dds.SubscriptionRequestType;
import net.es.nsi.dds.jaxb.dds.SubscriptionType;

/**
 * A singleton to load the very expensive NMWG JAXBContext once.
 *
 * @author hacksaw
 */
public class DdsParser extends JaxbParser {
    private static final String PACKAGES = "net.es.nsi.dds.jaxb.dds";
    private static final ObjectFactory factory = new ObjectFactory();

    private DdsParser() {
        super(PACKAGES);
    }

    /**
     * An internal static class that invokes our private constructor on object
     * creation.
     */
    private static class ParserHolder {
        public static final DdsParser INSTANCE = new DdsParser();
    }

    /**
     * Returns an instance of this singleton class.
     *
     * @return An object of the NmwgParser.
     */
    public static DdsParser getInstance() {
            return ParserHolder.INSTANCE;
    }

    public DocumentType readDocument(String filename) throws JAXBException, IOException {
        return this.parseFile(DocumentType.class, filename);
    }

    public void writeDocument(String file, DocumentType document) throws JAXBException, IOException {
        // Parse the specified file.
        JAXBElement<DocumentType> element = factory.createDocument(document);
        this.writeFile(element, file);
    }

    public CollectionType readCollection(String filename) throws JAXBException, IOException {
        return this.parseFile(CollectionType.class, filename);
    }

    public DocumentType xml2Document(InputStream is) throws JAXBException, IOException {
        return this.xml2Jaxb(DocumentType.class, is);
    }

    public String document2Xml(DocumentType document) throws JAXBException, IOException {
        JAXBElement<DocumentType> jaxb = factory.createDocument(document);
        return this.jaxb2Xml(jaxb);
    }

    public DocumentType xml2Document(String input) throws JAXBException, IllegalArgumentException {
        return this.xml2Jaxb(DocumentType.class, input);
    }

    public String documents2Xml(DocumentListType documents) throws JAXBException, IOException {
        JAXBElement<DocumentListType> jaxb = factory.createDocuments(documents);
        return this.jaxb2Xml(jaxb);
    }

    public DocumentListType xml2Documents(String input) throws JAXBException, IllegalArgumentException {
        return this.xml2Jaxb(DocumentListType.class, input);
    }

    public String collection2Xml(CollectionType collection) throws JAXBException, IOException {
        JAXBElement<CollectionType> jaxb = factory.createCollection(collection);
        return this.jaxb2Xml(jaxb);
    }

    public CollectionType xml2Collection(String input) throws JAXBException, IllegalArgumentException {
        return this.xml2Jaxb(CollectionType.class, input);
    }

    public String subscriptions2Xml(SubscriptionListType list) throws JAXBException, IOException {
        JAXBElement<SubscriptionListType> jaxb = factory.createSubscriptions(list);
        return this.jaxb2Xml(jaxb);
    }

    public SubscriptionListType xml2Subscriptions(String input) throws JAXBException, IllegalArgumentException {
        return this.xml2Jaxb(SubscriptionListType.class, input);
    }

    public String subscription2Xml(SubscriptionType subscription) throws JAXBException, IOException {
        JAXBElement<SubscriptionType> jaxb = factory.createSubscription(subscription);
        return this.jaxb2Xml(jaxb);
    }

    public SubscriptionType xml2Subscription(String input) throws JAXBException, IllegalArgumentException {
        return this.xml2Jaxb(SubscriptionType.class, input);
    }

    public String notifications2Xml(NotificationListType list) throws JAXBException, IOException {
        JAXBElement<NotificationListType> jaxb = factory.createNotifications(list);
        return this.jaxb2Xml(jaxb);
    }

    public NotificationListType xml2Notifications(String input) throws JAXBException, IllegalArgumentException {
        return this.xml2Jaxb(NotificationListType.class, input);
    }

    public String notification2Xml(NotificationType notification) throws JAXBException, IOException {
        JAXBElement<NotificationType> jaxb = factory.createNotification(notification);
        return this.jaxb2Xml(jaxb);
    }

    public NotificationType xml2Notification(String input) throws JAXBException, IllegalArgumentException {
        return this.xml2Jaxb(NotificationType.class, input);
    }

    public String subscriptionRequest2Xml(SubscriptionRequestType request) throws JAXBException, IOException {
        JAXBElement<SubscriptionRequestType> jaxb = factory.createSubscriptionRequest(request);
        return this.jaxb2Xml(jaxb);
    }

    public SubscriptionRequestType xml2SubscriptionRequest(String input) throws JAXBException, IllegalArgumentException {
        return this.xml2Jaxb(SubscriptionRequestType.class, input);
    }

    public ErrorType xml2Error(String input) throws JAXBException, IllegalArgumentException {
        return this.xml2Jaxb(ErrorType.class, input);
    }

    public String xmlFormatter(DocumentType document) throws JAXBException {
        return this.jaxb2XmlFormatter(factory.createDocument(document));
    }

    public String xmlFormatter(DocumentListType documents) throws JAXBException {
        return this.jaxb2XmlFormatter(factory.createDocuments(documents));
    }
}
