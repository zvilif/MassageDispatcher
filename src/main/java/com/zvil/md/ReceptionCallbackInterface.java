package com.zvil.md;

/**
 * Senders implement reception event callback by implementing this interface.
 * @author Zvi Lifshitz
 */
public interface ReceptionCallbackInterface {

    /**
     * Implement this method to do something when a message reception event is notified.
     * @param message   the received message
     */
    void receptionEvent(Message message);
}
