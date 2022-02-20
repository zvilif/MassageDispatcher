package com.zvil.md;

import io.jsonwebtoken.SignatureException;
import java.util.concurrent.ConcurrentHashMap;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;
import javax.persistence.TypedQuery;

/**
 * Root class that manages the root tables and starts everything.
 * @author Zvi Lifshitz
 */
public class Dispatcher {
    private static final String IMPROPER_INITIALIZATION = "Message dispatcher was not initialized properly";
    private static JwtService jwtService = null;
    private static EntityManager entityManager = null;
    private static EntityTransaction tx;
    private static final ConcurrentHashMap<Integer, Sender> senderMap = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Receiver> receiverMap = new ConcurrentHashMap<>();

    /**
     * Initializes the message repository including restoring from a persistent medium.
     * @param jwtSecret A secret string used by the dispatcher to initialize a JWT authentication service.
     * @param persistenceManager    The name of the implemented persistence provider. Can be null if persistence is not implemented.
     */
    public static void init(String jwtSecret, String persistenceManager) {
        jwtService = new JwtService(jwtSecret);
        if (persistenceManager != null) {
            restoreFromPersistence(persistenceManager);
            tx = entityManager.getTransaction();
            tx.begin();
        }
    }
    
    private static void restoreFromPersistence(String persistenceManager) {
        EntityManagerFactory emFactory = Persistence.createEntityManagerFactory(persistenceManager);
        entityManager = emFactory.createEntityManager();
        TypedQuery<Message> query = entityManager.createQuery("SELECT m FROM Message AS m ORDER BY m.serial", Message.class);
        query.getResultStream().forEach(m -> {
            Sender sender = createSender(m.getSenderID());
            Receiver receiver = createReceiver(m.getReceiverID());
            m.setSender(sender);
            m.setReceiver(receiver);
            sender.addMessage(m);
            receiver.addMessage(m);
        });
    }
    
    /**
     * Called by senders to create a sender object. The object may have been created already by
     * {@link #init(java.lang.String, java.lang.String) init()} in which case
     * the existing object is returned after setting the reception callback object in it.
     * @param jwt       An authentication token that encapsulates the sender ID
     * @param callback  A callback object for notifying reception events. Can be null if receipts are not required by this sender.
     * @return the created or existing Sender object.
     * @throws SignatureException       if the authentication token is not encoded properly
     * @throws IllegalStateException    if the message dispatcher was not initialized by a proper call to
     *                                  {@link #init(java.lang.String, java.lang.String) init()}.
     */
    public static Sender createSender(String jwt, ReceptionCallbackInterface callback)
        throws SignatureException, IllegalStateException
    {
        if (jwtService == null)
            throw new IllegalStateException(IMPROPER_INITIALIZATION);
        int userID = jwtService.decodeJWT(jwt);
        Sender sender = createSender(userID);
        if (callback != null)
            sender.setCallback(callback);
        return sender;
    }
    
    static Sender createSender(int userID) {
        return senderMap.computeIfAbsent(userID, id -> new Sender(id));
    }
    
    /**
     * Called by receivers to create a receiver object. The object may have been created already if messages have already been sent
     * to this receiver or during restore from persistence in {@link #init(java.lang.String, java.lang.String) init()}, in which case the
     * existing object is returned.
     * @param jwt       An authentication token that encapsulates the receiver ID
     * @return the created or existing Receiver object.
     * @throws SignatureException       if the authentication token is not encoded properly
     * @throws IllegalStateException    if the message dispatcher was not initialized by a proper call to
     *                                  {@link #init(java.lang.String, java.lang.String) init()}.
     */
    public static Receiver createReceiver(String jwt)
        throws SignatureException, IllegalStateException
    {
        if (jwtService == null)
            throw new IllegalStateException(IMPROPER_INITIALIZATION);
        int userID = jwtService.decodeJWT(jwt);
        return createReceiver(userID);
    }
    
    static Receiver createReceiver(int userID) {
        return receiverMap.computeIfAbsent(userID, id -> new Receiver(id));
    }
    
    /**
     * Persist a message
     * @param message 
     */
    static void persist(Message message) {
        if (entityManager != null) {
            entityManager.persist(message);
        }
    }
    
    /**
     * Remove a message from persistence medium
     * @param message 
     */
    static void remove(Message message) {
        if (entityManager != null)
            entityManager.remove(message);
    }
    
    /**
     * Cleanup all data.Useful for testing persistence (by calling {@link #cleanup()} and then {@link #init(java.lang.String, java.lang.String) init()} again.
     * <p>
     * Can be also used to test for memory leaks. If all sent messages were received then all maps and queues shall be empty when this
     * method is called.
     * @return true if and only if all message maps and queues are empty at the time of the calling.
     */
    public static boolean cleanup() {
        if (entityManager != null) {
            entityManager.getTransaction().commit();
            entityManager.close();
        }
        boolean result =
            senderMap.values().stream().allMatch(s -> s.cleanup()) &&
            receiverMap.values().stream().allMatch(s -> s.cleanup());
        senderMap.clear();
        receiverMap.clear();
        return result;
    }
}
