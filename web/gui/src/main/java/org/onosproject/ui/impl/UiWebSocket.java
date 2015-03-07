/*
 * Copyright 2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.eclipse.jetty.websocket.WebSocket;
import org.onlab.osgi.ServiceDirectory;
import org.onosproject.ui.UiConnection;
import org.onosproject.ui.UiExtensionService;
import org.onosproject.ui.UiMessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Web socket capable of interacting with the GUI.
 */
public class UiWebSocket
        implements UiConnection, WebSocket.OnTextMessage, WebSocket.OnControl {

    private static final Logger log = LoggerFactory.getLogger(UiWebSocket.class);

    private static final long MAX_AGE_MS = 15000;

    private static final byte PING = 0x9;
    private static final byte PONG = 0xA;
    private static final byte[] PING_DATA = new byte[]{(byte) 0xde, (byte) 0xad};

    private final ServiceDirectory directory;

    private Connection connection;
    private FrameConnection control;

    private final ObjectMapper mapper = new ObjectMapper();

    private long lastActive = System.currentTimeMillis();

    private Map<String, UiMessageHandler> handlers;

    /**
     * Creates a new web-socket for serving data to GUI.
     *
     * @param directory service directory
     */
    public UiWebSocket(ServiceDirectory directory) {
        this.directory = directory;
    }

    /**
     * Issues a close on the connection.
     */
    synchronized void close() {
        destroyHandlers();
        if (connection.isOpen()) {
            connection.close();
        }
    }

    /**
     * Indicates if this connection is idle.
     *
     * @return true if idle or closed
     */
    synchronized boolean isIdle() {
        boolean idle = (System.currentTimeMillis() - lastActive) > MAX_AGE_MS;
        if (idle || (connection != null && !connection.isOpen())) {
            return true;
        } else if (connection != null) {
            try {
                control.sendControl(PING, PING_DATA, 0, PING_DATA.length);
            } catch (IOException e) {
                log.warn("Unable to send ping message due to: ", e);
            }
        }
        return false;
    }

    @Override
    public void onOpen(Connection connection) {
        log.info("GUI client connected");
        this.connection = connection;
        this.control = (FrameConnection) connection;
        createHandlers();
    }

    @Override
    public synchronized void onClose(int closeCode, String message) {
        destroyHandlers();
        log.info("GUI client disconnected");
    }

    @Override
    public boolean onControl(byte controlCode, byte[] data, int offset, int length) {
        lastActive = System.currentTimeMillis();
        return true;
    }

    @Override
    public void onMessage(String data) {
        lastActive = System.currentTimeMillis();
        try {
            ObjectNode message = (ObjectNode) mapper.reader().readTree(data);
            String type = message.path("type").asText("unknown");
            UiMessageHandler handler = handlers.get(type);
            if (handler != null) {
                handler.process(message);
            } else {
                log.warn("No GUI message handler for type {}", type);
            }
        } catch (Exception e) {
            log.warn("Unable to parse GUI message {} due to {}", data, e);
            log.debug("Boom!!!", e);
        }
    }

    @Override
    public void sendMessage(ObjectNode message) {
        try {
            if (connection.isOpen()) {
                connection.sendMessage(message.toString());
            }
        } catch (IOException e) {
            log.warn("Unable to send message {} to GUI due to {}", message, e);
            log.debug("Boom!!!", e);
        }
    }

    // Creates new message handlers.
    private void createHandlers() {
        handlers = new HashMap<>();
        UiExtensionService service = directory.get(UiExtensionService.class);
        service.getExtensions().forEach(ext -> ext.messageHandlerFactory().newHandlers().forEach(handler -> {
            handler.init(this, directory);
            handler.messageTypes().forEach(type -> handlers.put(type, handler));
        }));
    }

    // Destroys message handlers.
    private synchronized void destroyHandlers() {
        handlers.forEach((type, handler) -> handler.destroy());
        handlers.clear();
    }
}

