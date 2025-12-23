# PHÂN TÍCH GIAO THỨC MẠNG TRONG ỨNG DỤNG P2P WHITEBOARD

## 1. Tổng quan về giao thức mạng trong dự án

Dự án P2P Whiteboard sử dụng kết hợp cả hai giao thức UDP và TCP để đạt được hiệu suất tối ưu trong việc phát hiện peer và truyền dữ liệu đáng tin cậy.

## 2. Phân tích chi tiết UDP trong dự án

### 2.1. Vai trò của UDP
- **Mục đích sử dụng**: UDP được sử dụng cho quá trình phát hiện peer (peer discovery) trong mạng cục bộ.
- **Lý do chọn UDP**:
  - Tốc độ nhanh, không cần thiết lập kết nối
  - Phù hợp cho việc gửi thông điệp broadcast
  - Độ trễ thấp, phù hợp cho việc phát hiện peer nhanh chóng

### 2.2. Cài đặt UDP trong PeerDiscovery
```java
// Sử dụng DatagramSocket cho UDP
private DatagramSocket broadcastSocket;

// Khởi tạo socket UDP với chế độ broadcast
broadcastSocket = new DatagramSocket();
broadcastSocket.setBroadcast(true);

// Gửi thông điệp broadcast
DatagramPacket packet = new DatagramPacket(
    message, message.length,
    InetAddress.getByName(BROADCAST_ADDRESS), BROADCAST_PORT
);
broadcastSocket.send(packet);
```

### 2.3. Đặc điểm của UDP trong dự án
- **Không đảm bảo giao nhận**: Có thể mất gói tin nhưng chấp nhận được trong ngữ cảnh phát hiện peer
- **Không có kiểm soát tắc nghẽn**: Phù hợp cho mạng cục bộ với lưu lượng thấp
- **Địa chỉ broadcast**: Sử dụng địa chỉ broadcast 192.168.1.255 cho mạng 192.168.1.0/24

## 3. Phân tích chi tiết TCP trong dự án

### 3.1. Vai trò của TCP
- **Mục đích sử dụng**: Truyền dữ liệu đáng tin cậy giữa các peer sau khi đã thiết lập kết nối
- **Lý do chọn TCP**:
  - Đảm bảo truyền dữ liệu tin cậy, đúng thứ tự
  - Tự động kiểm soát lưu lượng và tắc nghẽn
  - Phù hợp cho việc truyền dữ liệu quan trọng như hình vẽ, tin nhắn chat

### 3.2. Cài đặt TCP trong PeerConnection
```java
// Sử dụng Socket cho TCP
private final Socket socket;
private ObjectInputStream objectInputStream;
private ObjectOutputStream objectOutputStream;

// Khởi tạo luồng vào/ra
objectOutputStream = new ObjectOutputStream(socket.getOutputStream());
objectInputStream = new ObjectInputStream(socket.getInputStream());

// Đọc dữ liệu
Object obj = objectInputStream.readObject();

// Ghi dữ liệu
objectOutputStream.writeObject(message);
objectOutputStream.flush();
```

### 3.3. Đặc điểm của TCP trong dự án
- **Kết nối hướng kết nối (connection-oriented)**: Thiết lập kết nối trước khi truyền dữ liệu
- **Đáng tin cậy**: Đảm bảo dữ liệu được gửi đầy đủ và đúng thứ tự
- **Kiểm soát luồng và tắc nghẽn**: Tự động điều chỉnh tốc độ truyền
- **Truyền dữ liệu có cấu trúc**: Sử dụng ObjectInputStream/ObjectOutputStream để truyền đối tượng

## 4. So sánh giữa TCP và UDP trong dự án

| Đặc điểm | UDP trong dự án | TCP trong dự án |
|----------|----------------|----------------|
| **Mục đích** | Phát hiện peer | Truyền dữ liệu |
| **Độ tin cậy** | Không đảm bảo | Đảm bảo |
| **Tốc độ** | Nhanh hơn | Chậm hơn do overhead |
| **Kiểu kết nối** | Không kết nối | Hướng kết nối |
| **Kiểm soát tắc nghẽn** | Không có | Có |
| **Đảm bảo thứ tự** | Không | Có |
| **Port sử dụng** | 55556 (UDP) | Tùy chỉnh (thường > 1024) |

## 5. Ánh xạ lên mô hình OSI 7 tầng

### 5.1. Mô hình OSI 7 tầng

