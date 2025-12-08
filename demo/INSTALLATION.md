# Installation & Setup Guide

## Prerequisites

Before installing the P2P Whiteboard application, ensure you have:
1. Java Development Kit (JDK) 17 or higher
2. Maven 3.6 or higher
3. Approximately 100MB free disk space

---

## Windows Setup

### Step 1: Install Java 17

1. Download JDK 17 from [https://jdk.java.net/17/](https://jdk.java.net/17/)
2. Run the installer
3. Follow the installation wizard
4. Accept default installation path

### Step 2: Verify Java Installation

```cmd
java -version
javac -version
```

Should output:
```
java version "17.x.x"
```

### Step 3: Install Maven

1. Download Maven from [https://maven.apache.org/download.cgi](https://maven.apache.org/download.cgi)
2. Extract to a folder (e.g., `C:\tools\maven`)
3. Add Maven to PATH:
   - Right-click "This PC" → Properties
   - Click "Advanced system settings"
   - Click "Environment Variables"
   - Under "System variables", click "New"
   - Variable name: `MAVEN_HOME`
   - Variable value: `C:\tools\maven`
   - Click OK
   - Find `Path` variable, click Edit
   - Click New, add: `%MAVEN_HOME%\bin`
   - Click OK

### Step 4: Verify Maven Installation

```cmd
mvn --version
```

Should output Maven version info.

### Step 5: Build the Project

```cmd
cd e:\P2P\whiteboard\demo
build.bat build
```

Or manually:
```cmd
mvn clean package
```

### Step 6: Run the Application

```cmd
build.bat run
```

Or manually:
```cmd
java -jar target\p2p-whiteboard.jar
```

---

## Linux Setup (Ubuntu/Debian)

### Step 1: Install Java 17

```bash
# Update package list
sudo apt update

# Install JDK 17
sudo apt install openjdk-17-jdk -y

# Verify installation
java -version
```

### Step 2: Install Maven

```bash
# Install Maven
sudo apt install maven -y

# Verify installation
mvn --version
```

### Step 3: Build the Project

```bash
cd ~/P2P/whiteboard/demo
chmod +x build.sh
./build.sh build
```

Or manually:
```bash
mvn clean package
```

### Step 4: Run the Application

```bash
./build.sh run
```

Or manually:
```bash
java -jar target/p2p-whiteboard.jar
```

---

## macOS Setup

### Step 1: Install Java 17 (using Homebrew)

```bash
# Install Homebrew if not already installed
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install JDK 17
brew install openjdk@17

# Link Java
sudo ln -sfn /opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk \
  /Library/Java/JavaVirtualMachines/openjdk-17.jdk

# Verify installation
java -version
```

### Step 2: Install Maven

```bash
brew install maven

# Verify installation
mvn --version
```

### Step 3: Build the Project

```bash
cd ~/P2P/whiteboard/demo
chmod +x build.sh
./build.sh build
```

Or manually:
```bash
mvn clean package
```

### Step 4: Run the Application

```bash
./build.sh run
```

Or manually:
```bash
java -jar target/p2p-whiteboard.jar
```

---

## From Source Code

If you want to build from source files:

### Prerequisites
- Java 17+
- Maven 3.6+
- Git (optional)

### Build Steps

```bash
# Navigate to project
cd demo

# Clean and build
mvn clean package

# Run the application
java -jar target/p2p-whiteboard.jar
```

### Build Options

```bash
# Skip tests
mvn clean package -DskipTests

# Create executable JAR
mvn shade:shade

# Run tests only
mvn test

# Clean build artifacts
mvn clean
```

---

## Docker Setup (Optional)

Create a `Dockerfile`:

```dockerfile
FROM openjdk:17-jdk-slim

WORKDIR /app

COPY target/p2p-whiteboard.jar /app/

EXPOSE 55555 55556 55550-55559

CMD ["java", "-jar", "p2p-whiteboard.jar"]
```

Build and run:

```bash
docker build -t p2p-whiteboard .
docker run -it p2p-whiteboard
```

---

## Troubleshooting Installation

### Issue: "Java command not found"

**Solution**:
- Windows: Add Java to PATH (see Java setup section)
- Linux: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64`
- macOS: `export JAVA_HOME=$(/usr/libexec/java_home -v 17)`

### Issue: "Maven command not found"

**Solution**:
- Windows: Add Maven bin directory to PATH
- Linux: Run `sudo apt install maven -y`
- macOS: Run `brew install maven`

### Issue: "Build fails"

**Solution**:
1. Check Java version: `java -version` (should be 17+)
2. Check Maven version: `mvn --version` (should be 3.6+)
3. Clear Maven cache: `mvn clean`
4. Try building again: `mvn package`

### Issue: "Port already in use"

**Solution**:
- Windows: `netstat -ano | findstr :55555`
- Linux: `lsof -i :55555`
- Kill the process or wait for it to close

### Issue: "Peers not discovered"

**Solution**:
1. Check firewall settings (allow UDP 55555, 55556)
2. Verify machines are on same network
3. Disable VPN if connected
4. Check network adapter settings

### Issue: "Out of memory"

**Solution**:
```bash
# Increase heap size
java -Xms512m -Xmx1g -jar target/p2p-whiteboard.jar
```

---

## Verification

After installation, verify everything works:

### Quick Test

```bash
# Build
mvn clean package

# Check JAR exists
ls target/p2p-whiteboard.jar

# Run with timeout to verify startup
timeout 5 java -jar target/p2p-whiteboard.jar || true
```

### Network Test

Test peer discovery on localhost:

```bash
# Terminal 1
java -jar target/p2p-whiteboard.jar

# Terminal 2 (wait 3 seconds for discovery)
java -jar target/p2p-whiteboard.jar

# Click "Connect Peer" in Terminal 2
# Should see peer from Terminal 1 in the list
```

---

## Configuration

After installation, configure the application:

### Edit config.properties

```properties
# Network ports
discovery.port=55555
listen.port.base=55550

# Canvas settings
canvas.max_history_size=1000

# Performance
render.antialiasing=true
```

### Environment Variables

```bash
# Linux/macOS
export JAVA_OPTS="-Xms256m -Xmx1g"

# Windows
set JAVA_OPTS=-Xms256m -Xmx1g
```

---

## Uninstallation

### Windows

```cmd
# Remove application
rmdir /s /q e:\P2P\whiteboard

# Optional: Remove Java
# Go to Control Panel > Programs > Programs and Features
# Find "Java" and uninstall
```

### Linux/macOS

```bash
# Remove application
rm -rf ~/P2P/whiteboard

# Optional: Remove Java and Maven
# Ubuntu: sudo apt remove openjdk-17-jdk maven
# macOS: brew uninstall openjdk@17 maven
```

---

## Advanced Setup

### Custom JDK

If you have multiple Java versions:

```bash
# Set specific Java version
export JAVA_HOME=/path/to/java17
mvn clean package
```

### Custom Maven Repository

Edit `~/.m2/settings.xml`:

```xml
<settings>
  <mirrors>
    <mirror>
      <id>central</id>
      <mirrorOf>*</mirrorOf>
      <url>https://repo.maven.apache.org/maven2</url>
    </mirror>
  </mirrors>
</settings>
```

### Development Setup with IDE

**IntelliJ IDEA**:
1. Open project folder
2. File → Open
3. Select `pom.xml`
4. Click "Open"
5. IDEA auto-configures Maven
6. Run Main.java

**Eclipse**:
1. File → Import
2. Maven → Existing Maven Projects
3. Select project folder
4. Click Finish
5. Run → Run As → Java Application

**VS Code**:
1. Install Extension Pack for Java
2. Open project folder
3. VS Code auto-detects Maven
4. Run tests/application from command palette

---

## System Resources

Minimum requirements:
- **RAM**: 512MB for JVM
- **CPU**: 2 cores recommended
- **Disk**: 100MB for application + dependencies
- **Network**: 1Mbps for sync

Recommended:
- **RAM**: 2GB
- **CPU**: 4+ cores
- **Disk**: 1GB free space
- **Network**: 10Mbps+ for smooth operation

---

## Getting Help

If you encounter issues:

1. **Check Prerequisites**
   ```bash
   java -version
   mvn --version
   ```

2. **Review Logs**
   - Check console output for error messages
   - Look for stack traces

3. **Verify Network**
   ```bash
   # Check if ports are available
   # Windows
   netstat -ano | findstr 55555
   # Linux
   lsof -i :55555
   ```

4. **Consult Documentation**
   - README.md for detailed info
   - QUICKSTART.md for quick examples
   - TESTING.md for troubleshooting

5. **Rebuild from Scratch**
   ```bash
   mvn clean
   mvn package
   ```

---

## Next Steps

After successful installation:

1. **Read QUICKSTART.md** for quick start
2. **Review README.md** for full documentation
3. **Check TESTING.md** for test scenarios
4. **Run the application** and test features
5. **Explore the code** and customize

---

## Summary

| OS | Build Command | Run Command |
|----|---|---|
| Windows | `build.bat build` | `build.bat run` |
| Linux | `./build.sh build` | `./build.sh run` |
| macOS | `./build.sh build` | `./build.sh run` |

All systems:
```bash
mvn clean package
java -jar target/p2p-whiteboard.jar
```

---

**Installation Complete!** ✅

You're now ready to use the P2P Whiteboard application. Start drawing and collaborating!
