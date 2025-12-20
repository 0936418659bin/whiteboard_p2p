package com.whiteboard.network;

import java.io.Serializable;
import java.util.List;
import com.whiteboard.drawing.Shape;

public class NetworkProtocol {
    public enum MessageType {
        HELLO, // Initial connection
        SHAPES, // Drawing data
        ACK, // Acknowledgment
        SYNC_REQUEST, // Request full state
        SYNC_RESPONSE, // Full state response
        CLEAR, // Clear canvas
        UNDO, // Undo action
        PEER_LIST, // List of connected peers
        DISCONNECT // Peer disconnecting
    }

    public static class Message implements Serializable {
        public MessageType type;
        public String senderId;
        public long timestamp;
        public Object data;

        public Message(MessageType type, String senderId, Object data) {
            this.type = type;
            this.senderId = senderId;
            this.timestamp = System.currentTimeMillis();
            this.data = data;
        }

        @Override
        public String toString() {
            return String.format("Message{type=%s, sender=%s, timestamp=%d}",
                    type, senderId, timestamp);
        }
    }

    public static class ShapeData implements Serializable {
        public List<Shape> shapes;
        public long version;

        public ShapeData(List<Shape> shapes, long version) {
            this.shapes = shapes;
            this.version = version;
        }
    }

    public static class PeerInfo implements Serializable {
        public String peerId;
        public String name;
        public String ipAddress;
        public String roomId;
        public String roomPassword;
        public long lastSeen;
        public boolean isActive;
        public int port;

        public PeerInfo(String peerId, String name, String ipAddress) {
            this(peerId, name, ipAddress, null, 0);
        }

        public PeerInfo(String peerId, String name, String ipAddress, int port) {
            this(peerId, name, ipAddress, null, port);
        }

        public PeerInfo(String peerId, String name, String ipAddress, String roomId, int port) {
            this.peerId = peerId;
            this.name = name;
            this.ipAddress = ipAddress;
            this.roomId = roomId;
            this.port = port;
            this.lastSeen = System.currentTimeMillis();
            this.isActive = true;
        }

        @Override
        public String toString() {
            return String.format("%s (%s) - %s", name, peerId, ipAddress);
        }
    }
}
