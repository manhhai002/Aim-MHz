# Aim Tool Simulator V1.0 (Android Studio)

Prototype độc lập cho bài toán đường ngắm bida. **Không kết nối, đọc màn hình, sửa dữ liệu, hoặc hiển thị trên game 8 Ball Pool.**

## Tính năng có trong mã

1. Bàn bida giả lập bằng Android Canvas, khung tọa độ cố định 1000 x 500.
2. OpenCV (`org.opencv:opencv:4.14.0`) nhận ảnh `Bitmap` do chính TableView vẽ, phân đoạn 8 màu bi, tìm contour và tính tâm bi. Không truyền trực tiếp tọa độ bi từ bộ mô phỏng cho bộ dự đoán.
3. ShotPredictor giải tích ray-circle, ray-wall; dự đoán bi cái, bi mục tiêu, bi cái sau va chạm, và 0–2 lần phản xạ băng.
4. Lớp TrajectoryOverlayView phủ trong **cửa sổ của ứng dụng** bằng FrameLayout. Không dùng WindowManager, SYSTEM_ALERT_WINDOW, MediaProjection, accessibility, root, hook hay can thiệp trò chơi khác.
5. Mô phỏng một cú đánh đơn giản với quán tính, ma sát lăn, phản xạ băng, va chạm bi và lỗ bi.
6. Thử nghiệm JUnit cho ShotPredictor.

## Cách mở và chạy

1. Giải nén và chọn **File > Open** trong Android Studio, trỏ tới thư mục `AimToolSimulator`.
2. Dùng **JDK 17** và cài **Android SDK Platform 36** (qua SDK Manager).
3. Khi đồng bộ, Android Studio tải Gradle 8.13, Kotlin 2.2.21 và OpenCV 4.14.0 (cần Internet lần đầu). Project có `gradlew`/`gradlew.bat` và một Java bootstrap tối giản, mã nguồn công khai ở `tools/bootstrap/GradleWrapperMain.java`. Bootstrap được tạo cho bản demo này (không phải official Gradle Wrapper JAR), chỉ tải bản Gradle 8.13 chính thức và kiểm tra SHA-256. Bạn có thể thay nó bằng wrapper chính thức trong Android Studio nếu cần.
4. Chọn máy thật hoặc emulator Android API 26+, chọn cấu hình **app**, nhấn **Run**.
5. Để nhận diện bi, app sẽ hiển thị `OpenCV: 8 bi` (giảm khi bi rơi lỗ). Kéo ngón tay trên bàn để đổi hướng cơ. Đường trắng là tia trước va chạm, vàng là hướng bi mục tiêu, xanh là hướng bi cái sau chạm. Nhấn ĐÁNH THỬ để kiểm tra.
6. Kiểm thử mã tính toán: chạy task `:app:testDebugUnitTest`.

## Cấu trúc

- `app/src/main/java/com/aimsimulator/MainActivity.kt`: UI, worker OpenCV, scan loop.
- `ui/TableView.kt`: bảng ảo, input hướng, bitmap đầu vào OpenCV.
- `vision/BallDetector.kt`: thuật toán màu + contour bằng OpenCV.
- `model/Geometry.kt`: vector, bàn bida, dữ liệu bi.
- `model/ShotPredictor.kt`: phương trình đường đi và va chạm.
- `model/SimulatorEngine.kt`: mô phỏng động lực học.
- `ui/TrajectoryOverlayView.kt`: vẽ đường ngắm trong app.
- `app/src/test/.../ShotPredictorTest.kt`: kiểm thử toán học.

## Giới hạn rõ ràng

- Tính toán chỉ là mô hình hình học cơ bản: chưa có xoáy, ma sát theo tốc độ, lực tác động không chính tâm, kích thước/mặt cắt băng thực tế hay hiệu chỉnh theo 8 Ball Pool.
- Nhận diện là **color segmentation theo bảng màu được biết trước** cho ảnh của chính simulator, không phải AI học sâu hay mô hình tổng quát cho mọi hình ảnh/skin. Bi trùng nhau, màu nền thay đổi hoặc mờ có thể khiến nhận diện sai.
- Dự đoán đường bi mục tiêu dừng ở thành bàn, chưa dự đoán va chạm chuỗi của bi mục tiêu; khi bi rơi lỗ, độ chính xác mô hình chỉ mang tính minh họa.
- Ở phiên bản này không xin quyền chụp màn hình ứng dụng khác và không tạo overlay hệ thống.
- Chưa thể biên dịch APK và chạy bản Android thực tế trong môi trường soạn mã do thiếu Android SDK và không có mạng để tải Gradle. Phần mô hình toán học đã được biên dịch và kiểm tra độc lập bằng `kotlinc`/Java; thuật toán nhận diện màu được kiểm thử trên frame sinh tổng hợp với OpenCV Python. Bộ JUnit Android có trong project nhưng chưa chạy ở đây.

## Gợi ý phát triển V2 (trên bàn tự tạo)

- Hiệu chỉnh homography/bàn từ ảnh camera, nhận diện theo Hough circle + HSV để không phụ thuộc palette.
- Predict va chạm bi thứ 2, đánh giá đường vào lỗ, mô hình trượt/lăn/xoáy, benchmark dự đoán với simulation.
- Hạ độ phân giải nhận diện để tiết kiệm pin; thêm FPS monitor, debounce cảm ứng.
- Tối ưu hệ tọa độ, hỗ trợ dọc và landscape, UI cho nhiều cỡ màn hình.

## Phiên bản

- AGP 8.13.2; Gradle 8.13; Kotlin 2.2.21; OpenCV 4.14.0; compileSdk 36; minSdk 26.

Mục đích: học tập, thực hành thị giác máy tính và vật lý bida trong môi trường độc lập.

## Kiểm chứng cục bộ khi tạo project

- Kotlin model: biên dịch thành công bằng kotlinc; smoke test 4 tình huống PASSED.
- OpenCV segmentation (kiểm thử bản Python tương đương trên bitmap tổng hợp): nhận đủ 8/8 bi.
- Gradle/Android build: chưa kiểm chứng do môi trường tạo file không có Android SDK/mạng.
- Không có APK dựng sẵn; mở project trong Android Studio và Build > Build APK(s).

## Tạo APK tự động trên GitHub (không cần Android Studio)

Project có sẵn workflow `.github/workflows/build-apk.yml` để GitHub tự tải Android SDK, Gradle và OpenCV, rồi build APK debug. Khi upload toàn bộ project (giữ nguyên thư mục `.github`) lên repository GitHub, chọn **Actions > Build Android APK > Run workflow**. Nếu build thành công, vào phần **Artifacts** của lần chạy đó tải `AimToolSimulator-debug-apk.zip`, giải nén và cài `app-debug.apk` trên Android 8+.

**Trạng thái:** workflow được tạo nhưng **chưa được chạy xác nhận** do máy tạo project không có Android SDK và mạng tải phụ thuộc. Tệp ZIP mã nguồn không phải APK cài đặt trực tiếp. APK debug tự ký bởi Android Gradle Plugin chỉ phù hợp thử nghiệm.
