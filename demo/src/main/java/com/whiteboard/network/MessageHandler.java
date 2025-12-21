package com.whiteboard.network;

import com.whiteboard.drawing.Shape;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class MessageHandler {
    private final Map<String, PeerConnection> connections;
    private Consumer<NetworkProtocol.ShapeData> onShapesReceived;
    private Consumer<String> onClearReceived;
    private Consumer<String> onUndoReceived;
    private Consumer<List<NetworkProtocol.PeerInfo>> onPeerListReceived;
    private Consumer<String> onDisconnectReceived;

    public MessageHandler() {
        this.connections = new ConcurrentHashMap<>();
    }

    public void addConnection(PeerConnection connection) {
        connections.put(connection.getPeerId(), connection);
        connection.setMessageHandler(this::handleMessage);
    }

    public void removeConnection(String peerId) {
        PeerConnection conn = connections.remove(peerId);
        if (conn != null) {
            conn.disconnect();
        }
    }

    private void handleMessage(NetworkProtocol.Message message) {
        switch (message.type) {
            case HELLO:
                handleHello(message);
                break;
            case SHAPES:
                if (onShapesReceived != null && message.data instanceof NetworkProtocol.ShapeData) {
                    onShapesReceived.accept((NetworkProtocol.ShapeData) message.data);
                }
                break;
            case CLEAR:
                if (onClearReceived != null) {
                    onClearReceived.accept(message.senderId);
                }
                break;
            case UNDO:
                if (onUndoReceived != null) {
                    onUndoReceived.accept(message.senderId);
                }
                break;
            case PEER_LIST:
                if (onPeerListReceived != null && message.data instanceof List) {
                    onPeerListReceived.accept((List<NetworkProtocol.PeerInfo>) message.data);
                }
                break;
            case ACK:
                // Handle acknowledgment if needed
                break;
            case SYNC_REQUEST:
                // Handle sync request
                break;
            case SYNC_RESPONSE:
                // Handle sync response
                break;
            case DISCONNECT:
                if (onDisconnectReceived != null) {
                    String reason = (message.data instanceof String) ? (String) message.data : "";
                    System.out.println("[MessageHandler] Received DISCONNECT message from " 
                            + message.senderId + " with reason: " + reason);
                    onDisconnectReceived.accept(reason);
                } else {
                    System.err.println("[MessageHandler] DISCONNECT received but no callback registered!");
                }
                break;
        }
    }

    private void handleHello(NetworkProtocol.Message message) {
        System.out.println("[MessageHandler] Peer connected: " + message.senderId);
        // Khi nhận HELLO, có thể cập nhật peerId nếu cần (hiện tại đã match từ UDP discovery)
    }

    public void broadcastShapes(List<Shape> shapes, String senderId) throws Exception {
        // Sao chép sang ArrayList để tránh gửi trực tiếp subList (ArrayList$SubList
        // không tuần tự hoá được, gây NotSerializableException)
        List<Shape> payload = new ArrayList<>(shapes);
        NetworkProtocol.ShapeData data = new NetworkProtocol.ShapeData(payload, System.currentTimeMillis());
        NetworkProtocol.Message message = new NetworkProtocol.Message(
                NetworkProtocol.MessageType.SHAPES, senderId, data);

        for (PeerConnection connection : connections.values()) {
            if (!connection.getPeerId().equals(senderId)) {
                try {
                    connection.sendMessage(message);
                } catch (Exception e) {
                    System.err.println("Failed to send shapes to " + connection.getPeerId());
                }
            }
        }
    }

    public void broadcastClear(String senderId) throws Exception {
        NetworkProtocol.Message message = new NetworkProtocol.Message(
                NetworkProtocol.MessageType.CLEAR, senderId, null);

        for (PeerConnection connection : connections.values()) {
            if (!connection.getPeerId().equals(senderId)) {
                try {
                    connection.sendMessage(message);
                } catch (Exception e) {
                    System.err.println("Failed to send clear to " + connection.getPeerId());
                }
            }
        }
    }

    public void broadcastUndo(String senderId) throws Exception {
        NetworkProtocol.Message message = new NetworkProtocol.Message(
                NetworkProtocol.MessageType.UNDO, senderId, null);

        for (PeerConnection connection : connections.values()) {
            if (!connection.getPeerId().equals(senderId)) {
                try {
                    connection.sendMessage(message);
                } catch (Exception e) {
                    System.err.println("Failed to send undo to " + connection.getPeerId());
                }
            }
        }
    }

    public void setOnShapesReceived(Consumer<NetworkProtocol.ShapeData> callback) {
        this.onShapesReceived = callback;
    }

    public void setOnClearReceived(Consumer<String> callback) {
        this.onClearReceived = callback;
    }

    public void setOnUndoReceived(Consumer<String> callback) {
        this.onUndoReceived = callback;
    }

    public void setOnPeerListReceived(Consumer<List<NetworkProtocol.PeerInfo>> callback) {
        this.onPeerListReceived = callback;
    }

    public void setOnDisconnectReceived(Consumer<String> callback) {
        this.onDisconnectReceived = callback;
    }

    public Collection<PeerConnection> getConnections() {
        return new ArrayList<>(connections.values());
    }

    public PeerConnection getConnection(String peerId) {
        return connections.get(peerId);
    }

    public int getConnectionCount() {
        return connections.size();
    }

    public void closeAll() {
        for (PeerConnection connection : connections.values()) {
            connection.disconnect();
        }
        connections.clear();
    }
}
