package com.zvil.md.test;

import com.zvil.md.Dispatcher;
import com.zvil.md.JwtService;
import com.zvil.md.Message;
import com.zvil.md.Receiver;
import com.zvil.md.ReceptionCallbackInterface;
import com.zvil.md.Sender;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test dispatcher operation (without persistence)
 * @author Zvi Lifshitz
 */
public class TestDispatcher {
    private static String jwtSecret;
    private static JwtService jwtService;
    private static final String SUBJECT = "Message subject ";
    private static final String JPA_MANAGER = "com.zvil_MessageDispatcher_persistence.2PU";
    
    public TestDispatcher() {
    }

    @BeforeAll
    public static void setUpClass() {
        jwtSecret = JwtService.generateKey(128);
        jwtService = new JwtService(jwtSecret);
    }
    
    @AfterAll
    public static void tearDownClass() {
    }
    
    @BeforeEach
    public void setUp() {
        Dispatcher.init(jwtSecret, JPA_MANAGER);
    }
    
    @AfterEach
    public void tearDown() {
        assertTrue(Dispatcher.cleanup());
    }

    private Sender createSender(int senderID, ReceptionCallbackInterface callback) {
        return Dispatcher.createSender(jwtService.createJWT(senderID), callback);
    }
    
    private Receiver createReceiver(int receiverID) {
        return Dispatcher.createReceiver(jwtService.createJWT(receiverID));
    }
    
    @Test
    public void testCreateUsers() {
        assertNotNull(createSender(101, null));
        assertNotNull(createReceiver(202));
    }
    
    @Test
    public void testBasicFlow() {
        int sender1 = 101;
        int receiver1 = 202;
        Sender sender = createSender(sender1, null);
        for (int i = 0; i < 10; i++) {
            Message msg = new Message();
            msg.setSubject(SUBJECT + i);
            sender.sendMessage(msg, receiver1, false);
        }
        Receiver receiver = createReceiver(receiver1);
        for (int i = 0; i < 10; i++) {
            Message msg = receiver.getNextMessage();
            assertEquals(msg.getSubject(), SUBJECT + i);
        }
        assertNull(receiver.getNextMessage());
    }

    @Test
    public void testRetrieveById() {
        int sender1 = 101;
        int receiver1 = 202;
        Sender sender = createSender(sender1, null);
        String msg5 = null;
        for (int i = 0; i < 10; i++) {
            Message msg = new Message();
            msg.setSubject(SUBJECT + i);
            String id = sender.sendMessage(msg, receiver1, false);
            if (i == 5)
                msg5 = id;
        }
        Receiver receiver = createReceiver(receiver1);
        Message msg = receiver.getMessage(msg5);
        assertEquals(msg.getSubject(), SUBJECT + 5);
        for (int i = 0; i < 10; i++) {
            if (i == 5)
                ++i;
            msg = receiver.getNextMessage();
            assertEquals(msg.getSubject(), SUBJECT + i);
        }
        assertNull(receiver.getNextMessage());
    }

    @Test
    public void testCheckIfStillAwaiting() {
        int sender1 = 101;
        int receiver1 = 202;
        Sender sender = createSender(sender1, null);
        String msg5 = null;
        for (int i = 0; i < 10; i++) {
            Message msg = new Message();
            msg.setSubject(SUBJECT + i);
            String id = sender.sendMessage(msg, receiver1, false);
            if (i == 5)
                msg5 = id;
        }
        Receiver receiver = createReceiver(receiver1);
        Message msg;
        for (int i = 0; i < 5; i++) {
            msg = receiver.getNextMessage();
            assertEquals(msg.getSubject(), SUBJECT + i);
        }
        assertTrue(sender.isAwaiting(msg5));
        for (int i = 5; i < 10; i++) {
            msg = receiver.getNextMessage();
            assertEquals(msg.getSubject(), SUBJECT + i);
        }
        assertFalse(sender.isAwaiting(msg5));
        assertNull(receiver.getNextMessage());
    }

