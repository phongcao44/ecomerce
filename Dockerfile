# Dùng image JDK làm base
FROM openjdk:17-jdk-slim

# Tạo thư mục chứa app
WORKDIR /app

# Copy file jar vào container
COPY build/libs/*.jar app.jar

# Chạy app
CMD ["java", "-jar", "app.jar"]
