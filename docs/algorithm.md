# DDS Server Pseudo Code
The following appendix contains example pseudo code for the DDS server function.  The pseudo code describes the DDS abstract API logic, and can be used to implement the DDS function within an NSI deployment.

The NSI CS Aggregator NSA will deploy a full DDS server performing both requester and provider functions.  The Aggregator NSA registers for document notification from all peer NSA, and delivers document notifications to all subscribed peers.  The Aggregator also publishes documents associated with its own NSA such as an NSA description document.  An Aggregator would use the addDocument/updateDocument API or some locally defined mechanism to publish these documents into the local DDS server instance, thereby allowing them to be propagated to all peers forming the GDS.

The NSI CS uPA NSA does not require access to documents published by other NSA within the GDS.  For this reason, the uPA has two implementation options for integration into the DDS.  The first is to use a DDS requester client to publish its documents (addDocument/updateDocument API) into an Aggregator that will maintain the lifecycle of the documents on behalf of the uPA.  This will require a prearranged agreement between the uPA and Aggregator.

The second option is for the uPA to deploy a DDS server but only enable the provider role.  In this configuration the DDS server allows peer Aggregators to subscribe for notifications on document events relating to the uPA’s documents, but does not itself subscribe to any peer NSA for document notifications.  This will result in only the uPA’s documents being contained in the local DDS server, with all peer NSA being updated with uPA document notifications.