| Tầng | Tên gọi | Chức năng chính |
|------|---------|----------------|
| 7 | Application | Giao diện giữa ứng dụng và mạng |
| 6 | Presentation | Chuyển đổi dữ liệu, mã hóa, nén |
| 5 | Session | Thiết lập, duy trì, kết thúc phiên |
| 4 | Transport | Truyền dữ liệu tin cậy (TCP/UDP) |
| 3 | Network | Định tuyến, đánh địa chỉ logic (IP) |
| 2 | Data Link | Truy cập đường truyền, đánh địa chỉ vật lý (MAC) |
| 1 | Physical | Truyền bit qua phương tiện vật lý |

### 5.2. Ánh xạ trong dự án

#### 5.2.1. Tầng Ứng dụng (Application Layer - 7)
- **Thành phần**: Lớp `PeerDiscovery`, `PeerConnection`, `MessageHandler`
- **Chức năng**: Cung cấp giao diện cho ứng dụng whiteboard tương tác với mạng
- **Ví dụ**: Xử lý tin nhắn chat, hình vẽ, quản lý kết nối peer

#### 5.2.2. Tầng Trình diễn (Presentation Layer - 6)
- **Thành phần**: Tuần tự hóa/giải tuần tự hóa đối tượng
- **Chức năng**: Chuyển đổi đối tượng Java thành luồng byte và ngược lại
- **Ví dụ**:
  ```java
  // Tuần tự hóa
  objectOutputStream.writeObject(message);
  
  // Giải tuần tự hóa
  Object obj = objectInputStream.readObject();
  ```

#### 5.2.3. Tầng Phiên (Session Layer - 5)
- **Thành phần**: Quản lý phiên kết nối trong `PeerConnection`
- **Chức năng**: Thiết lập, duy trì và kết thúc phiên làm việc giữa các peer
- **Ví dụ**:
  ```java
  // Thiết lập kết nối
  public PeerConnection(Socket socket, String peerId) throws IOException {
      this.socket = socket;
      this.peerId = peerId;
      initializeStreams();
      startThreads();
  }
  
  // Đóng kết nối
  public synchronized void disconnect() {
      if (!isConnected) return;
      isConnected = false;
      // ... đóng các luồng và socket
  }
  ```

#### 5.2.4. Tầng Giao vận (Transport Layer - 4)
- **Giao thức**: TCP và UDP
- **Chức năng**:
  - **TCP**: Đảm bảo truyền dữ liệu tin cậy, đúng thứ tự
  - **UDP**: Truyền dữ liệu không đảm bảo, không kết nối
- **Ví dụ**:
  ```java
  // Sử dụng TCP
  Socket socket = new Socket(address, port);
  
  // Sử dụng UDP
  DatagramSocket udpSocket = new DatagramSocket();
  ```

#### 5.2.5. Tầng Mạng (Network Layer - 3)
- **Giao thức**: IP (Internet Protocol)
- **Chức năng**: Định tuyến gói tin giữa các mạng khác nhau
- **Thực hiện bởi**: Hệ điều hành và phần cứng mạng
- **Ví dụ trong code**:
  ```java
  // Sử dụng địa chỉ IP để kết nối
  InetAddress address = InetAddress.getByName("192.168.1.100");
  ```

#### 5.2.6. Tầng Liên kết dữ liệu (Data Link Layer - 2)
- **Giao thức**: Ethernet, WiFi (802.11)
- **Chức năng**: Truy cập đường truyền, phát hiện và sửa lỗi cơ bản
- **Thực hiện bởi**: Driver card mạng và hệ điều hành

#### 5.2.7. Tầng Vật lý (Physical Layer - 1)
- **Thành phần**: Cáp mạng, switch, hub, tín hiệu điện/từ
- **Chức năng**: Truyền dẫn bit thô qua phương tiện vật lý

## 6. Tổng kết

Dự án P2P Whiteboard đã tận dụng hiệu quả cả hai giao thức TCP và UDP, mỗi giao thức phục vụ mục đích riêng phù hợp với đặc tính của chúng:

1. **UDP** được sử dụng cho việc phát hiện peer ban đầu, nơi tốc độ quan trọng hơn độ tin cậy.
2. **TCP** được sử dụng cho việc truyền dữ liệu ứng dụng, nơi đòi hỏi độ tin cậy cao.

Việc ánh xạ lên mô hình OSI 7 tầng cho thấy dự án đã triển khai đầy đủ các chức năng từ tầng ứng dụng đến tầng giao vận, trong khi các tầng thấp hơn được ủy quyền cho hệ điều hành và phần cứng mạng xử lý.

Cách tiếp cận này đảm bảo hiệu suất tối ưu trong khi vẫn duy trì được độ tin cậy cần thiết cho ứng dụng cộng tác thời gian thực như whiteboard.
