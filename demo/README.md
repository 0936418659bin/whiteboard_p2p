# P2P Whiteboard Pro - Hướng Dẫn Sử Dụng

## 📋 Mục Lục
1. [Giới Thiệu](#giới-thiệu)
2. [Tính Năng](#tính-năng)
3. [Yêu Cầu Hệ Thống](#yêu-cầu-hệ-thống)
4. [Cài Đặt](#cài-đặt)
5. [Cách Sử Dụng](#cách-sử-dụng)
6. [Workflow](#workflow)
7. [Troubleshooting](#troubleshooting)
8. [Build & Deploy](#build--deploy)

---

## 🎯 Giới Thiệu

**P2P Whiteboard Pro** là ứng dụng vẽ bảng trắng hợp tác theo thời gian thực, được xây dựng bằng **Java thuần 100%** với kiến trúc **Peer-to-Peer (P2P) thuần túy**. Ứng dụng cho phép nhiều người dùng trên cùng một mạng LAN vẽ và chỉnh sửa cùng một canvas một cách đồng bộ, không cần server trung tâm.

### Đặc Điểm Nổi Bật
- ✅ **P2P Thuần 100%**: Không cần server, giao tiếp trực tiếp giữa các peer
- ✅ **Real-time Collaboration**: Đồng bộ hình vẽ theo thời gian thực
- ✅ **Room-based**: Tạo/join phòng với mã phòng và mật khẩu
- ✅ **Host/Guest Model**: Quản lý quyền host và guest
- ✅ **Multi-tool Drawing**: Hỗ trợ nhiều công cụ vẽ (Pencil, Line, Rectangle, Circle, Text, v.v.)
- ✅ **Layer Management**: Quản lý layers với opacity, visibility, merge
- ✅ **File Operations**: Save/Load project, Export PNG/SVG
- ✅ **Zoom & Pan**: Phóng to/thu nhỏ và di chuyển canvas
- ✅ **Grid & Snap**: Lưới và snap-to-grid cho vẽ chính xác

---

## ✨ Tính Năng

### 🎨 Drawing Tools (Công Cụ Vẽ)
- **Pencil**: Vẽ tự do bằng chuột
- **Line**: Vẽ đường thẳng
- **Rectangle**: Vẽ hình chữ nhật
- **Circle**: Vẽ hình tròn
- **Ellipse**: Vẽ hình elip
- **Eraser**: Xóa các nét vẽ
- **Text**: Thêm văn bản với font, size, style tùy chỉnh
- **Polygon**: Vẽ đa giác
- **Star**: Vẽ ngôi sao
- **Arrow**: Vẽ mũi tên
- **Curve**: Vẽ đường cong

### 🎨 Color & Styling
- **Color Picker**: Chọn màu từ bảng màu đầy đủ
- **Stroke Width**: Điều chỉnh độ dày nét vẽ (1-50px)
- **Fill Color**: Tô màu cho shapes
- **Gradient**: Tô màu gradient cho shapes
- **Text Styling**: Font (8 fonts), Size (8-72px), Style (Bold, Italic), Alignment (Left, Center, Right)

### 🌐 P2P Networking
- **UDP Broadcast Discovery**: Tự động tìm peer trên cùng mạng LAN
- **TCP Direct Connection**: Kết nối TCP trực tiếp giữa các peer
- **Room System**: Tạo/join phòng với mã phòng và mật khẩu
- **Host/Guest Roles**: Phân quyền host (có thể kick guest, đóng phòng)
- **Real-time Sync**: Đồng bộ shapes, clear, undo theo thời gian thực
- **Connection Status**: Hiển thị trạng thái kết nối và số lượng peer

### 📁 File Operations
- **Save Project**: Lưu toàn bộ project (shapes + layers) dạng JSON
- **Load Project**: Mở lại project đã lưu
- **Export PNG**: Xuất canvas thành hình ảnh PNG
- **Export SVG**: Xuất canvas thành file SVG vector
- **Recent Files**: Danh sách 5 file mở gần đây

### 📐 Advanced Features
- **Layer Management**: 
  - Tạo/xóa/đổi tên layers
  - Di chuyển layer (lên/xuống)
  - Ẩn/hiện layer
  - Điều chỉnh opacity (0-100%)
  - Merge layers
- **Selection & Manipulation**:
  - Chọn single/multiple shapes
  - Resize handles (8 hướng)
  - Move, Rotate shapes
  - Copy/Paste shapes
  - Delete selected shapes
- **Zoom & Pan**:
  - Zoom in/out (10%-500%)
  - Zoom to fit window
  - Zoom to selection
  - Pan canvas bằng chuột
- **Grid & Alignment**:
  - Grid sizes: 5, 10, 15, 20, 25, 50px
  - Snap to grid
  - Align left/right/top/bottom
  - Distribute horizontally/vertically

### 👥 Collaboration Features
- **Multi-peer Drawing**: Nhiều peer vẽ cùng lúc
- **Peer Attribution**: Xem ai đã vẽ shape nào (hover tooltip)
- **Synchronized Actions**: Clear và Undo đồng bộ giữa tất cả peer
- **Peer List**: Danh sách các peer đang kết nối
- **Host Controls**: Host có thể kick guest khỏi phòng

---

## 💻 Yêu Cầu Hệ Thống

### Minimum Requirements
- **OS**: Windows 7+, Linux, macOS
- **Java**: JDK 17 hoặc cao hơn
- **RAM**: 512MB trở lên
- **Network**: Cùng mạng LAN (Local Area Network)
- **Firewall**: Cho phép UDP port 55556 và TCP ports (dynamic)

### Recommended
- **OS**: Windows 10+, Ubuntu 20.04+, macOS 11+
- **Java**: JDK 17 hoặc JDK 21
- **RAM**: 1GB trở lên
- **Network**: Mạng LAN ổn định (Ethernet hoặc WiFi)

---

## 📦 Cài Đặt

### Cách 1: Sử Dụng JAR File (Khuyến Nghị)

1. **Download JAR file**:
   ```bash
   # File: p2p-whiteboard.jar (fat JAR với tất cả dependencies)
   ```

2. **Chạy ứng dụng**:
   ```bash
   java -jar p2p-whiteboard.jar
   ```

### Cách 2: Build Từ Source Code

#### Prerequisites
- JDK 17+
- Maven 3.6+

#### Build Steps

**Windows:**
```cmd
cd demo
build.bat
```

**Linux/macOS:**
```bash
cd demo
chmod +x build.sh
./build.sh
```

**Hoặc dùng Maven trực tiếp:**
```bash
cd demo
mvn clean package
```

Output JAR sẽ nằm tại: `demo/target/p2p-whiteboard.jar`

### Cách 3: Chạy Trực Tiếp Từ IDE

1. Mở project trong IDE (IntelliJ IDEA, Eclipse, VS Code)
2. Set JDK 17+
3. Run class `com.whiteboard.ui.MainFrame`

---

## 🚀 Cách Sử Dụng

### Bước 1: Khởi Động Ứng Dụng

Chạy ứng dụng, bạn sẽ thấy dialog **Startup Dialog**:

```
┌─────────────────────────────────────┐
│  P2P Whiteboard Pro                 │
├─────────────────────────────────────┤
│  Name: [Peer-xxxx]                  │
│  Room Code: [room-1]                │
│  Password: [********]               │
│                                     │
│  [ ] Create Room                    │
│  [ ] Join Room                      │
│                                     │
│  [Cancel]  [OK]                     │
└─────────────────────────────────────┘
```

### Bước 2: Tạo Hoặc Join Phòng

#### Tạo Phòng (Host)
1. Nhập **Name** (tên của bạn)
2. Nhập **Room Code** (mã phòng, ví dụ: `room-1`)
3. Nhập **Password** (mật khẩu phòng, có thể để trống)
4. Chọn **Create Room**
5. Click **OK**

→ Bạn sẽ trở thành **Host** của phòng.

#### Join Phòng (Guest)
1. Nhập **Name** (tên của bạn)
2. Nhập **Room Code** (phải khớp với phòng host đã tạo)
3. Nhập **Password** (phải khớp với mật khẩu phòng)
4. Chọn **Join Room**
5. Click **OK**

→ Ứng dụng sẽ tự động tìm và kết nối đến host.

### Bước 3: Vẽ và Cộng Tác

Sau khi kết nối thành công:

1. **Chọn công cụ vẽ** từ Tool Panel (bên trái):
   - Click vào icon công cụ (Pencil, Line, Rectangle, v.v.)

2. **Chọn màu** từ Color Panel:
   - Click vào color picker để chọn màu
   - Điều chỉnh stroke width bằng slider

3. **Vẽ trên canvas**:
   - Click và kéo chuột để vẽ
   - Shapes sẽ tự động sync đến tất cả peer khác

4. **Xem peer khác vẽ**:
   - Shapes từ peer khác sẽ xuất hiện trên canvas của bạn
   - Hover vào shape để xem ai đã vẽ (hiển thị sau 2 giây)

### Bước 4: Sử Dụng Các Tính Năng Nâng Cao

#### Layer Management
- **Tạo layer mới**: Click "New Layer" trong Layer Panel
- **Đổi tên layer**: Double-click vào tên layer
- **Ẩn/hiện layer**: Click vào icon mắt 👁️
- **Điều chỉnh opacity**: Kéo slider opacity
- **Merge layers**: Chọn layer → Click "Merge Down"

#### File Operations
- **Save Project**: File → Save Project (Ctrl+S)
- **Load Project**: File → Load Project (Ctrl+O)
- **Export PNG**: File → Export → PNG
- **Export SVG**: File → Export → SVG

#### Zoom & Pan
- **Zoom In**: Zoom Panel → Click "+" hoặc Ctrl+Scroll Up
- **Zoom Out**: Zoom Panel → Click "-" hoặc Ctrl+Scroll Down
- **Pan**: Chọn Pan tool → Kéo canvas
- **Zoom to Fit**: Click "Fit Window"

#### Grid & Alignment
- **Bật Grid**: Grid Panel → Toggle "Show Grid"
- **Snap to Grid**: Toggle "Snap to Grid"
- **Align Shapes**: Chọn shapes → Click align buttons

### Bước 5: Quản Lý Peer

#### Xem Danh Sách Peer
- Status bar hiển thị số lượng peer đang kết nối
- Hover vào peer count để xem danh sách chi tiết

#### Host Actions (Chỉ Host)
- **Kick Guest**: Right-click vào peer trong peer list → "Kick"
- **Đóng Phòng**: File → Exit → Tất cả guest sẽ bị disconnect

#### Guest Actions
- **Rời Phòng**: File → Exit → Chỉ bạn bị disconnect

---

## 🔄 Workflow

### Workflow Tổng Quan

```
┌─────────────────────────────────────────────────────────────┐
│                    STARTUP DIALOG                           │
│  ┌──────────────┐              ┌──────────────┐            │
│  │ Create Room  │              │  Join Room   │            │
│  └──────┬───────┘              └──────┬───────┘            │
│         │                              │                    │
│         ▼                              ▼                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         UDP BROADCAST DISCOVERY                      │  │
│  │  - Broadcast presence every 3s                       │  │
│  │  - Listen for other peers                            │  │
│  │  - Filter by roomId & password                       │  │
│  └──────────────────────────────────────────────────────┘  │
│         │                              │                    │
│         ▼                              ▼                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         TCP CONNECTION ESTABLISHMENT                  │  │
│  │  - Host: Listen on ServerSocket                      │  │
│  │  - Guest: Connect to Host's IP:Port                  │  │
│  │  - Bidirectional ObjectStream                         │  │
│  └──────────────────────────────────────────────────────┘  │
│         │                              │                    │
│         ▼                              ▼                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         MAIN FRAME - DRAWING CANVAS                   │  │
│  │  - Draw shapes locally                                │  │
│  │  - Broadcast shapes to all peers                      │  │
│  │  - Receive shapes from peers                          │  │
│  │  - Render on canvas                                  │  │
│  └──────────────────────────────────────────────────────┘  │
│         │                              │                    │
│         ▼                              ▼                    │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         REAL-TIME SYNCHRONIZATION                    │  │
│  │  - Shape updates                                     │  │
│  │  - Clear/Undo actions                                │  │
│  │  - Layer changes                                     │  │
│  └──────────────────────────────────────────────────────┘  │
```

### Workflow Chi Tiết: Tạo Phòng và Join

#### Host Workflow
```
1. User chọn "Create Room"
   ↓
2. Nhập Room Code + Password
   ↓
3. MainFrame khởi tạo:
   - Tạo ServerSocket (port tự động)
   - Start PeerDiscovery (UDP broadcast)
   - Start acceptConnections thread
   ↓
4. PeerDiscovery broadcast presence:
   - Gửi UDP packet mỗi 3 giây
   - Chứa: peerId, name, listenPort, roomId, password
   ↓
5. Khi có guest join:
   - Nhận UDP packet từ guest
   - Match roomId & password
   - Auto-connect TCP đến guest
   ↓
6. TCP Connection established:
   - Gửi HELLO message
   - Gửi initial SHAPES sync (nếu có)
   ↓
7. Ready for collaboration
```

#### Guest Workflow
```
1. User chọn "Join Room"
   ↓
2. Nhập Room Code + Password (phải khớp với host)
   ↓
3. MainFrame khởi tạo:
   - Tạo ServerSocket (port tự động)
   - Start PeerDiscovery (UDP broadcast + listen)
   ↓
4. PeerDiscovery:
   - Broadcast presence (để host biết)
   - Listen UDP packets từ host
   ↓
5. Khi nhận UDP packet từ host:
   - Verify roomId & password match
   - Extract host IP:Port
   - Auto-connect TCP đến host
   ↓
6. TCP Connection established:
   - Gửi HELLO message
   - Nhận HELLO từ host
   - Nhận initial SHAPES sync
   ↓
7. Ready for collaboration
```

### Workflow Chi Tiết: Vẽ và Sync

```
┌─────────────┐                    ┌─────────────┐
│   Peer A    │                    │   Peer B    │
│  (Drawing)  │                    │ (Viewing)   │
└──────┬──────┘                    └──────┬──────┘
       │                                  │
       │ 1. User draws shape              │
       │    (mouse drag)                  │
       │                                  │
       ▼                                  │
┌──────────────────┐                     │
│ DrawingCanvas    │                     │
│ - startDrawing() │                     │
│ - drawShape()    │                     │
│ - finishShape()  │                     │
└──────┬───────────┘                     │
       │                                  │
       │ 2. Shape created                 │
       │    (local render)                │
       │                                  │
       ▼                                  │
┌──────────────────┐                     │
│ onShapeDrawn     │                     │
│ callback         │                     │
└──────┬───────────┘                     │
       │                                  │
       │ 3. Broadcast shape               │
       │                                  │
       ▼                                  │
┌─────────────────────────────────────────┐
│ MessageHandler.broadcastShapes()         │
│ - Create SHAPES message                 │
│ - Send to all connected peers           │
└──────┬──────────────────────────────────┘
       │                                  │
       │ 4. TCP send                      │
       │                                  │
       ▼                                  │
┌──────────────────┐                     │
│ PeerConnection  │                     │
│ - writeMessages │                     │
│ - ObjectStream  │                     │
└──────┬───────────┘                     │
       │                                  │
       │ 5. Network transmission          │
       │                                  │
       └──────────────────────────────────┘
                                         │
                                         ▼
                              ┌──────────────────┐
                              │ PeerConnection   │
                              │ - readMessages   │
                              │ - ObjectStream   │
                              └──────┬───────────┘
                                     │
                                     │ 6. Receive message
                                     │
                                     ▼
                              ┌──────────────────┐
                              │ MessageHandler   │
                              │ - handleMessage  │
                              │ - onShapesReceived│
                              └──────┬───────────┘
                                     │
                                     │ 7. Process on EDT
                                     │
                                     ▼
                              ┌──────────────────┐
                              │ DrawingCanvas    │
                              │ - drawRemoteShape│
                              │ - In-place update│
                              │ - Repaint        │
                              └──────────────────┘
                                     │
                                     │ 8. Shape rendered
                                     │
                                     ▼
                              ┌──────────────────┐
                              │ Canvas Display   │
                              │ (Peer B sees it) │
                              └──────────────────┘
```

### Workflow: Clear và Undo Sync

```
Peer A clicks "Clear" button
    ↓
MainFrame.performClearWithBroadcast()
    ↓
1. Clear local canvas
    ↓
2. MessageHandler.broadcastClear()
    ↓
3. Send CLEAR message to all peers
    ↓
    └──────────────┬──────────────┘
                   │
        ┌──────────┴──────────┐
        │                      │
        ▼                      ▼
   Peer B                 Peer C
   (receive CLEAR)       (receive CLEAR)
        │                      │
        ▼                      ▼
   onClearReceived        onClearReceived
        │                      │
        ▼                      ▼
   canvas.clearCanvas()  canvas.clearCanvas()
```

Tương tự với **Undo**:
- Peer A clicks "Undo"
- Broadcast UNDO message với senderId
- Các peer khác nhận → `canvas.undoForPeer(senderId)`

---

## 🔧 Troubleshooting

### Vấn Đề: Không Tìm Thấy Phòng

**Triệu Chứng**: Join room nhưng hiện "Room not found"

**Nguyên Nhân**:
- Room code hoặc password không khớp
- Host chưa chạy hoặc chưa tạo phòng
- Firewall chặn UDP port 55556
- Không cùng mạng LAN

**Giải Pháp**:
1. Kiểm tra room code và password trên cả 2 máy phải giống nhau
2. Đảm bảo host đã chạy và chọn "Create Room"
3. Tắt firewall tạm thời để test
4. Kiểm tra cả 2 máy cùng mạng LAN (ping IP)

### Vấn Đề: Kết Nối Thành Công Nhưng Không Thấy Shapes

**Triệu Chứng**: Connection thành công nhưng vẽ không sync

**Nguyên Nhân**:
- Firewall chặn TCP ports
- Code version khác nhau giữa 2 máy (serialVersionUID mismatch)
- Connection bị đóng do lỗi

**Giải Pháp**:
1. Kiểm tra firewall cho phép TCP connections
2. Build lại project trên cả 2 máy với code mới nhất
3. Xem log console để tìm lỗi cụ thể

### Vấn Đề: Drawing Lag/Delay

**Triệu Chứng**: Bên vẽ mượt nhưng bên xem bị lag

**Nguyên Nhân**:
- Network latency
- Quá nhiều repaint
- Không có throttling

**Giải Pháp**:
- Code đã có optimizations:
  - In-place updates cho remote shapes
  - Repaint debouncing (60fps max)
  - Pencil throttling (2 points threshold)
- Nếu vẫn lag, kiểm tra network quality

### Vấn Đề: "Address already in use" khi chạy nhiều instance

**Triệu Chứng**: Lỗi khi chạy nhiều instance trên cùng máy

**Giải Pháp**:
- Code đã xử lý: chỉ một instance listen UDP, các instance khác chỉ broadcast
- Nếu vẫn lỗi, đảm bảo chỉ một instance chọn "Create Room"

### Vấn Đề: Host Disconnect → Guest tự động disconnect

**Triệu Chứng**: Host đóng app → Guest nhận "Room closed"

**Giải Pháp**:
- Đây là behavior đúng: Host đóng phòng → gửi DISCONNECT message với reason "ROOM_CLOSED"
- Guest nhận message → tự động quay về Startup Dialog

---

## 🏗️ Build & Deploy

### Build Scripts

**Windows (`build.bat`):**
```batch
@echo off
mvn clean package
if %ERRORLEVEL% EQU 0 (
    echo Build successful!
    echo JAR file: target\p2p-whiteboard.jar
) else (
    echo Build failed!
)
```

**Linux/macOS (`build.sh`):**
```bash
#!/bin/bash
mvn clean package
if [ $? -eq 0 ]; then
    echo "Build successful!"
    echo "JAR file: target/p2p-whiteboard.jar"
else
    echo "Build failed!"
fi
```

### Maven Commands

```bash
# Clean và build
mvn clean package

# Skip tests
mvn clean package -DskipTests

# Build với dependencies (fat JAR)
mvn clean package assembly:single

# Install vào local repository
mvn install
```

### Output Files

Sau khi build, bạn sẽ có:
- `target/p2p-whiteboard.jar` - Standard JAR (cần dependencies)
- `target/p2p-whiteboard-jar-with-dependencies.jar` - Fat JAR (khuyến nghị)

### Deploy

1. **Copy JAR file** đến các máy cần chạy
2. **Đảm bảo Java 17+** đã cài đặt
3. **Chạy**: `java -jar p2p-whiteboard.jar`
4. **Firewall**: Cho phép UDP 55556 và TCP ports

---

## 📚 Tài Liệu Tham Khảo

- [ARCHITECTURE.md](./ARCHITECTURE.md) - Kiến trúc và cơ chế hoạt động chi tiết
- [FEATURES_IMPLEMENTED.md](./FEATURES_IMPLEMENTED.md) - Danh sách tính năng đã implement
- [TESTING.md](./TESTING.md) - Hướng dẫn testing

---

## 📝 License

Project này được phát triển cho mục đích giáo dục và nghiên cứu.

---

## 👥 Contributors

Developed with ❤️ using pure Java and P2P networking.
