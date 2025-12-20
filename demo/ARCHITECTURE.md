# P2P Whiteboard Pro - Kiến Trúc và Cơ Chế Hoạt Động

## 📋 Mục Lục
1. [Tổng Quan Kiến Trúc](#tổng-quan-kiến-trúc)
2. [P2P Networking - Cơ Chế Hoạt Động](#p2p-networking---cơ-chế-hoạt-động)
3. [Kiến Thức Lập Trình Mạng](#kiến-thức-lập-trình-mạng)
4. [Các Module Chính](#các-module-chính)
5. [Luồng Dữ Liệu](#luồng-dữ-liệu)
6. [Đồng Bộ Hóa và Xử Lý Xung Đột](#đồng-bộ-hóa-và-xử-lý-xung-đột)
7. [Tối Ưu Hóa Hiệu Suất](#tối-ưu-hóa-hiệu-suất)

---

## 🏗️ Tổng Quan Kiến Trúc

### Kiến Trúc Tổng Thể

```
┌─────────────────────────────────────────────────────────────┐
│                      APPLICATION LAYER                      │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │
│  │  MainFrame   │  │   UI Panels  │  │ DrawingCanvas │     │
│  │  (Orchestrator)│ │  (Controls)  │  │  (Renderer)   │     │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘     │
│         │                  │                  │             │
└─────────┼──────────────────┼──────────────────┼─────────────┘
          │                  │                  │
┌─────────┼──────────────────┼──────────────────┼─────────────┐
│         │                  │                  │               │
│  ┌──────▼──────┐  ┌───────▼──────┐  ┌───────▼──────┐        │
│  │ MessageHandler│ │ PeerDiscovery│ │ PeerConnection│        │
│  │ (Routing)     │ │ (UDP Discovery)│ │ (TCP Stream) │        │
│  └──────┬───────┘  └───────┬──────┘  └───────┬──────┘        │
│         │                  │                  │                │
└─────────┼──────────────────┼──────────────────┼───────────────┘
          │                  │                  │
┌─────────┼──────────────────┼──────────────────┼─────────────┐
│         │                  │                  │               │
│  ┌──────▼──────┐  ┌───────▼──────┐  ┌───────▼──────┐        │
│  │   UDP Socket│  │  TCP Socket   │  │ ObjectStream │        │
│  │  (Port 55556)│ │ (Dynamic Port)│ │ (Serialization)│       │
│  └─────────────┘  └──────────────┘  └──────────────┘        │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
              ┌───────────────────────┐
              │    NETWORK LAYER      │
              │   (LAN/WiFi/Ethernet) │
              └───────────────────────┘
```

### Mô Hình P2P: Star Topology

```
                    ┌─────────┐
                    │  Host   │
                    │ (Peer A)│
                    └────┬────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
   ┌────────┐      ┌────────┐      ┌────────┐
   │ Guest  │      │ Guest  │      │ Guest  │
   │(Peer B)│      │(Peer C)│      │(Peer D)│
   └────────┘      └────────┘      └────────┘

Mỗi Guest kết nối trực tiếp đến Host.
Host có thể broadcast message đến tất cả Guest.
Guest có thể gửi message đến Host.
Guest-Guest không kết nối trực tiếp (qua Host relay).
```

**Lý Do Chọn Star Topology:**
- ✅ Đơn giản hơn mesh topology
- ✅ Host quản lý tập trung (room, kick, disconnect)
- ✅ Dễ implement và debug
- ✅ Phù hợp với mô hình Host/Guest

---

## 🌐 P2P Networking - Cơ Chế Hoạt Động

### 1. UDP Broadcast Discovery

#### Mục Đích
Tìm các peer khác trên cùng mạng LAN mà không cần biết IP cụ thể.

#### Cơ Chế Hoạt Động

**Broadcast Address:**
```java
private static final String BROADCAST_ADDRESS = "192.168.1.99";
private static final int BROADCAST_PORT = 55556;
```

**Broadcast Message Format:**
```
peerId|peerName|listenPort|roomId|roomPassword
```

Ví dụ:
```
ddff3a6c|Giang|62717|room-1|mypassword
```

**Luồng Broadcast:**

```
┌─────────────────────────────────────────────────────────┐
│              PEER DISCOVERY BROADCAST                   │
│                                                          │
│  Every 3 seconds:                                        │
│  1. Create DatagramPacket                                │
│     - Data: "peerId|name|port|roomId|password"          │
│     - Address: 192.168.1.99 (broadcast)                 │
│     - Port: 55556                                        │
│                                                          │
│  2. Send via DatagramSocket                             │
│     - socket.setBroadcast(true)                          │
│     - socket.send(packet)                                │
│                                                          │
│  3. Packet được broadcast đến tất cả máy trong subnet   │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

**Listen và Parse:**

```
┌─────────────────────────────────────────────────────────┐
│              PEER DISCOVERY LISTEN                      │
│                                                          │
│  1. Bind DatagramSocket to port 55556                   │
│     - Chỉ một instance có thể bind (BindException)      │
│     - Các instance khác chỉ broadcast                   │
│                                                          │
│  2. Receive UDP packets                                 │
│     - socket.receive(packet)                            │
│     - Parse: "peerId|name|port|roomId|password"         │
│                                                          │
│  3. Filter by roomId & password                         │
│     - Nếu roomId khác → ignore                           │
│     - Nếu password khác → ignore                        │
│                                                          │
│  4. Add/Update discovered peers                          │
│     - Map<String, PeerInfo> discoveredPeers             │
│     - Trigger onPeerFound callback                      │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

#### Kiến Thức Mạng: UDP Broadcast

**UDP (User Datagram Protocol):**
- **Connectionless**: Không cần thiết lập connection trước
- **Unreliable**: Không đảm bảo delivery (có thể mất gói)
- **Fast**: Overhead thấp, phù hợp cho discovery
- **Broadcast**: Có thể gửi đến tất cả máy trong subnet

**Broadcast Address:**
- `192.168.1.99` là broadcast address cho subnet `192.168.1.0/24`
- Tất cả máy trong subnet sẽ nhận được packet
- Không cần biết IP cụ thể của từng máy

**Port 55556:**
- Port cố định cho UDP discovery
- Tất cả peer phải listen trên port này
- Chỉ một instance trên mỗi máy có thể bind (OS limitation)

### 2. TCP Direct Connection

#### Mục Đích
Thiết lập kết nối TCP đáng tin cậy để truyền dữ liệu (shapes, messages).

#### Cơ Chế Hoạt Động

**Host Side (Server):**
```java
// Tạo ServerSocket với port tự động (port 0)
ServerSocket serverSocket = new ServerSocket(0);
int listenPort = serverSocket.getLocalPort(); // OS chọn port rảnh

// Listen cho incoming connections
while (true) {
    Socket socket = serverSocket.accept(); // Blocking call
    PeerConnection connection = new PeerConnection(socket, peerId);
    messageHandler.addConnection(connection);
}
```

**Guest Side (Client):**
```java
// Connect đến Host's IP:Port (từ UDP discovery)
Socket socket = new Socket(hostIP, hostPort);
PeerConnection connection = new PeerConnection(socket, peerId);
messageHandler.addConnection(connection);
```

**Bidirectional Communication:**

```
┌─────────────────────────────────────────────────────────┐
│              PEER CONNECTION ARCHITECTURE               │
│                                                          │
│  ┌──────────────────────────────────────────────┐      │
│  │         PeerConnection                       │      │
│  │                                               │      │
│  │  ┌──────────────┐      ┌──────────────┐     │      │
│  │  │ Read Thread  │      │ Write Thread │     │      │
│  │  │              │      │              │     │      │
│  │  │ ObjectInputStream │  │ ObjectOutputStream│ │      │
│  │  │              │      │              │     │      │
│  │  │ readObject() │      │ writeObject()│     │      │
│  │  │              │      │              │     │      │
│  │  └──────┬───────┘      └──────┬───────┘     │      │
│  │         │                     │              │      │
│  └─────────┼─────────────────────┼──────────────┘      │
│            │                     │                      │
│            ▼                     ▼                      │
│  ┌──────────────────────────────────────────────┐      │
│  │         TCP Socket (Bidirectional)          │      │
│  └──────────────────────────────────────────────┘      │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

#### Kiến Thức Mạng: TCP Socket

**TCP (Transmission Control Protocol):**
- **Connection-oriented**: Phải thiết lập connection trước (3-way handshake)
- **Reliable**: Đảm bảo delivery, ordering, error detection
- **Stream-based**: Dữ liệu là stream bytes, không phải packets
- **Bidirectional**: Có thể gửi/nhận đồng thời

**ServerSocket vs Socket:**
- **ServerSocket**: Listen cho incoming connections (server side)
- **Socket**: Kết nối đến server (client side) hoặc accepted connection (server side)

**Object Streams:**
- **ObjectOutputStream**: Serialize Java objects thành bytes
- **ObjectInputStream**: Deserialize bytes thành Java objects
- Yêu cầu class implements `Serializable`
- Cần `serialVersionUID` để đảm bảo compatibility

### 3. Message Protocol

#### Message Types

```java
public enum MessageType {
    HELLO,          // Initial connection greeting
    SHAPES,         // Drawing data (Shape objects)
    ACK,            // Acknowledgment
    SYNC_REQUEST,   // Request full state sync
    SYNC_RESPONSE,  // Full state response
    CLEAR,          // Clear canvas command
    UNDO,           // Undo action command
    PEER_LIST,      // List of connected peers
    DISCONNECT      // Peer disconnecting (with reason)
}
```

#### Message Structure

```java
public class Message implements Serializable {
    public MessageType type;      // Loại message
    public String senderId;       // ID của peer gửi
    public long timestamp;        // Timestamp khi tạo message
    public Object data;           // Payload (ShapeData, String, List, etc.)
}
```

#### Message Flow Examples

**HELLO Message:**
```
Peer A connects to Peer B
    ↓
Peer A sends: Message(HELLO, "peerA", null)
    ↓
Peer B receives → Log "Peer connected: peerA"
```

**SHAPES Message:**
```
Peer A draws a shape
    ↓
Create: Message(SHAPES, "peerA", ShapeData([shape1, shape2], version))
    ↓
Broadcast to all peers except peerA
    ↓
Peer B receives → Extract ShapeData → Render shapes on canvas
```

**CLEAR Message:**
```
Peer A clicks "Clear"
    ↓
Send: Message(CLEAR, "peerA", null)
    ↓
Broadcast to all peers
    ↓
All peers receive → Clear their canvas
```

**DISCONNECT Message:**
```
Host closes room
    ↓
Send: Message(DISCONNECT, "hostId", "ROOM_CLOSED")
    ↓
All guests receive → Show dialog → Return to StartupDialog
```

---

## 📚 Kiến Thức Lập Trình Mạng

### 1. Java Networking APIs

#### java.net.DatagramSocket (UDP)

**Khởi Tạo:**
```java
DatagramSocket socket = new DatagramSocket(port);
socket.setBroadcast(true); // Cho phép broadcast
```

**Gửi Packet:**
```java
byte[] data = message.getBytes();
DatagramPacket packet = new DatagramPacket(
    data, data.length,
    InetAddress.getByName("192.168.1.99"), // Broadcast address
    55556 // Port
);
socket.send(packet);
```

**Nhận Packet:**
```java
byte[] buffer = new byte[1024];
DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
socket.receive(packet); // Blocking call
String received = new String(packet.getData(), 0, packet.getLength());
```

**Đặc Điểm:**
- **Non-blocking có thể**: Có thể dùng `socket.setSoTimeout()` để timeout
- **Thread-safe**: Mỗi socket nên dùng trong một thread riêng
- **Resource management**: Phải `close()` khi không dùng

#### java.net.ServerSocket (TCP Server)

**Khởi Tạo:**
```java
ServerSocket serverSocket = new ServerSocket(0); // Port 0 = auto-assign
int port = serverSocket.getLocalPort(); // Lấy port đã assign
```

**Accept Connections:**
```java
while (true) {
    Socket clientSocket = serverSocket.accept(); // Blocking call
    // Xử lý clientSocket trong thread riêng
    new Thread(() -> handleClient(clientSocket)).start();
}
```

**Đặc Điểm:**
- **Blocking**: `accept()` sẽ block cho đến khi có connection
- **One connection per accept**: Mỗi `accept()` trả về một Socket mới
- **Port reuse**: Có thể set `SO_REUSEADDR` để reuse port

#### java.net.Socket (TCP Client/Connection)

**Kết Nối:**
```java
Socket socket = new Socket("192.168.1.154", 51970);
```

**I/O Streams:**
```java
// Text I/O
BufferedReader reader = new BufferedReader(
    new InputStreamReader(socket.getInputStream())
);
PrintWriter writer = new PrintWriter(
    socket.getOutputStream(), true
);

// Binary/Object I/O
ObjectInputStream ois = new ObjectInputStream(
    socket.getInputStream()
);
ObjectOutputStream oos = new ObjectOutputStream(
    socket.getOutputStream()
);
```

**Đặc Điểm:**
- **Bidirectional**: Có thể đọc/ghi đồng thời
- **Stream-based**: Dữ liệu là stream, không phải packets
- **Blocking I/O**: `read()` sẽ block cho đến khi có dữ liệu

### 2. Serialization

#### Java Serialization

**Yêu Cầu:**
```java
public class Shape implements Serializable {
    private static final long serialVersionUID = 1L; // QUAN TRỌNG!
    
    // Tất cả fields phải là Serializable hoặc transient
    public int x1, y1;
    public Color color; // Color implements Serializable
    // ...
}
```

**Serialize:**
```java
ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
oos.writeObject(shape);
oos.flush();
```

**Deserialize:**
```java
ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
Shape shape = (Shape) ois.readObject();
```

**serialVersionUID:**
- **Mục đích**: Đảm bảo compatibility giữa các version của class
- **Nếu không match**: `InvalidClassException`
- **Best practice**: Đặt cố định (ví dụ: `1L`) và không đổi

**Transient Fields:**
```java
transient private Graphics2D g2d; // Không serialize field này
```

### 3. Concurrency và Threading

#### Thread Model

**Daemon Threads:**
```java
Thread thread = new Thread(() -> {
    // Network I/O operations
});
thread.setDaemon(true); // Tự động terminate khi app exit
thread.start();
```

**Thread Safety:**
```java
// ConcurrentHashMap cho thread-safe map
private final Map<String, PeerConnection> connections = 
    new ConcurrentHashMap<>();

// Synchronized cho critical sections
synchronized (objectOutputStream) {
    objectOutputStream.writeObject(message);
    objectOutputStream.flush();
}
```

**BlockingQueue:**
```java
// Thread-safe queue cho producer-consumer pattern
private final BlockingQueue<Message> messageQueue = 
    new LinkedBlockingQueue<>();

// Producer (main thread)
messageQueue.offer(message);

// Consumer (write thread)
Message message = messageQueue.take(); // Blocking call
```

#### Event Dispatch Thread (EDT)

**Swing Threading Rule:**
- Tất cả UI operations phải chạy trên EDT
- Network threads không được trực tiếp update UI

**Solution:**
```java
// Network thread
new Thread(() -> {
    // Receive message from network
    Shape shape = receiveShape();
    
    // Update UI on EDT
    SwingUtilities.invokeLater(() -> {
        canvas.drawRemoteShape(shape);
    });
}).start();
```

### 4. Network Error Handling

#### Common Exceptions

**BindException:**
```java
try {
    socket = new DatagramSocket(BROADCAST_PORT);
} catch (BindException be) {
    // Port đã được sử dụng
    // Graceful fallback: chỉ broadcast, không listen
}
```

**SocketException:**
```java
try {
    socket.receive(packet);
} catch (SocketException e) {
    // Socket đã đóng hoặc network error
    // Clean up và disconnect
}
```

**EOFException:**
```java
try {
    Object obj = ois.readObject();
} catch (EOFException e) {
    // End of stream - connection closed normally
    break;
}
```

**InvalidClassException:**
```java
try {
    Shape shape = (Shape) ois.readObject();
} catch (InvalidClassException e) {
    // serialVersionUID không khớp
    // Skip message này, tiếp tục đọc message tiếp theo
    continue;
}
```

---

## 🧩 Các Module Chính

### 1. Network Module (`com.whiteboard.network`)

#### PeerDiscovery.java

**Chức Năng:**
- UDP broadcast để announce presence
- Listen UDP packets từ peers khác
- Filter peers theo roomId và password
- Maintain discovered peers map

**Key Methods:**
```java
public void start() // Start broadcast và listen threads
private void broadcastPresence() // Broadcast mỗi 3 giây
private void listenForPeers() // Listen và parse UDP packets
public Collection<PeerInfo> getDiscoveredPeers() // Get discovered peers
```

**Threads:**
- `discoveryThread`: Broadcast presence
- `listeningThread`: Listen UDP packets

#### PeerConnection.java

**Chức Năng:**
- Quản lý một TCP connection đến một peer
- Bidirectional messaging qua ObjectStreams
- Thread-safe message queue

**Key Methods:**
```java
public void sendMessage(Message message) // Add message to queue
private void readMessages() // Read thread - deserialize và dispatch
private void writeMessages() // Write thread - serialize và send
public void disconnect() // Close connection
```

**Threads:**
- `readThread`: Đọc messages từ ObjectInputStream
- `writeThread`: Ghi messages từ queue đến ObjectOutputStream

#### MessageHandler.java

**Chức Năng:**
- Quản lý tất cả PeerConnections
- Route messages đến appropriate handlers
- Broadcast messages đến tất cả peers

**Key Methods:**
```java
public void addConnection(PeerConnection conn) // Add connection
public void broadcastShapes(List<Shape> shapes, String senderId) // Broadcast shapes
public void broadcastClear(String senderId) // Broadcast clear
public void broadcastUndo(String senderId) // Broadcast undo
private void handleMessage(Message message) // Route message
```

**Callbacks:**
```java
setOnShapesReceived(Consumer<ShapeData>)
setOnClearReceived(Consumer<String>)
setOnUndoReceived(Consumer<String>)
setOnDisconnectReceived(Consumer<String>)
```

#### NetworkProtocol.java

**Chức Năng:**
- Định nghĩa message types và data structures
- Protocol contracts cho communication

**Classes:**
- `MessageType`: Enum các loại message
- `Message`: Message structure
- `ShapeData`: Wrapper cho List<Shape>
- `PeerInfo`: Thông tin về một peer

### 2. Drawing Module (`com.whiteboard.drawing`)

#### DrawingCanvas.java

**Chức Năng:**
- Main drawing surface
- Handle mouse events
- Render shapes
- Manage layers

**Key Methods:**
```java
public void drawRemoteShape(Shape shape) // Draw shape from network
public void startDrawing(MouseEvent e) // Start drawing on mouse press
public void continueDrawing(MouseEvent e) // Continue on mouse drag
public void finishDrawing(MouseEvent e) // Finish on mouse release
```

**Optimizations:**
- In-place updates cho remote shapes
- Repaint debouncing (Timer, 60fps max)
- Pencil throttling (2 points threshold)

#### Shape.java

**Chức Năng:**
- Data model cho tất cả shapes
- Serialization support
- Rendering logic

**Fields:**
```java
public ShapeType type;
public int x1, y1, x2, y2;
public Color color;
public int strokeWidth;
public String text; // For TEXT type
public List<Point> freeDrawPoints; // For PENCIL type
public String peerId; // Who drew this
public long timestamp;
// Text styling
public String fontName;
public int fontSize;
public int fontStyle;
public int textAlign;
```

**Serialization:**
```java
private static final long serialVersionUID = 1L;
```

### 3. UI Module (`com.whiteboard.ui`)

#### MainFrame.java

**Chức Năng:**
- Main application window
- Orchestrate tất cả components
- Network initialization
- Event handling

**Key Responsibilities:**
- Initialize network (PeerDiscovery, MessageHandler, ServerSocket)
- Handle peer discovery và connection
- Bridge UI events và network actions
- Manage host/guest roles

**Key Methods:**
```java
private void initNetwork() // Initialize network components
private void acceptConnections() // Accept TCP connections (host)
private void connectToPeer(PeerInfo peerInfo) // Connect to peer (guest)
private void handleRemoteRoomDisconnect(String reason) // Handle disconnect
```

---

## 🔄 Luồng Dữ Liệu

### Luồng: Vẽ Shape và Sync

```
User Action (Mouse Drag)
    ↓
DrawingCanvas.startDrawing()
    ↓
DrawingCanvas.continueDrawing()
    ↓
Shape created (local render)
    ↓
onShapeDrawn callback triggered
    ↓
MainFrame.onShapeDrawn()
    ↓
MessageHandler.broadcastShapes()
    ↓
For each PeerConnection:
    - messageQueue.offer(SHAPES message)
    ↓
PeerConnection.writeMessages() thread
    ↓
ObjectOutputStream.writeObject(message)
    ↓
TCP Socket → Network
    ↓
[NETWORK TRANSMISSION]
    ↓
TCP Socket → Network
    ↓
PeerConnection.readMessages() thread
    ↓
ObjectInputStream.readObject()
    ↓
MessageHandler.handleMessage()
    ↓
onShapesReceived callback
    ↓
MainFrame.onShapesReceived()
    ↓
SwingUtilities.invokeLater(() -> {
    canvas.drawRemoteShape(shape)
})
    ↓
DrawingCanvas.drawRemoteShape()
    ↓
In-place update existing shape (if exists)
    ↓
Repaint timer triggered
    ↓
Canvas repaint (max 60fps)
    ↓
Shape rendered on remote peer's screen
```

### Luồng: Discovery và Connection

```
App Start
    ↓
StartupDialog → User inputs (room, password)
    ↓
MainFrame.initNetwork()
    ↓
Create ServerSocket (port auto-assign)
    ↓
Start PeerDiscovery
    ↓
┌─────────────────────────┬─────────────────────────┐
│   Broadcast Thread      │   Listen Thread         │
│                         │                         │
│ Every 3s:               │ Bind UDP port 55556     │
│ - Create DatagramPacket │                         │
│ - Send to broadcast     │ While true:              │
│   address               │ - socket.receive()       │
│                         │ - Parse packet          │
│                         │ - Filter by room/pass   │
│                         │ - Add/update peer       │
│                         │ - Trigger callback      │
└─────────────────────────┴─────────────────────────┘
    ↓
Peer found callback
    ↓
MainFrame.connectToPeer()
    ↓
Create Socket(peerIP, peerPort)
    ↓
Create PeerConnection
    ↓
Initialize ObjectStreams
    ↓
Start read/write threads
    ↓
Send HELLO message
    ↓
Connection established
    ↓
Ready for collaboration
```

---

## 🔀 Đồng Bộ Hóa và Xử Lý Xung Đột

### Timestamp-based Ordering

**Cơ Chế:**
- Mỗi Shape có `timestamp` (System.currentTimeMillis())
- Khi có conflict, shape có timestamp nhỏ hơn được ưu tiên
- Đảm bảo consistency giữa các peer

**Implementation:**
```java
// Trong Shape
public long timestamp = System.currentTimeMillis();

// Khi merge shapes
shapes.sort((a, b) -> Long.compare(a.timestamp, b.timestamp));
```

### In-place Updates

**Vấn Đề:**
- Nếu remove và re-add shape mỗi lần update → lag
- Layer manipulation overhead

**Giải Pháp:**
```java
// Tìm existing shape
Shape existing = remoteStrokeMap.get(key);

if (existing == null) {
    // Add new shape
    layerManager.addShapeToActiveLayer(shape);
} else {
    // Update in-place
    existing.x1 = shape.x1;
    existing.y1 = shape.y1;
    // ... update other fields
    // Không remove/add từ layer
}
```

### Repaint Debouncing

**Vấn Đề:**
- Quá nhiều repaint() calls → UI lag
- Mỗi shape update trigger repaint

**Giải Pháp:**
```java
private boolean pendingRepaint = false;
private Timer repaintTimer = new Timer(16, e -> { // ~60fps
    if (pendingRepaint) {
        repaint();
        pendingRepaint = false;
    } else {
        timer.stop();
    }
});

// Khi có update
pendingRepaint = true;
if (!repaintTimer.isRunning()) {
    repaintTimer.start();
}
```

---

## ⚡ Tối Ưu Hóa Hiệu Suất

### 1. Network Optimizations

**Pencil Throttling:**
```java
// Chỉ gửi sau mỗi N points
private int freeDrawPointsSinceBroadcast = 0;
private static final int PENCIL_BROADCAST_THRESHOLD = 2;

if (tool == PENCIL) {
    freeDrawPointsSinceBroadcast++;
    if (freeDrawPointsSinceBroadcast >= PENCIL_BROADCAST_THRESHOLD) {
        broadcastShape();
        freeDrawPointsSinceBroadcast = 0;
    }
}
```

**Batch Updates:**
```java
// Gửi nhiều shapes trong một message
List<Shape> batch = new ArrayList<>();
// ... collect shapes
messageHandler.broadcastShapes(batch, peerId);
```

### 2. UI Optimizations

**EDT Usage:**
```java
// Tất cả UI updates trên EDT
SwingUtilities.invokeLater(() -> {
    canvas.drawRemoteShape(shape);
});
```

**Repaint Debouncing:**
- Giới hạn repaint rate ~60fps
- Tránh repaint không cần thiết

**In-place Updates:**
- Không remove/add shapes từ layers
- Chỉ update fields của existing shape

### 3. Memory Optimizations

**ConcurrentHashMap:**
- Thread-safe, hiệu suất tốt cho concurrent access

**BlockingQueue:**
- Efficient producer-consumer pattern
- Không cần polling

**Shape Pooling (Future):**
- Có thể implement object pooling để reuse Shape objects

---

## 📊 Tổng Kết

### P2P Architecture Highlights

1. **Pure P2P**: Không có server trung tâm, giao tiếp trực tiếp giữa peers
2. **UDP Discovery**: Tìm peers trên LAN mà không cần biết IP cụ thể
3. **TCP Reliability**: Dùng TCP cho data transmission đảm bảo delivery
4. **Star Topology**: Host-centric, đơn giản và dễ quản lý
5. **Real-time Sync**: Đồng bộ shapes, clear, undo theo thời gian thực

### Network Knowledge Applied

1. **UDP Broadcast**: Discovery mechanism
2. **TCP Sockets**: Reliable data transmission
3. **Object Serialization**: Java object transfer
4. **Threading**: Concurrent network I/O
5. **Error Handling**: Graceful degradation

### Performance Optimizations

1. **Throttling**: Giảm network traffic
2. **Debouncing**: Giảm UI repaints
3. **In-place Updates**: Giảm object manipulation
4. **EDT Usage**: Thread-safe UI updates

---

**Tài liệu này cung cấp cái nhìn sâu về kiến trúc và cơ chế hoạt động của P2P Whiteboard Pro. Để hiểu chi tiết implementation, xem source code trong các file tương ứng.**