```
PROGRAM DdsServer:

    // Global variables holding configuration, state, and discovered documents.
    DECLARE a list variable called Peers holding configuration information for all peers;
    DECLARE a map variable called GlobalDocumentSpace holding all known documents in the 
            GDS(indexed by unique document identifier);
    DECLARE a map variable called LastDiscovered holding discovered date/time values for
            each document (indexed by unique document identifier);
    DECLARE a map variable called MySubscriptions holding local subscriptions on remote
            DDS servers (indexed by peer containing subscription);
    DECLARE a map variable called PeerSubscriptions holding remote DDS server
            subscriptions on local DDS server(indexed by peer owning subscription);
    DECLARE a string variable called MyNsaId holding the local NSA identifier;
    DECLARE a time variable called SubscriptionAuditInterval holding the time between
            subscription audit intervals;
    DECLARE a time variable called ExpireAuditInterval holding the time between document
            expiry audit intervals;

    // start() initializes the system and registers subscriptions with all remote DDS
    // server Peers.
    PROCEDURE start() {
        // Initialize the DDS system.
        READ Peers from list of peer NSA from configuration;
        READ SubscriptionAuditInterval from configuration;
        READ ExpireAuditInterval from configuration;
        READ MyNsaId from configuration;
        READ GlobalDocumentSpace from storage discarding any expired documents;

        SET MySubscriptions to an empty map<peer, subscription>;
        SET PeerSubscriptions to an empty map<peer, subscription>;

        // For simplification register for all document events on all Peers configured as
        // a provider role.  Each peer will send a full list of documents present in their
        // document space.


        // For simplification register for all document events on all Peers.  Each peer
        // will send a full list of documents present in their document space.
        FOR each peer in Peers with a provider role DO
            // First we need to delete any existing subscriptions we may have on this
            // peer.
            CALL peer.getSubscriptions(MyNsaId)
            RETURNING status, subscriptions, and lastModifiedTime;
            IF status is success THEN
                FOR each subscription in subscriptions DO
                    CALL peer.deleteSubscription(subscription.id);
                ENDFOR;
            ENDIF;

            // Add the new subscription and store it for later auditing.
            CALL peer.addSubscription(MyNsaId, notificationCallback,
                    filter(include event All)) RETURNING status, subscription, and
                    lastModifiedTime;
            IF status is success and subscription is present THEN
                STORE <peer, subscription> in MySubscriptions;
            ENDIF;
        ENDFOR;

        // Schedule maintenance tasks.
        SCHEDULE subscriptionAudit() at SubscriptionAuditInterval;
        SCHEDULE documentExpireAudit() at ExpireAuditInterval;
    }

    
    // subscriptionAudit() verifies there is an active subscription on all configured DDS
    // Peers. It will create a new subscription if one does not exist, and will delete any
    // subscriptions no longer in use.
    PROCEDURE subscriptionAudit() {
        // oldSubscriptions will hold the list of MySubscriptions we need to clean up when
        // audit is completed.
        DECLARE a map variable called oldSubscriptions to hold the list of MySubscriptions
                to clean up when audit is completed (indexed by peer containing the
                subscription);
        SET oldSubscriptions to copy of MySubscriptions;

        // Audit subscription for each of our configured Peers.
        FOR each peer in Peers with a provider role DO
            SET subscription to MySubscriptions.get(peer);

            IF subscription is present THEN
                // Get subscription for this peer.
                CALL peer.getSubscription(subscription.id) RETURNING oldSubscription;

                // Remove this subscription from our cleanup list. 
                REMOVE oldSubscription from oldSubscriptions;

                IF oldSubscription is present THEN
                    // This subscription is still valid so proceed to next iteration.
                    CONTINUE;
                ENDIF;

                // This subscription is no longer valid.
                REMOVE subscription from MySubscriptions;

            ENDIF;

            // We do not have a subscription for this peer so create one.
            CALL peer.addSubscription(MyNsaId, notificationCallback,
                    filter(include event All)) RETURNING newSubscription;

            IF newSubscription is present THEN
                STORE <peer, newSubscription> in MySubscriptions;
            ENDIF;
        ENDFOR;

        // Now remove any MySubscriptions no longer needed.
        FOR each subscription in oldSubscriptions DO
            SET peer to subscription.peer;
            CALL peer.deleteSubscription(subscriptionId);
        ENDFOR;

        // Schedule our next audit run.
        SCHEDULE subscriptionAudit() at SubscriptionAuditInterval;
    }

    // documentExpireAudit() - removes any expired documents from the local document
    // space.
    PROCEDURE documentExpireAudit() {
        FOR each document in GlobalDocumentSpace DO
            IF document.expires is in past THEN
                REMOVE document from GlobalDocumentSpace;
            ENDIF;
        ENDFOR;

        // Schedule our next audit run.
        SCHEDULE documentExpireAudit() at ExpireAuditInterval;
    }

    // notificationCallback() is the notification callback endpoint for delivery of
    // subscription events from remote DDS Peers.
    API notificationCallback(notifications) RETURNS status {
        VALIDATE parameters notifications RETURNING failed if invalid;

        // Reject the notification if not from a valid peer.
        IF notifications.providerId not in list of Peers with a provider role THEN
            RETURN status of failed(invalid peer);
        ENDIF;

        // Reject the notification if not a valid subscription.
        IF notifications.id not in list of MySubscriptions THEN
            RETURN status of failed(invalid subscription);
        ENDIF;


        // Process each notification, storing new/updated documents and propagating any
        // changes to peers.
        FOR each notification in notifications DO
            // Get document out of notification.
            SET document to notification.document;

            // Create a unique document identifier for indexing.
            CALL uid(document.nsa, document.type, document.id) RETURNING uid;

            // If an old version of the document is present make sure this is a newer
            // version before storing and propagating.
            SET oldDocument to GlobalDocumentSpace.get(uid);
            IF oldDocument is present THEN
                IF oldDocument.version is less than document.version THEN
                    REPLACE oldDocument in GlobalDocumentSpace with document;
                    STORE current date/time in LastDiscovered indexed by uid;
                    CALL propagateDocument(providerId, UPDATE, document);
                ENDIF;
            ELSE
                STORE document in GlobalDocumentSpace indexed by uid;
                STORE current date/time in LastDiscovered for uid;
                CALL propdateDocument(providerId, NEW, document);
            ENDIF;
        ENDFOR;	
    }

    // propdateDocument() sends document notification events to all DDS peer subscribed
    // for the document event type.
    PROCEDURE propagateDocument(providerId, event, document) {
        // Inspect each subscription to see if it matches this document event.
        FOR each subscription in PeerSubscriptions DO
            // Do not send the document event back to the originating provider.
            IF subscription.requesterId equals providerId THEN
                CONTINUE;
            ENDIF;

            // If the subscription matches the document even propagate.
            IF subscription.filter matches event and document THEN
                SET callback to subscription.callback;
                SET notification to new notification(MyNsaId, event, document);
                CALL callback(notification) RETURNING status;

                // Subscription may no longer be valid.  Delete and let peer
                // re-register their next audit.
                IF status is not success THEN
                    DELETE subscription from PeerSubscriptions;
                ENDIF;
             ENDIF;
        ENDFOR;
    }


    // getDocuments() returns a list of documents and the time of the latest document
    // change on the DDS provider.
    API getDocuments([nsa], [type], [id], [lastDiscoveredTime])
            RETURNS status, a list of [0..n] document, and [lastDiscoveredTime] {
        VALIDATE parameters nsa, type, id, and lastDiscoveredTime
                RETURNING status of failed(invalid parameter) if invalid;

        DECLARE a list variable called results to hold documents matching the
                query filter;
        DECLARE a date/time variable called newLast to hold the time of the most recently
                discovered document;

        SET newLast to Date(0);

        IF lastDiscoveredTime is absent THEN
            SET lastDiscoveredTime to Date(0);
        ENDIF;

        // Inspect each document in the GDS for a match.
        FOR each document in GlobalDocumentSpace DO
            // Create a unique document identifier for indexing.
            CALL uid(document.nsa, document.type, document.id) RETURNING uid;

            // Determine if this document meets any lastDiscoveredTime criteria.
            DECLARE a date/time variable called currentLast to hold the current document’s
                    last discovered time;
            SET currentLast to LastDiscovered.get(uid);
            IF currentLast is later than lastDiscoveredTime THEN
                // Now match on the other criteria.
                IF document matches filter(nsa, type, id) THEN
                    STORE document in results;

                    // Track the latest discovered time.
                    IF currentLast is later than newLast THEN
                        STORE currentLast in newLast;
                    ENDIF;
                ENDIF;
            ENDIF;
        ENDFOR;

        RETURN status of success, results, and newLast;
    }

    // getLocalDocuments() returns a list of documents associated with the queried DDS
    // provider and the time of the latest document change on that provider.
    API getLocalDocuments([type], [id], [lastDiscoveredTime])
            RETURNS status, a list of [0..n] document, and [lastDiscoveredTime] {
        CALL getDocuments(MyNsaId, type, id, lastDiscoveredTime)
                RETURNS results and newLast;
        RETURN results and newLast;
    }

    // getDocument() returns the requested document and the time of the latest change
    // on the document.
    API getDocument(nsa, type, id, [lastDiscoveredTime])
            RETURNS status, [document], and [lastDiscoveredTime] {
        CALL getDocuments(nsa, type, id, lastDiscoveredTime) RETURNS results and newLast;
        RETURN results and newLast;
    }

    // addDocument() adds a new document to the space associated with the DDS provider.
    API addDocument(nsa, type, id, version, expires, [signature], contents)
            RETURNS status, [document], and [lastDiscoveredTime] {
        VALIDATE nsa, type, id, version, expires, signature, and contents
                RETURNING status of failed(invalid parameter) if invalid;

        // Build the unique document identifier and determine if document already exists.
        CALL uid(document.nsa, document.type, document.id) RETURNING uid;
        SET document to GlobalDocumentSpace.get(uid);

		// A document can only be added when one does not already exist.
        IF document is present THEN
            RETURN status of failed(document exists);
        ENDIF;

        // Add the new document.
        SET document to
                new document(nsa, type, id, version, expires, signature, contents);
        STORE document in GlobalDocumentSpace indexed by uid;

        // Update the lastDiscoveredTime.
        SET lastDiscoveredTime as current date/time;
        STORE lastDiscoveredTime in LastDiscovered indexed by uid;

        // Send the new document event to all peers.
        CALL propagateDocument(MyNsaId, NEW, document);

        RETURN status of success, document, and lastDiscoveredTime;
    }

    // updateDocument - updates an existing document within the space associated with the
    // DDS provider.
    API updateDocument(nsa, type, id, version, expires, [signature], contents)
            RETURNS status, [document], and [lastDiscoveredTime] {
        VALIDATE nsa, type, id, version, expires, signature, and contents
                RETURNING status of failed(invalid parameter) if invalid;

        // Build the unique document identifier and retrieve the document for update.
        CALL uid(document.nsa, document.type, document.id) RETURNING uid;
        SET document to GlobalDocumentSpace.get(uid);

        // A document must be present to update.
        IF document is not present THEN
            RETURN status of failed(document does not exists);
        ENDIF;

        // Update only if this is a new document.
        IF document.version is not less than version THEN
            RETURN status of failed(invalid version);
        ENDIF;

        // Replace existing document with the updated document.
        SET updatedDocument to
                new document(nsa, type, id, version, expires, signature, contents);
        REPLACE document in GlobalDocumentSpace with updatedDocument;

        // Update the lastDiscoveredTime.
        SET lastDiscoveredTime as current date/time;
        STORE lastDiscoveredTime in LastDiscovered indexed by uid;

        // Send document update event to all peers.
        CALL propagateDocument(MyNsaId, UPDATE, document);

        RETURN status of success, document, and lastDiscoveredTime;
    }

    // addSubscription() subscribes a requester for document event notifications based on
    // the supplied filter.
    API addSubscripton(requesterId, callback, filter)
                RETURNS status, [subscription], and [lastModifiedTime] {
        VALIDATE requesterId, callback, and filter
                RETURNING status of failed(invalid parameter) if invalid;

        // Verify this requesting peer is configured for a requester role.
        IF requesterId not in list of Peers with a requester role THEN
            RETURN status of failed(invalid peer);
        ENDIF;

        // Create the new subscription with a new unique subscription identifier.
        SET subscription to new subscription(requesterId, callback, filter);
        STORE subscription in PeerSubscriptions indexed by subscription.id;

        // Save the of this subscription’s creation for lastModifiedTime queries.
        SET lastModifiedTime as current date/time;
        STORE lastModifiedTime in LastModified indexed by subscription.id;

        // Send a notification for all documents matching the new filter but with document
        // event All.
        FOR each document in GlobalDocumentSpace DO
            IF subscription.filter matches document THEN
                SET callback to subscription.callback;
                SET notification to new notification(MyNsaId, All, document);
                CALL callback(notification) RETURNING status;
                IF status is not success THEN
                    DELETE subscription from PeerSubscriptions;
                    RETURN status of failed(invalid endpoint);
                ENDIF;
            ENDIF;
        ENDFOR;

        RETURN status of success, subscription, and lastModifiedTime;
    }

    // editSubscription() allows an existing subscription to be edited.
    API editSubscription(id, requesterId, callback, filter)
            RETURNS status, [subscription], and [lastModifiedTime] {
        VALIDATE id, requesterId, callback, and filter
                RETURNING status of failed(invalid parameter) if invalid;

        // Get the current subscription.
        SET subscription to PeerSubscriptions.get(id);

        // A subscription must be present to update.
        IF subscription is not present THEN
            RETURN status of failed(subscription does not exists);
        ENDIF;

        // Update the subscription.
        SET newSubscription to new subscription(requesterId, callback, filter);
        REPLACE subscription in PeerSubscriptions with newSubscription;

        // Updated the last modified time.
        SET lastModifiedTime as current date/time;
        STORE lastModifiedTime in LastModified indexed by subscription.id;

        // Build a list of notifications based on documents matching the updated filter
        // criteria.
        DECLARE a list variable called notifications to hold a list of notification for
                each document matching filter criteria;
        FOR each document in GlobalDocumentSpace DO
            IF newSubscription.filter matches document THEN
                SET notification to new notification(MyNsaId, All, document);
                STORE notification in notifications;
            ENDIF;
        ENDFOR;

        // Send list of notifications to the subscriber.
        SET callback to newSubscription.callback;
        CALL callback(notifications) RETURNING status;
        IF status is not success THEN
            DELETE newSubscription from PeerSubscriptions;
            RETURN status of failed(invalid endpoint);
        ENDIF;

        RETURN status of success, newSubscription, and lastModifiedTime;
    }

    // deleteSubscription() deletes the subscription associated with id from the provider
    // NSA.
    API deleteSubscription(id) RETURNS status, and [subscription] {
        VALIDATE id RETURNING status of failed(invalid parameter) if invalid;

        // Get the subscription.
        SET subscription to PeerSubscriptions.get(id);

        // A subscription must be present to delete.
        IF subscription is not present THEN
            RETURN status of failed(subscription not found);
        ENDIF;

        DELETE subscription from PeerSubscriptions;

        RETURN status of success and subscription;
    }

    // getSubscriptions() returns a list of subscriptions and the time of the latest
    // subscription change on the provider NSA.
    API getSubscriptions([requesterId], [lastModifiedTime])
            RETURNS status, list of [0..n] subscription, and [lastModifiedTime] {
        VALIDATE requesterId and lastModifiedTime
                RETURNING status of failed(invalid parameter) if invalid;

        DECLARE a list variable called results to hold the matching list of subscriptions;
        DECLARE a date/time variable called newLast to hold the most recent
                lastModifiedTime;

        SET newLast to Date(0);

        // If a lastModifiedTime filter was not provided set to start of time so all
        // subscriptions are more recent.
        IF lastModifiedTime is absent THEN
            SET lastModifiedTime to Date(0);
        ENDIF;

	// Add subscriptions that match the requested filter.
	FOR each subscription in PeerSubscriptions DO
            DECLARE a date/time variable called currentLast to hold this subscription's
                    lastModifiedTime;
            SET currentLast to LastModified.get(subscription.id);
            IF currentLast is later than lastModifiedTime THEN
                IF subscription matches filter(requesterId, lastModifiedTime) THEN
                    STORE subscription in results;

                    IF currentLast is later than newLast THEN
                        STORE currentLast in newLast;
                    ENDIF;
                ENDIF;
	     ENDIF;
        ENDFOR;

        RETURN status of success, results, and newLast;
    }

    // getSubscription() returns a single subscription identified by the id parameter and
    // the time this subscription was last modified.
    API getSubscription(id, [lastModifiedTime])
            RETURNS status, [subscription], and [lastModifiedTime] {
        VALIDATE id and lastModifiedTime
                RETURNING status of failed(invalid parameter) if invalid;

        // Get the subscription.
        SET subscription to PeerSubscriptions.get(id);

        // A subscription must be present for this to be successful.
        IF subscription is not present THEN
            RETURN status of failed(subscription not found);
        ENDIF;

        DECLARE a date/time variable called currentLast to hold this subscription's
                lastModifiedTime;
        SET currentLast to LastModified.get(subscription.id);

        // If a lastModifiedTime filter was not provided set to start of time so all
        // subscriptions are more recent.
        IF lastModifiedTime is absent THEN
            SET lastModifiedTime to Date(0);
        ENDIF;

        IF currentLast is later than lastModifiedTime THEN
            RETURN status of success and subscription;
        ELSE
            RETURN status of success(not modified);
        ENDIF;
    }

    // getAll() returns a collection of subscriptions, documents, and local documents
    // discovered since lastDiscoveredTime (treating lastDiscoveredTime as
    // lastModifiedTime in the case of subscriptions). The time of the last
    // discovered/modified element is also returned.
    API getAll([lastDiscoveredTime])
            RETURNS status, list of [0..n] subscription, list of [0..n] document,
            list of [0..n] local document, and [lastDiscoveredTime] {
        VALIDATE lastDiscoveredTime
                RETURNING status of failed(invalid parameter) if invalid;

        DECLARE a list variable called subscriptions to hold the matching list of
                subscriptions;
        DECLARE a list variable called documents to hold the matching list of documents;
        DECLARE a list variable called local to hold the matching list of local documents;
        DECLARE a variable called status to hold the return status of method calls;
        DECLARE a date/time variable called recentTime to hold the lastDiscoveredTime;
        DECLARE a date/time variable called currentLast to hold the individual call
                results;

        CALL getSubscriptions(NULL, lastModifiedTime)
                RETURNING status, subscriptions, and recentTime;
        IF status is failed THEN
            RETURN status;
        ENDIF;

        CALL getDocuments(NULL, NULL, NULL, lastDiscoveredTime)
                RETURNING status, documents, and currentLast;
        IF status is failed THEN
            RETURN status;
        ENDIF;

        IF currentLast is later than recentTime THEN
            SET recentTime to currentLast;
        ENDIF;

        CALL getLocalDocuments(NULL, NULL, lastDiscoveredTime)
                RETURNING status, local, and lastDiscoveredTime;
        IF status is failed THEN
            RETURN status;
        ENDIF;

        IF currentLast is later than recentTime THEN
            SET recentTime to currentLast;
        ENDIF;

	RETURN status of success, subscriptions, documents, local, and recentTime; 
    }

END;
```
