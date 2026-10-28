# Đánh Vần — app Android đánh vần tiếng Việt (offline)

App thực hành đánh vần cho học sinh mới học, chỉ dùng từ 2 chữ cái:
phụ âm đầu + nguyên âm (+ dấu), không ch/gh/kh/ng/nh/ph/th/tr.

## Trạng thái tên và package — TẠM, chưa chốt
- Package tạm: `vn.danhvan.tieuhoc` (dùng tạm, chưa chốt tên app chính thức).
- Tên hiển thị tạm: "Đánh Vần".
- Khi chốt tên thật, đổi `applicationId` / `namespace` trong
  `app/build.gradle.kts` và `app_name` trong `app/src/main/res/values/strings.xml`.

## Chạy app
1. Mở thư mục này bằng Android Studio (mẫu quoc-te-app cũng không có
   Gradle wrapper sẵn trong repo, Android Studio sẽ tự xử lý wrapper khi sync).
2. Chọn thiết bị / máy ảo Android (minSdk 24), bấm Run.
3. Mở app là vào thi luôn, không màn hình đăng nhập, không tài khoản.

## Phiên bản dùng
Lấy từ mẫu `quoc-te-app`, chỉ bỏ phần online:
- Android Gradle Plugin 8.11.1, Kotlin 2.1.21, Kotlin Compose plugin 2.1.21.
- Compose BOM 2025.06.00, activity-compose 1.10.1,
  lifecycle-viewmodel-compose 2.9.1, core-ktx 1.16.0, coroutines-android 1.10.2.
- compileSdk / targetSdk 36, minSdk 24, Java 17.
- KHÔNG có: Firebase, google-services, Google Sign-in, Credential Manager,
  Realtime Database, xếp hạng, quảng cáo, mua trong app, chat tự do.

## Dữ liệu — từ đâu, để ở đâu
- `app/src/main/assets/bo-tu-2-chu-cai.json`: copy nguyên vẹn từ
  `~/workspace/research/danh-van/bo-tu-2-chu-cai.json` (196 từ, 160 từ có hình).
  Không sửa nội dung file này trong app; muốn đổi dữ liệu thì đổi ở file gốc rồi copy lại.
- `app/src/main/assets/hinh/DV*.png`: copy từ
  `~/workspace/research/danh-van/hinh/` (160 hình, đặt tên theo id từ).
  App load hình theo id, ví dụ từ `bò` (DV006) load `hinh/DV006.png`.
  Không copy hình cũ (`hinh-cu`), hình mẫu (`hinh-mau`) hay tờ tổng hợp (`hinh-sheets`).

## 7 chặng (theo đúng `meta.chang_thi` trong file dữ liệu)
1. Nghe chọn đúng từ — phát giọng đọc từ, bé chọn đúng từ.
2. Hình chọn đúng từ — hiện hình, bé chọn đúng từ, chỉ từ có hình.
3. Đoạn văn dài — đoạn từ `meta.doan_van_chang3`, từ 10 từ trở lên,
   đúng 2 từ đọc được, đáp án chọn 2 từ, không bấm vào từ trong đoạn.
   SỬA 2026-10-29: đoạn hiện trên 1 BẢNG (không dùng hình trang sách),
   giọng đọc tiếng Việt đọc đoạn văn + câu hỏi, có nút phát lại.
4. Ghép chữ + dấu theo giọng đọc — nghe từ, ghép phụ âm + nguyên âm + dấu,
   có ô "không dấu" cho từ không dấu.
   SỬA 2026-10-29: đúng 9 ô — 3 phụ âm + 3 nguyên âm + 3 dấu, chọn 1 mỗi loại.
   Cách xếp giống chặng 7, chỉ khác chặng 4 là nghe giọng đọc, chặng 7 là nhìn hình.
