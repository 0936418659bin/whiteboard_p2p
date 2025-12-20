package com.whiteboard.network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PeerDiscovery {
    private static final String BROADCAST_ADDRESS = "192.168.1.99";
    private static final int BROADCAST_PORT = 55556;

    private final String peerId;
    private final String peerName;
    private final int listenPort;
    private final String roomId;
    private final String roomPassword;
    private DatagramSocket broadcastSocket;
    private Thread discoveryThread;
    private Thread listeningThread;
    private boolean isDiscovering;
    private final Map<String, NetworkProtocol.PeerInfo> discoveredPeers;
    private Consumer<NetworkProtocol.PeerInfo> onPeerFound;
    private Consumer<NetworkProtocol.PeerInfo> onPeerLost;

    public PeerDiscovery(String peerId, String peerName, int listenPort, String roomId, String roomPassword) {
        this.peerId = peerId;
        this.peerName = peerName;
        this.listenPort = listenPort;
        this.roomId = roomId;
        this.roomPassword = roomPassword;
        this.discoveredPeers = new ConcurrentHashMap<>();
        this.isDiscovering = false;
    }

    public void start() throws IOException {
        if (isDiscovering)
            return;
        isDiscovering = true;

        broadcastSocket = new DatagramSocket();
        broadcastSocket.setBroadcast(true);

        discoveryThread = new Thread(this::broadcastPresence);
        discoveryThread.setDaemon(true);
        discoveryThread.setName("Discovery-Broadcast");
        discoveryThread.start();

        listeningThread = new Thread(this::listenForPeers);
        listeningThread.setDaemon(true);
        listeningThread.setName("Discovery-Listen");
        listeningThread.start();
    }

    private void broadcastPresence() {
        try {
            String messageStr = peerId + "|" + peerName + "|" + listenPort + "|" +
                    (roomId == null ? "" : roomId) + "|" + (roomPassword == null ? "" : roomPassword);
            byte[] message = messageStr.getBytes();

            while (isDiscovering) {
                try {
                    DatagramPacket packet = new DatagramPacket(
                            message, message.length,
                            InetAddress.getByName(BROADCAST_ADDRESS), BROADCAST_PORT);
                    System.out.println("[PeerDiscovery] Broadcasting presence: peerId=" + peerId
                            + ", name=" + peerName
                            + ", listenPort=" + listenPort
                            + ", roomId=" + (roomId == null ? "" : roomId)
                            + ", roomPassword=" + (roomPassword == null || roomPassword.isEmpty() ? "(empty)" : "***"));
                    broadcastSocket.send(packet);
                    Thread.sleep(3000); // Broadcast every 3 seconds
                } catch (IOException e) {
                    System.err.println("Error broadcasting presence: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("Broadcast error: " + e.getMessage());
        }
    }

    private void listenForPeers() {
        DatagramSocket socket = null;
        try {
            try {
                socket = new DatagramSocket(BROADCAST_PORT);
                socket.setBroadcast(true);
                System.out.println("[PeerDiscovery] Started listening on UDP port " + BROADCAST_PORT
                        + " for room=" + (roomId == null ? "" : roomId));
            } catch (BindException be) {
                // Trên một máy chỉ cần một tiến trình listen UDP; các tiến trình khác vẫn có
                // thể broadcast.
                System.err.println("[PeerDiscovery] Listen socket already in use on port " + BROADCAST_PORT
                        + ", skip listening in this instance.");
                return;
            }

            byte[] buffer = new byte[1024];

            while (isDiscovering) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    System.out
                            .println("[PeerDiscovery] Received UDP packet from " + packet.getAddress().getHostAddress()
                                    + ":" + packet.getPort() + ", length=" + packet.getLength());

                    String data = new String(packet.getData(), 0, packet.getLength());
                    String[] parts = data.split("\\|");

                    if (parts.length >= 3) {
                        String discoveredPeerId = parts[0];
                        String discoveredName = parts[1];
                        int discoveredPort = Integer.parseInt(parts[2]);
                        String discoveredRoom = parts.length >= 4 ? parts[3] : "";
                        String discoveredPass = parts.length >= 5 ? parts[4] : "";
                        String ipAddress = packet.getAddress().getHostAddress();

                        if (!discoveredPeerId.equals(peerId)) {
                            // chỉ quan tâm peers cùng phòng (roomId) và cùng mật khẩu nếu có đặt
                            if (roomId != null && !roomId.isEmpty() && !roomId.equals(discoveredRoom)) {
                                System.out.println("[PeerDiscovery] Ignored peer " + discoveredPeerId
                                        + " from IP " + ipAddress
                                        + " vì room khác. LocalRoom=" + roomId
                                        + ", RemoteRoom=" + discoveredRoom);
                                continue;
                            }
                            if (roomPassword != null && !roomPassword.isEmpty()
                                    && !roomPassword.equals(discoveredPass)) {
                                System.out.println("[PeerDiscovery] Ignored peer " + discoveredPeerId
                                        + " from IP " + ipAddress
                                        + " vì sai mật khẩu phòng.");
                                continue;
                            }

                            NetworkProtocol.PeerInfo peerInfo = new NetworkProtocol.PeerInfo(discoveredPeerId,
                                    discoveredName, ipAddress, discoveredRoom, discoveredPort);

                            NetworkProtocol.PeerInfo existing = discoveredPeers.get(discoveredPeerId);
                            if (existing == null) {
                                discoveredPeers.put(discoveredPeerId, peerInfo);
                                System.out.println("[PeerDiscovery] Found peer: " + peerInfo
                                        + " (ip=" + ipAddress + ", port=" + discoveredPort + ")");
                                if (onPeerFound != null) {
                                    onPeerFound.accept(peerInfo);
                                }
                            } else {
                                existing.lastSeen = System.currentTimeMillis();
                                existing.name = discoveredName;
                                existing.roomId = discoveredRoom;
                                existing.port = discoveredPort;
                                System.out.println("[PeerDiscovery] Updated peer: " + existing);
                            }
                        }
                    }
                } catch (IOException e) {
                    if (isDiscovering) {
                        System.err.println("Error listening for peers: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Listen error: " + e.getMessage());
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public void stop() {
        isDiscovering = false;
        if (broadcastSocket != null)
            broadcastSocket.close();
    }

    public Collection<NetworkProtocol.PeerInfo> getDiscoveredPeers() {
        return new ArrayList<>(discoveredPeers.values());
    }

    public void setPeerFoundCallback(Consumer<NetworkProtocol.PeerInfo> callback) {
        this.onPeerFound = callback;
    }

    public void setPeerLostCallback(Consumer<NetworkProtocol.PeerInfo> callback) {
        this.onPeerLost = callback;
    }
}
