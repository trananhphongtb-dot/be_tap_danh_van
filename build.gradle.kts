// Phiên bản plugin lấy từ mẫu quoc-te-app, chỉ BỎ plugin google-services
// vì app này chạy offline hoàn toàn, không Firebase, không đăng nhập.
plugins {
    id("com.android.application") version "8.11.1" apply false
    id("org.jetbrains.kotlin.android") version "2.1.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21" apply false
}
