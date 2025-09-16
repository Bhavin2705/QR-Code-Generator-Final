# QR Generator App

## Overview
A modern web application to generate, scan, and manage QR codes. Supports text, images, and documents, with history, download, and sharing features. Built with Spring Boot (Java), MySQL, and a responsive frontend.

## Features
- Generate QR codes for text, images, and documents
- Scan QR codes from uploaded images
- Download QR codes as images
- View recent activity/history
- Delete and regenerate QR codes
- Responsive UI with dark/light mode
- Accessible from any device on the same network

## Requirements
- Java 17+
- Maven
- MySQL (running and accessible)
- Node.js (optional, for frontend development)

## Setup Instructions

### 1. Clone the Repository
```sh
git clone https://github.com/Bhavin2705/QR-Code-Generator-Final.git
cd QR-Code-Generator-Final
```

### 2. Configure Database
- Create a MySQL database named `qrapp`.
- Update `src/main/resources/application.properties` with your MySQL username and password.

### 3. Build the Project
```sh
mvn clean install
```

### 4. Run the Application
```sh
mvn spring-boot:run
```

### 5. Access the App
- **Find your computer’s local IP address:**
  - On Windows, run `ipconfig` and look for `IPv4 Address` (e.g., `192.168.1.5`).
- **Open the app in your browser:**
  ```
  http://<your-ip>:8081
  ```
  Example: `http://192.168.1.5:8081`

- **To access from other devices on the same network:**
  - Use the same URL with your computer’s IP.

### 6. Common Issues
- **Firewall:** Allow inbound connections on port 8081.
- **Spring Boot binding:** The app is configured to listen on all interfaces (`server.address=0.0.0.0`).
- **Mobile hotspot:** Some hotspots block local traffic; use Wi-Fi for best results.

## Project Structure
```
src/
  main/
    java/com/qrapp/...
    resources/
      static/         # Frontend files (HTML, JS, CSS)
      templates/      # Thymeleaf templates
      application.properties
  test/
    java/com/qrapp/...
pom.xml
```

## License
MIT
