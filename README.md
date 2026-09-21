# Game Bắn Tàu Online

Đồ án môn **Lập trình mạng máy tính**

Game bắn tàu (Battleship) nhiều người chơi qua mạng, xây dựng bằng **Java Socket (TCP)** và giao diện **Swing**. Server đa luồng, hỗ trợ nhiều phòng chơi song song.

Toàn bộ giao thức tầng ứng dụng do nhóm tự thiết kế, không sử dụng thư viện trung gian nào.

---

## Công nghệ sử dụng

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ | Java 17+ |
| Tầng giao vận | TCP, cổng 5000 |
| Tầng ứng dụng | Giao thức tự định nghĩa, dạng text |
| Giao diện | Java Swing |
| Đồng thời | Thread pool, mô hình thread-per-connection |
| Thư viện ngoài | Không có |

---

## Cấu trúc thư mục

```
src/bantau/
├── common/     Lớp dùng chung cho cả server và client
├── server/     Server đa luồng, quản lý phòng và ván đấu
└── client/     Giao diện người chơi
```

---

## Cách chạy

**Yêu cầu:** JDK 17 trở lên.

```bash
# Biên dịch
javac -encoding UTF-8 -d bin src/bantau/**/*.java

# Chạy server
java -cp bin bantau.server.ServerMain

# Chạy client (mở nhiều cửa sổ)
java -cp bin bantau.client.ClientMain
```

Trong VS Code: mở file có hàm `main`, bấm **Run** phía trên hàm đó.

**Chơi trên hai máy khác nhau:** máy chạy server xem địa chỉ IP bằng `ipconfig`, máy còn lại nhập IP đó khi đăng nhập. Cần mở cổng 5000 trên tường lửa.

---

## Tiến độ

- [x] **Giai đoạn 1** — Echo server, một client
- [x] **Giai đoạn 2** — Server đa luồng, nhiều client, broadcast
- [ ] **Giai đoạn 3** — Giao thức bản tin có cấu trúc, đăng nhập
- [ ] **Giai đoạn 4** — Lobby và phòng chơi
- [ ] **Giai đoạn 5** — Bàn cờ và logic đặt tàu
- [ ] **Giai đoạn 6** — Ván đấu, lượt bắn, điều kiện thắng
- [ ] **Giai đoạn 7** — Client console
- [ ] **Giai đoạn 8** — Giao diện Swing
- [ ] **Giai đoạn 9** — Hoàn thiện

---

## Tài liệu

Đặc tả giao thức đầy đủ: [`docs/PROTOCOL.md`](docs/PROTOCOL.md)

---

## Tác giả

Lê Mai Quốc Khánh