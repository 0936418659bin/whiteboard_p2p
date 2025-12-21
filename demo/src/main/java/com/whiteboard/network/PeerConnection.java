package com.whiteboard.network;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class PeerConnection {
    private final Socket socket;
    private final String peerId;
    private final BlockingQueue<NetworkProtocol.Message> messageQueue;
    private ObjectInputStream objectInputStream;
    private ObjectOutputStream objectOutputStream;
    private boolean isConnected;
    private Consumer<NetworkProtocol.Message> messageHandler;
    private Consumer<String> disconnectHandler;
    private Thread readThread;
    private Thread writeThread;

    public PeerConnection(Socket socket, String peerId) throws IOException {
        this.socket = socket;
        this.peerId = peerId;
        this.messageQueue = new LinkedBlockingQueue<>();
        this.isConnected = true;

        initializeStreams();
        startThreads();
    }

    private void initializeStreams() throws IOException {
        objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
        objectOutputStream.flush();
        objectInputStream = new ObjectInputStream(socket.getInputStream());
    }

    private void startThreads() {
        readThread = new Thread(this::readMessages);
        readThread.setDaemon(true);
        readThread.setName("Read-" + peerId);
        readThread.start();

        writeThread = new Thread(this::writeMessages);
        writeThread.setDaemon(true);
        writeThread.setName("Write-" + peerId);
        writeThread.start();
    }

    private void readMessages() {
        try {
            while (isConnected) {
                try {
                    Object obj = objectInputStream.readObject();
                    if (obj instanceof NetworkProtocol.Message message) {
                        if (messageHandler != null) {
                            messageHandler.accept(message);
                        }
                    }
                } catch (java.io.InvalidClassException e) {
                    // Lỗi serialVersionUID không khớp - không disconnect, chỉ log và skip message này
                    System.err.println("[PeerConnection] InvalidClassException from peer " + peerId
                            + ": " + e.getMessage() + " - skipping message. "
                            + "Có thể do version code khác nhau giữa 2 máy.");
                    // Tiếp tục đọc message tiếp theo thay vì disconnect
                    continue;
                } catch (EOFException e) {
                    // End of stream - connection đóng bình thường
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("[PeerConnection] ClassNotFoundException from peer " + peerId
                            + ": " + e.getMessage() + " - skipping message");
                    continue; // Skip message này, tiếp tục đọc
                }
            }
        } catch (SocketException e) {
            // Connection closed bình thường
            System.out.println("[PeerConnection] Socket closed for peer " + peerId);
        } catch (IOException e) {
            if (isConnected) {
                System.err.println("[PeerConnection] IOException reading from peer " + peerId + ": " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }

    private void writeMessages() {
        try {
            while (isConnected) {
                try {
                    NetworkProtocol.Message message = messageQueue.take();
                    synchronized (objectOutputStream) {
                        objectOutputStream.writeObject(message);
                        objectOutputStream.flush();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Error writing to peer " + peerId + ": " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    public void sendMessage(NetworkProtocol.Message message) throws IOException {
        if (!isConnected) {
            throw new IOException("Connection to " + peerId + " is closed");
        }
        messageQueue.offer(message);
    }
    
    /**
     * Force flush output stream để đảm bảo message được gửi ngay lập tức.
     * Dùng cho các message quan trọng như DISCONNECT.
     */
    public void flush() {
        synchronized (objectOutputStream) {
            try {
                if (objectOutputStream != null && isConnected) {
                    objectOutputStream.flush();
                }
            } catch (IOException e) {
                System.err.println("[PeerConnection] Error flushing stream: " + e.getMessage());
            }
        }
    }

    public void setMessageHandler(Consumer<NetworkProtocol.Message> handler) {
        this.messageHandler = handler;
    }

    public void setDisconnectHandler(Consumer<String> handler) {
        this.disconnectHandler = handler;
    }

    public synchronized void disconnect() {
        if (!isConnected)
            return;

        isConnected = false;
        try {
            if (objectInputStream != null)
                objectInputStream.close();
            if (objectOutputStream != null)
                objectOutputStream.close();
            if (socket != null && !socket.isClosed())
                socket.close();
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }

        if (disconnectHandler != null) {
            disconnectHandler.accept(peerId);
        }
    }

    public String getPeerId() {
        return peerId;
    }

    public boolean isConnected() {
        return isConnected && !socket.isClosed();
    }

    public String getRemoteAddress() {
        return socket.getInetAddress().getHostAddress();
    }
}
