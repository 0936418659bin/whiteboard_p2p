# PHÂN TÍCH KIẾN TRÚC HỆ THỐNG P2P WHITEBOARD

## 1. Tổng quan
Hệ thống bảng trắng P2P (Peer-to-Peer) này được thiết kế để cho phép nhiều người dùng vẽ và cộng tác trên cùng một bảng vẽ ảo theo thời gian thực. Hệ thống sử dụng mô hình mạng ngang hàng (P2P) thuần túy, nơi các peer (máy ngang hàng) kết nối trực tiếp với nhau mà không cần máy chủ trung tâm.

## 2. Kiến trúc mạng P2P thuần túy

### 2.1. Đặc điểm của P2P thuần túy
- **Không có máy chủ trung tâm**: Không có điểm trung tâm nào kiểm soát toàn bộ hệ thống.
- **Kết nối trực tiếp**: Các peer kết nối trực tiếp với nhau để trao đổi dữ liệu.
- **Tự tổ chức**: Các peer tự động phát hiện và kết nối với nhau.
- **Phân tán**: Dữ liệu và xử lý được phân tán trên tất cả các peer tham gia.

### 2.2. Kiểm tra tính P2P thuần túy của hệ thống
Sau khi phân tích mã nguồn, hệ thống này là **P2P thuần túy** vì:

1. **Không có máy chủ trung tâm**:
   - Không có thành phần nào đóng vai trò trung tâm điều khiển toàn bộ hệ thống.
   - Mỗi peer hoạt động độc lập và bình đẳng.

2. **Cơ chế phát hiện peer (Peer Discovery)**:
   - Sử dụng UDP broadcast để phát hiện các peer khác trong cùng mạng cục bộ.
   - Mỗi peer tự động phát sóng sự hiện diện của mình và lắng nghe các peer khác.
   - Có hỗ trợ phòng (room) để phân nhóm các peer.

3. **Kết nối trực tiếp giữa các peer**:
   - Sau khi phát hiện, các peer thiết lập kết nối TCP trực tiếp với nhau.
   - Dữ liệu được truyền trực tiếp giữa các peer mà không qua trung gian.

4. **Đồng bộ hóa dữ liệu phân tán**:
   - Mỗi peer duy trì trạng thái riêng của bảng vẽ.
   - Thay đổi từ một peer được phát tán đến tất cả các peer khác.
   - Sử dụng cơ chế timestamp để xử lý xung đột.

## 3. Các thành phần chính

### 3.1. Peer Discovery
- **Chức năng**: Phát hiện các peer khác trong mạng cục bộ.
- **Công nghệ**: Sử dụng UDP broadcast trên cổng 55556.
- **Cơ chế hoạt động**:
  - Mỗi peer định kỳ gửi thông điệp broadcast chứa thông tin định danh (ID, tên, cổng, phòng, mật khẩu).
  - Đồng thời lắng nghe các thông điệp từ peer khác.
  - Chỉ kết nối với các peer trong cùng phòng và có mật khẩu khớp (nếu có).

### 3.2. Peer Connection
- **Chức năng**: Quản lý kết nối TCP giữa các peer.
- **Đặc điểm**:
  - Mỗi kết nối có 2 luồng riêng biệt: đọc và ghi.
  - Hỗ trợ gửi nhận các đối tượng tuần tự hóa qua ObjectInputStream/ObjectOutputStream.
  - Có cơ chế đánh dấu tin nhắn đã gửi để tránh lặp.

### 3.3. Message Handler
- **Chức năng**: Xử lý các loại tin nhắn khác nhau giữa các peer.
- **Các loại tin nhắn hỗ trợ**:
  - HELLO: Thiết lập kết nối ban đầu.
  - SHAPES: Dữ liệu hình vẽ.
  - CHAT: Tin nhắn trò chuyện.
  - CLEAR: Xóa toàn bộ bảng vẽ.
  - UNDO: Hoàn tác thao tác.
  - PEER_LIST: Danh sách các peer đang kết nối.
  - DISCONNECT: Ngắt kết nối.

### 3.4. Đồng bộ hóa dữ liệu
- **Cơ chế đồng bộ hóa**:
  - Mỗi thay đổi (vẽ, xóa, hoàn tác) được đóng gói thành tin nhắn và gửi đến tất cả các peer khác.
  - Sử dụng timestamp để xác định thứ tự các thay đổi.
  - Mỗi peer tự áp dụng thay đổi vào bản sao cục bộ của mình.

## 4. Luồng hoạt động

### 4.1. Khi một peer khởi động
1. Khởi tạo giao diện người dùng.
2. Bắt đầu quá trình phát hiện peer (PeerDiscovery).
3. Lắng nghe kết nối TCP từ các peer khác.
4. Khi phát hiện peer mới, thiết lập kết nối TCP với peer đó.

### 4.2. Khi có thao tác vẽ
1. Người dùng thực hiện thao tác vẽ trên bảng.
2. Hệ thống đóng gói thông tin hình vẽ thành tin nhắn SHAPES.
3. Gửi tin nhắn đến tất cả các peer đang kết nối.
4. Các peer nhận được tin nhắn, cập nhật bảng vẽ cục bộ.

### 4.3. Khi có peer mới tham gia
1. Peer mới phát hiện các peer hiện có qua UDP broadcast.
2. Thiết lập kết nối TCP với các peer hiện có.
3. Gửi yêu cầu đồng bộ hóa trạng thái hiện tại.
4. Nhận và áp dụng toàn bộ trạng thái từ các peer khác.

## 5. Đánh giá

### 5.1. Ưu điểm
- **Không phụ thuộc vào máy chủ**: Hoạt động độc lập, không cần cơ sở hạ tầng trung tâm.
- **Khả năng mở rộng**: Thêm peer mới dễ dàng mà không ảnh hưởng đến hiệu suất tổng thể.
- **Bảo mật**: Dữ liệu chỉ được chia sẻ trong nhóm peer đã xác thực (nếu có mật khẩu).
- **Hiệu suất**: Giảm độ trễ do truyền dữ liệu trực tiếp giữa các peer.

### 5.2. Hạn chế
- **Phạm vi hoạt động**: Chỉ hoạt động trong mạng cục bộ do sử dụng UDP broadcast.
- **Khả năng mở rộng mạng lớn**: Có thể gặp vấn đề khi số lượng peer tăng lên do mỗi peer cần duy trì kết nối với tất cả các peer khác.
- **Đồng bộ hóa phức tạp**: Cần xử lý các trường hợp xung đột khi nhiều người cùng chỉnh sửa.

## 6. Kết luận
Hệ thống bảng trắng P2P này là một ví dụ điển hình về ứng dụng P2P thuần túy, nơi các peer hoạt động độc lập và giao tiếp trực tiếp với nhau mà không cần máy chủ trung tâm. Hệ thống sử dụng kết hợp UDP cho việc phát hiện peer và TCP cho việc truyền dữ liệu đáng tin cậy, đảm bảo đồng bộ hóa dữ liệu giữa các peer một cách hiệu quả.

Mặc dù có một số hạn chế về khả năng mở rộng và phạm vi hoạt động, hệ thống này phù hợp cho các tình huống cộng tác trong nhóm nhỏ trong cùng mạng nội bộ, nơi yêu cầu về bảo mật và độ trễ thấp được ưu tiên.
