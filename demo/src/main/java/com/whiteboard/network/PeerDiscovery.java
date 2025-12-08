package com.whiteboard.network;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class PeerDiscovery {
    private static final String BROADCAST_ADDRESS = "255.255.255.255";
    private static final int BROADCAST_PORT = 55556;

    private final String peerId;
    private final String peerName;
    private final int listenPort;
    private DatagramSocket broadcastSocket;
    private Thread discoveryThread;
    private Thread listeningThread;
    private boolean isDiscovering;
    private final Map<String, NetworkProtocol.PeerInfo> discoveredPeers;
    private Consumer<NetworkProtocol.PeerInfo> onPeerFound;
    private Consumer<NetworkProtocol.PeerInfo> onPeerLost;

    public PeerDiscovery(String peerId, String peerName, int listenPort) {
        this.peerId = peerId;
        this.peerName = peerName;
        this.listenPort = listenPort;
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
            String messageStr = peerId + "|" + peerName + "|" + listenPort;
            byte[] message = messageStr.getBytes();

            while (isDiscovering) {
                try {
                    DatagramPacket packet = new DatagramPacket(
                            message, message.length,
                            InetAddress.getByName(BROADCAST_ADDRESS), BROADCAST_PORT);
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
        try {
            DatagramSocket socket = new DatagramSocket(BROADCAST_PORT);
            socket.setBroadcast(true);

            byte[] buffer = new byte[1024];

            while (isDiscovering) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String data = new String(packet.getData(), 0, packet.getLength());
                    String[] parts = data.split("\\|");

                    if (parts.length == 3) {
                        String discoveredPeerId = parts[0];
                        String discoveredName = parts[1];
                        int discoveredPort = Integer.parseInt(parts[2]);
                        String ipAddress = packet.getAddress().getHostAddress();

                        if (!discoveredPeerId.equals(peerId)) {
                            NetworkProtocol.PeerInfo peerInfo = new NetworkProtocol.PeerInfo(discoveredPeerId,
                                    discoveredName, ipAddress, discoveredPort);

                            if (!discoveredPeers.containsKey(discoveredPeerId)) {
                                discoveredPeers.put(discoveredPeerId, peerInfo);
                                if (onPeerFound != null) {
                                    onPeerFound.accept(peerInfo);
                                }
                            } else {
                                discoveredPeers.get(discoveredPeerId).lastSeen = System.currentTimeMillis();
                            }
                        }
                    }
                } catch (IOException e) {
                    if (isDiscovering) {
                        System.err.println("Error listening for peers: " + e.getMessage());
                    }
                }
            }
            socket.close();
        } catch (Exception e) {
            System.err.println("Listen error: " + e.getMessage());
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
