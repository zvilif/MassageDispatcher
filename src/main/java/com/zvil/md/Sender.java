package com.zvil.md;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Each sender instantiates an object of this class and uses it to send messages and other sender operations.
 * @author Zvi Lifshitz
 */
public class Sender {
    private final int senderID;
    private ReceptionCallbackInterface callback = null;
    private final ConcurrentHashMap<String, Message> messageMap;

    /**
     * The class constructor (internal) receives a sender ID.
     * @param senderID  Sender ID
     */
    Sender(int senderID) {
        this.senderID = senderID;
        messageMap = new ConcurrentHashMap<>();
    }

    void setCallback(ReceptionCallbackInterface callback) {
        this.callback = callback;
    }

    int getSenderID() {
        return senderID;
    }
    
    /**
     * Send a message to the given receiver.
     * @param message       The message to send
     * @param receiverID    Receiver ID
     * @param needsReceipt  set to true if you want to get notified (through the callback object provided when calling
     * {@link Dispatcher#createSender(java.lang.String, com.zvil.md.ReceptionCallbackInterface) Dispatcher.createSender()}.
     * @return the ID of the sent message.
     */
    public String sendMessage(Message message, int receiverID, boolean needsReceipt) {
        String id = Message.generateMessageID();
        message.setMessageID(id);
        message.setSender(this);
        Receiver receiver = Dispatcher.createReceiver(receiverID);
        message.setReceiver(receiver);
        message.setNeedsReceipt(needsReceipt);
        addMessage(message);
        receiver.sendMessage(message);
        return id;
    }
    
    /**
     * Check if the message with the given ID is still waiting at the dispatcher.
     * @param messageID message ID
     * @return          true if and only if the message is still awaiting.
     */
    public boolean isAwaiting(String messageID) {
        return messageMap.get(messageID) != null;
    }
    
    /**
     * Remove the message with the given ID from the dispatcher.
     * @param messageID message ID
     * @return  true if and only if the message was still awaiting at the dispatcher.
     */
    public boolean removeMessage(String messageID) {
        Message message = deleteMessage(messageID);
        if (message == null)
            return false;
        message.getReceiver().deleteMessage(message);
        return true;
    }
    
    /**
     * Add a message to the message map
     * @param message
     */
    void addMessage(Message message) {
        messageMap.put(message.getMessageID(), message);
    }
    
    /**
     * Remove a message from the message table
     * @param messageID message ID
     * @return the removed message or null if the message is not in the map
     */
    Message deleteMessage(String messageID) {
        return messageMap.remove(messageID);
    }
    
    /**
     * Called when a message is retrieved by a receiver. Delete it from the message map.
     * If necessary use the callback object to notify the sender.
     * @param message 
     */
    void retreivedMessage(Message message) {
        deleteMessage(message.getMessageID());
        if (message.isNeedsReceipt() && callback != null)
            callback.receptionEvent(message);
    }
    
    /**
     * clear the message map
     * @return true if the map was empty at the time of the request
     */
    boolean cleanup() {
        boolean result = messageMap.isEmpty();
        messageMap.clear();
        return result;
    }
}
