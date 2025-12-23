# P2P Whiteboard Application

## Mô tả dự án

Đây là một ứng dụng bảng trắng (whiteboard) hoạt động theo mô hình peer-to-peer (P2P), cho phép nhiều người dùng cùng vẽ và tương tác trên cùng một bảng vẽ thời gian thực. Ứng dụng được phát triển bằng Java và sử dụng các giao thức mạng để đồng bộ hóa dữ liệu giữa các peer.

## Kiến trúc hệ thống

### Mô hình mạng P2P
- **Mô hình lai (Hybrid P2P)**: Kết hợp giữa mô hình ngang hàng và máy chủ trung tâm
- **Phát hiện peer**: Sử dụng cơ chế broadcast hoặc server để phát hiện các peer trong mạng LAN
- **Đồng bộ hóa dữ liệu**: Sử dụng cơ chế Operational Transformation (OT) để giải quyết xung đột khi nhiều người cùng chỉnh sửa

### Các thành phần chính

#### 1. Giao diện người dùng
- `MainFrame`: Cửa sổ chính của ứng dụng
- `DrawingCanvas`: Khu vực vẽ chính
- `ToolPanel`: Thanh công cụ vẽ
- `ChatPanel`: Khung chat giữa các peer
- `LayerPanel`: Quản lý các lớp vẽ (layers)
- `ColorPanel`: Chọn màu sắc và độ dày nét vẽ

#### 2. Xử lý đồ họa
- `DrawingTool`: Quản lý các công cụ vẽ (bút, đường thẳng, hình chữ nhật, hình tròn, văn bản)
- `Shape`: Lớp cơ sở cho các đối tượng đồ họa
- `DrawingHistory`: Lưu trữ lịch sử các thao tác để hỗ trợ chức năng Undo/Redo
- `LayerManager`: Quản lý các lớp vẽ
- `SelectionManager`: Xử lý chọn và di chuyển đối tượng

#### 3. Mạng và đồng bộ hóa
- `PeerConnection`: Quản lý kết nối giữa các peer
- `NetworkProtocol`: Định nghĩa cấu trúc tin nhắn và giao thức trao đổi dữ liệu
- `PeerDiscovery`: Tìm kiếm và kết nối với các peer khác trong mạng
- `MessageHandler`: Xử lý các loại tin nhắn khác nhau
- `StateManager`: Quản lý trạng thái đồng bộ giữa các peer
- `ConflictResolver`: Giải quyết xung đột khi có nhiều thay đổi đồng thời

## Cơ chế hoạt động

### 1. Thiết lập kết nối
1. Khi khởi động, mỗi peer sẽ:
   - Mở một cổng để lắng nghe kết nối đến
   - Phát tin broadcast để tìm các peer khác trong mạng LAN
   - Kết nối với các peer đã biết

2. Quá trình bắt tay (handshake):
   - Peer gửi tin nhắn `HELLO` chứa thông tin định danh
   - Peer nhận phản hồi `ACK` để xác nhận kết nối
   - Đồng bộ hóa trạng thái ban đầu

### 2. Đồng bộ hóa bản vẽ
1. Khi người dùng thực hiện thao tác vẽ:
   - Ứng dụng tạo đối tượng `Shape` tương ứng
   - Thêm vào lịch sử vẽ (`DrawingHistory`)
   - Đóng gói thành tin nhắn `SHAPES` và gửi đến tất cả các peer đang kết nối

2. Khi nhận được tin nhắn `SHAPES`:
   - Giải mã dữ liệu nhận được
   - Áp dụng các thay đổi vào bản vẽ hiện tại
   - Cập nhật giao diện người dùng

### 3. Giải quyết xung đột
1. Sử dụng cơ chế Operational Transformation (OT):
   - Mỗi thao tác được gán một timestamp và version
   - Khi có xung đột, `ConflictResolver` sẽ dựa vào timestamp để xác định thứ tự áp dụng
   - Đảm bảo tính nhất quán cuối cùng (eventual consistency)

2. Đồng bộ hóa toàn bộ trạng thái:
   - Khi một peer mới tham gia, có thể yêu cầu đồng bộ toàn bộ trạng thái hiện tại
   - Peer hiện có sẽ gửi toàn bộ dữ liệu vẽ thông qua tin nhắn `SYNC_RESPONSE`

### 4. Truyền tin nhắn chat
1. Khi người dùng gửi tin nhắn:
   - Tạo tin nhắn loại `CHAT`
   - Gửi đến tất cả các peer đang kết nối
   - Hiển thị trong khung chat

## Hướng dẫn biên dịch và chạy

### Yêu cầu hệ thống
- JDK 11 hoặc cao hơn
- Maven 3.6.3 hoặc cao hơn

### Các bước cài đặt
1. Clone repository
2. Mở terminal trong thư mục dự án
3. Chạy lệnh: `mvn clean install`
4. Chạy ứng dụng: `mvn exec:java -Dexec.mainClass="com.example.Main"`

## Hướng dẫn sử dụng
1. Khởi động ứng dụng
2. Nhập tên người dùng và cổng mặc định (ví dụ: 8080)
3. Để kết nối với peer khác, nhấn "Connect to Peer" và nhập địa chỉ IP và cổng của peer đó
4. Bắt đầu vẽ và tương tác với các peer khác

## Các tính năng nổi bật
- Vẽ tự do với nhiều công cụ khác nhau
- Hỗ trợ nhiều lớp vẽ (layers)
- Chức năng Undo/Redo
- Chat trực tuyến giữa các peer
- Phóng to/thu nhỏ và di chuyển bảng vẽ
- Lưu và mở file bản vẽ
- Hỗ trợ kéo thả hình ảnh

## Hạn chế và hướng phát triển

### Hạn chế hiện tại
- Chỉ hoạt động trong mạng LAN
- Chưa hỗ trợ mã hóa dữ liệu
- Giới hạn số lượng kết nối đồng thời

### Hướng phát triển
- Thêm hỗ trợ kết nối qua Internet
- Tích hợp bảo mật (mã hóa dữ liệu, xác thực người dùng)
- Hỗ trợ thêm nhiều định dạng xuất file
- Cải thiện hiệu năng với lượng lớn dữ liệu
- Thêm tính năng lưu trữ đám mây

## Tác giả
[Your Name] - [Your Contact Information]

## Giấy phép
Dự án được phát triển dưới giấy phép MIT.