5. Bắt chữ rơi — giọng đọc yêu cầu 1 chữ cái, các chữ rơi xuống, bé chạm đúng chữ.
   SỬA 2026-10-29: chữ rơi thẳng từ trên xuống, phẳng, đơn giản, tốc độ vừa phải,
   không có hình em bé trong màn chơi. Chữ yêu cầu rơi qua khỏi màn hình mà
   không bắt được tính là 1 câu sai, áp dụng luật điểm chung (sai 2 câu trừ 1).
   TODO trong `ui/StageFalling.kt`: nếu người dùng muốn hụt trừ ngay 1 điểm
   thì đổi đúng chỗ gọi `onAnswer(false, ...)` khi hụt thành trừ điểm trực tiếp.
6. Nghe câu 3 từ chọn đoạn chữ đúng — câu từ `meta.cau_nghe_chang6`,
   đáp án gây nhiễu đổi chủ ngữ / tân ngữ / động từ.
7. Nhìn hình tự xếp chữ — hiện hình (không đọc từ đáp án), bé tự xếp từ
   từ 3 phụ âm + 3 nguyên âm + 3 dấu (dấu đúng là "không dấu" nếu từ không dấu).

## Giọng đọc — bắt buộc tiếng Việt
- `audio/VietnameseTts.kt` dùng `TextToSpeech` với `Locale("vi", "VN")`
  cho mọi từ, câu, đoạn văn. Không dùng giọng tiếng Anh, không để mặc định hệ thống.
- Mọi chặng đều có nút "Nghe lại". Chặng 7 chỉ đọc hướng dẫn chung,
  không đọc từ đáp án (đọc từ sẽ lộ đáp án).
- Nếu máy thiếu dữ liệu giọng vi-VN, app vẫn giữ Locale vi-VN và không
  chuyển sang tiếng Anh; người dùng cần cài giọng tiếng Việt trên máy.

## Luật điểm và luật không lặp
- Luật điểm chung mọi chặng: 10 điểm qua chặng, đúng +1, sai 2 câu trừ 1
  ở câu sai thứ 2. Xem `data/GameRules.kt`.
- TODO (chưa được người dùng xác nhận): bộ đếm sai đang là cộng dồn trong
  1 lần vào chặng, điểm đang có sàn 0. Muốn đổi thì sửa đúng file
  `data/GameRules.kt` (hằng số `FLOOR_AT_ZERO` và hàm `applyAnswer`).
- Luật không lặp: theo từng chặng, theo từng hồ sơ trên máy
  (SharedPreferences `danhvan_offline`, vì không đăng nhập). Chọn ngẫu nhiên
  không lặp câu đã dùng, câu vừa hiện là đã dùng, chỉ reset khi hết toàn bộ
  ngân hàng của chặng đó và phải báo "Đã hết câu mới, bắt đầu vòng mới!".
  Xem `data/ProgressStore.kt`.
- Mở khóa chặng: chặng sau mở khi chặng trước đủ 10 điểm 1 lần.
  Muốn mở hết thì đổi `UnlockRules.UNLOCK_ALL` thành true trong `GameRules.kt`.

## TODO — chưa xong / chưa chốt
- [ ] Chưa có GitHub Actions, chưa có Play signing (chưa cần, app mới ở dạng code).
- [ ] Chưa chốt tên app chính thức và package (đang dùng tạm, xem đầu README).
- [ ] Chưa test build thật trên Android Studio / máy thật trong lần giao này
      (môi trường hiện tại không có Android SDK để chạy `./gradlew assembleDebug`).
      Cần kiểm tra tiếp: mở bằng Android Studio, sync Gradle, Run, kiểm tra
      giọng đọc vi-VN trên máy thật, kiểm tra 7 chặng, điểm, mở khóa, không lặp.
- [ ] Icon app hiện là vector sách mở tạm, chưa phải icon chốt.
- [ ] Chặng 5 (đã sửa 2026-10-29): chữ YÊU CẦU rơi qua khỏi màn hình là hụt = 1 câu sai;
      chỉ các chữ gây nhiễu mới quay lại trên cùng. Chưa có giới hạn thời gian
      riêng ngoài tốc độ rơi (chữ yêu cầu hết màn hình trong ~5–8 giây).
      TODO: hụt trừ ngay 1 điểm hay giữ luật chung — xem `ui/StageFalling.kt`.
