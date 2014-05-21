package org.haox.kerb;

import junit.framework.Assert;
import org.haox.kerb.dispatch.AsyncDispatcher;
import org.haox.kerb.event.Event;
import org.haox.kerb.event.EventType;
import org.haox.kerb.event.MessageEvent;
import org.haox.kerb.handler.*;
import org.haox.kerb.message.Message;
import org.haox.kerb.transport.Transport;
import org.haox.kerb.transport.accept.Acceptor;
import org.haox.kerb.transport.accept.UdpAcceptor;
import org.haox.kerb.transport.connect.Connector;
import org.haox.kerb.transport.connect.UdpConnector;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public class TestTransport {
    private String serverHost = "127.0.0.1";
    private short serverPort = 8181;

    private AsyncDispatcher serverDispatcher;
    private Acceptor acceptor;
    private Connector connector;
    private AsyncDispatcher clientDispatcher;

    private String TEST_MESSAGE = "Hello world!";
    private String clientRecvedMessage;

    @Before
    public void setUp() throws IOException, InterruptedException {
        setUpServerSide();
        Thread.sleep(1000);
        setUpClientSide();
    }

    private void setUpServerSide() {
        serverDispatcher = new AsyncDispatcher();
        serverDispatcher.start();

        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void process(Event event) {
                MessageEvent msgEvent = (MessageEvent) event;
                if (msgEvent.getEventType() == EventType.NEW_INBOUND_MESSAGE) {
                    msgEvent.getTransport().postMessage(msgEvent.getMessage());
                } else if (msgEvent.getEventType() == EventType.NEW_OUTBOUND_MESSAGE) {
                    try {
                        msgEvent.getTransport().sendMessage(msgEvent.getMessage());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        serverDispatcher.register(new AsyncMessageHandler(messageHandler));

        acceptor = new UdpAcceptor();
        serverDispatcher.register(acceptor);

        acceptor.listen(serverHost, serverPort);
    }

    private void setUpClientSide() throws IOException {
        clientDispatcher = new AsyncDispatcher();
        clientDispatcher.start();

        MessageHandler messageHandler = new MessageHandler() {
            @Override
            public void process(Event event) {
                MessageEvent msgEvent = (MessageEvent) event;
                if (msgEvent.getEventType() == EventType.NEW_INBOUND_MESSAGE) {
                    synchronized (TestTransport.this) {
                        clientRecvedMessage = new String(msgEvent.getMessage().getContent().array());
                        System.out.println("Recved clientRecvedMessage: " + clientRecvedMessage);
                    }
                } else if (msgEvent.getEventType() == EventType.NEW_OUTBOUND_MESSAGE) {
                    try {
                        msgEvent.getTransport().sendMessage(msgEvent.getMessage());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        clientDispatcher.register(new AsyncMessageHandler(messageHandler));

        connector = new UdpConnector();
        clientDispatcher.register(connector);
        TransportHandler transportHandler = new TransportHandler() {
            @Override
            protected void onNewTransport(Transport transport) {
                transport.postMessage(new Message(ByteBuffer.wrap(TEST_MESSAGE.getBytes())));
            }
        };
        clientDispatcher.register(new AsyncTransportHandler(transportHandler));

        connector.connect(serverHost, serverPort);
    }

    @Test
    public void testUdpTransport() throws IOException, InterruptedException {
        while (true) {
            synchronized (this) {
                if (clientRecvedMessage == null) {
                    Thread.sleep(1000);
                } else {
                    System.out.println("Got clientRecvedMessage: " + clientRecvedMessage);
                    break;
                }
            }
        }
        Assert.assertEquals(TEST_MESSAGE, clientRecvedMessage);
    }

    @After
    public void cleanUp() {
        acceptor.stop();
        connector.stop();
        serverDispatcher.stop();
        clientDispatcher.stop();
    }
}