    @Test
    public void testRemoveMessage() {
        int sender1 = 101;
        int receiver1 = 202;
        Sender sender = createSender(sender1, null);
        String msg5 = null;
        for (int i = 0; i < 10; i++) {
            Message msg = new Message();
            msg.setSubject(SUBJECT + i);
            String id = sender.sendMessage(msg, receiver1, false);
            if (i == 5)
                msg5 = id;
        }
        Receiver receiver = createReceiver(receiver1);
        Message msg;
        for (int i = 0; i < 5; i++) {
            msg = receiver.getNextMessage();
            assertEquals(msg.getSubject(), SUBJECT + i);
        }
        sender.removeMessage(msg5);
        for (int i = 6; i < 10; i++) {
            msg = receiver.getNextMessage();
            assertEquals(msg.getSubject(), SUBJECT + i);
        }
        assertNull(receiver.getNextMessage());
    }

    private int nextNotification = 0;
    @Test
    public void testNotifyReception() {
        // Test that every other message is sent with a request for receipt.
        int sender1 = 101;
        int receiver1 = 202;
        Sender sender = createSender(sender1, (Message msg) -> {
            assertEquals(msg.getSubject(), SUBJECT + nextNotification);
            nextNotification += 2;
        });
        boolean notify = true;
        for (int i = 0; i < 10; i++) {
            Message msg = new Message();
            msg.setSubject(SUBJECT + i);
            sender.sendMessage(msg, receiver1, notify);
            notify = !notify;
        }
        Receiver receiver = createReceiver(receiver1);
        for (int i = 0; i < 10; i++) {
            Message msg = receiver.getNextMessage();
            assertEquals(msg.getSubject(), SUBJECT + i);
        }
        assertNull(receiver.getNextMessage());
        assertTrue(nextNotification >= 10);
    }

    @Test
    public void testMultipleSendersAndReceivers() {
        int S1 = 101;
        int R1 = 201;
        int SENDERS = 4;
        int RECEIVERS = 3;
        Sender[] senders = new Sender[SENDERS];
        Receiver[] receivers = new Receiver[RECEIVERS];
        for (int i = 0; i < SENDERS; i++)
            senders[i] = createSender(S1 + i, null);
        for (int i = 0; i < 100; i++) {
            int ns = i % SENDERS;
            int nr = i % RECEIVERS;
            Message msg = new Message();
            msg.setSubject(SUBJECT + i + " from " + S1 + ns);
            senders[ns].sendMessage(msg, R1 + nr, false);
        }
        for (int i = 0; i < RECEIVERS; i++)
            receivers[i] = createReceiver(R1 + i);
        Message msg;
        for (int i = 0; i < 100; i++) {
            int ns = i % SENDERS;
            int nr = i % RECEIVERS;
            msg = receivers[nr].getNextMessage();
            assertEquals(msg.getSubject(), SUBJECT + i + " from " + S1 + ns);
        }
    }
    
    @Test
    public void testPersistence() {
        int S1 = 101;
        int R1 = 201;
        int SENDERS = 4;
        int RECEIVERS = 3;
        Sender[] senders = new Sender[SENDERS];
        Receiver[] receivers = new Receiver[RECEIVERS];
        for (int i = 0; i < SENDERS; i++)
            senders[i] = createSender(S1 + i, null);
        for (int i = 0; i < 100; i++) {
            int ns = i % SENDERS;
            int nr = i % RECEIVERS;
            Message msg = new Message();
            msg.setSubject(SUBJECT + i + " from " + S1 + ns);
            senders[ns].sendMessage(msg, R1 + nr, false);
        }

        Dispatcher.cleanup();
        Dispatcher.init(jwtSecret, JPA_MANAGER);

        for (int i = 0; i < RECEIVERS; i++)
            receivers[i] = createReceiver(R1 + i);
        Message msg;
        for (int i = 0; i < 100; i++) {
            int ns = i % SENDERS;
            int nr = i % RECEIVERS;
            msg = receivers[nr].getNextMessage();
            assertEquals(msg.getSubject(), SUBJECT + i + " from " + S1 + ns);
        }
    }
}
