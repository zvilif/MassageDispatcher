package com.zvil.md;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.MapKeyColumn;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * Objects of this class represent messages and their associated metadata.
 * @author Zvi Lifshitz
 */
@Entity
@Table(name="messages")
public class Message implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "serial", updatable = false, nullable = false)
    private long serial;
    
    private String messageID;
    private int senderID;
    private int receiverID;
    private boolean needsReceipt;
    private String subject;

    @ElementCollection
    @CollectionTable(name = "message_params", joinColumns = {@JoinColumn(name = "msg_serial", referencedColumnName = "serial")})
    @MapKeyColumn(name = "param_key")
    @Column(name = "param_value")
    private final Map<String, String> params;

    @Transient private Sender sender;
    @Transient private Receiver receiver;

    public Message() {
        params = new HashMap<>();
    }
    
    // Non public methods

    long getSerial() {
        return serial;
    }

    void setSerial(long serial) {    
        this.serial = serial;
    }

    void setMessageID(String messageID) {
        this.messageID = messageID;
    }

    int getSenderID() {
        return senderID;
    }

    int getReceiverID() {
        return receiverID;
    }

    void setSenderID(int senderID) {
        this.senderID = senderID;
    }

    void setReceiverID(int receiverID) {
        this.receiverID = receiverID;
    }

    Sender getSender() {
        return sender;
    }

    void setSender(Sender sender) {
        this.sender = sender;
        senderID = sender.getSenderID();
    }

    Receiver getReceiver() {
        return receiver;
    }

    void setReceiver(Receiver receiver) {
        this.receiver = receiver;
        receiverID = receiver.getReceiverID();
    }

    boolean isNeedsReceipt() {
        return needsReceipt;
    }

    void setNeedsReceipt(boolean needsReceipt) {
        this.needsReceipt = needsReceipt;
    }
    
    // Public methods

    public String getMessageID() {
        return messageID;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
    
    /**
     * Add a parameter to the parameter table
     * @param key   key
     * @param value value
     */
    public void putParam(String key, String value) {
        params.put(key, value);
    }
    
    /**
     * Get the value of the parameter with the given key.
     * @param key   key
     * @return      the value associated with the key or null if none exists.
     */
    public String getParam(String key) {
        return params.get(key);
    }

    /**
     * Get the parameter table
     * @return the parameter table
     */
    public Map<String, String> getParams() {
        return params;
    }
    
    /**
     * A static method that generates a unique message ID. In this implementation we use a random UUID. To guarantee 100%
     * uniqueness other methods may be preferable, such as concatenating current timestamp with a rotating integer value.
     * @return the generated message ID.
     */
    static String generateMessageID() {
        return UUID.randomUUID().toString();
    }
}
