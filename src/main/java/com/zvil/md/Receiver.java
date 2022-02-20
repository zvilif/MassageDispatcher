package com.zvil.md;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Receivers use this class to receive messages and other receiver operations.
 * @author Zvi Lifshitz
 */
public class Receiver {
    private final int ReceiverID;
    private final ConcurrentHashMap<String, Message> messageMap;
    private final ConcurrentLinkedQueue<Message> messageQueue;

    Receiver(int ReceiverID) {
        this.ReceiverID = ReceiverID;
        messageMap = new ConcurrentHashMap<>();
        messageQueue = new ConcurrentLinkedQueue<>();
    }

    int getReceiverID() {
        return ReceiverID;
    }
    
    /**
     * Get the message at the head of the message queue and remove it from the queue
     * @return the head message or null if the message queue is empty.
     */
    public Message getNextMessage() {
        Message message = messageQueue.poll();
        if (message != null) {
            messageMap.remove(message.getMessageID());
            message.getSender().retreivedMessage(message);
            Dispatcher.remove(message);
        }
        return message;
    }
    
    /**
     * Get a specific message and remove it from the message map and message queue
     * @param messageID the message ID
     * @return the retrieved message or null if the message does not exist.
     */
    public Message getMessage(String messageID) {
        Message message = messageMap.remove(messageID);
        if (message != null) {
            messageQueue.remove(message);
            message.getSender().retreivedMessage(message);
            Dispatcher.remove(message);
        }
        return message;
    }
    
    /**
     * Add a newly sent message
     * @param message 
     */
    void sendMessage(Message message) {
        addMessage(message);
        Dispatcher.persist(message);
    }
    
    /**
     * Add a message to the message map and message queue.
     * @param message 
     */
    void addMessage(Message message) {
        messageMap.put(message.getMessageID(), message);
        messageQueue.add(message);
    }
    
    /**
     * Remove a message, if exists, from the message map and message queue.
     * @param message
     */
    void deleteMessage(Message message) {
        messageMap.remove(message.getMessageID());
        messageQueue.remove(message);
        Dispatcher.remove(message);
    }

    /**
     * clear the message map and queue
     * @return true if the table was empty at the time of the request
     */
    boolean cleanup() {
        boolean result = messageMap.isEmpty() && messageQueue.isEmpty();
        messageMap.clear();
        messageQueue.clear();
        return result;
    }
}
