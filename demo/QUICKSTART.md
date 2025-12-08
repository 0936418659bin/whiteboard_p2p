# Quick Start Guide for P2P Whiteboard

## Installation & Setup

### Step 1: Prerequisites
- Install Java Development Kit (JDK) 17 or higher
  ```bash
  java -version  # Check your Java version
  ```
- Install Maven 3.6 or higher
  ```bash
  mvn --version  # Check Maven version
  ```

### Step 2: Clone/Setup Project
```bash
cd e:\P2P\whiteboard\demo
```

### Step 3: Build the Project
```bash
mvn clean package
```

This will create the executable JAR file in `target/` directory.

## Running the Application

### Option 1: From JAR (Recommended)
```bash
java -jar target/p2p-whiteboard.jar
```

### Option 2: Using Maven
```bash
mvn exec:java -Dexec.mainClass="com.whiteboard.ui.MainFrame"
```

## Testing Locally (Single Machine)

### Run Two Instances
Open two terminal windows:
```bash
# Terminal 1
java -jar target/p2p-whiteboard.jar

# Terminal 2  
java -jar target/p2p-whiteboard.jar
```

### Connect the Instances
1. In Instance 2, click "🔌 Connect Peer"
2. Select the peer from Instance 1
3. Click "Connect"
4. Start drawing in either window - changes appear in both

## Testing on Network (Multiple Machines)

### Setup
1. Ensure all machines are on the same network (WiFi or Ethernet)
2. Run `java -jar target/p2p-whiteboard.jar` on each machine
3. Peers automatically discover each other

### Connect Machines
1. Click "🔌 Connect Peer" 
2. Select a peer (shows as "Peer-[ID] (IP_ADDRESS)")
3. Click "Connect"
4. Start drawing!

## Controls

| Control | Action |
|---------|--------|
| Pencil Tool | Freehand drawing |
| Line Tool | Draw straight lines |
| Rectangle/Circle | Draw shapes |
| Color Button | Pick color |
| Width Slider | Adjust stroke width |
| ↶ Undo Button | Undo last action |
| 🗑️ Clear Button | Clear entire canvas |

## Network Configuration

### Default Ports
- Discovery: 55555 (UDP broadcast sender), 55556 (UDP receiver)
- P2P Connections: 55550-55559 (TCP)

### Firewall Requirements
Allow these ports in your firewall:
- UDP: 55555, 55556
- TCP: 55550-55559

## Troubleshooting

### Issue: "Port already in use"
**Solution**: Close other instances or change port in code

### Issue: Peers not discovered
**Solution**: 
1. Check both machines are on same network
2. Disable VPN/Proxy temporarily
3. Check firewall allows UDP/TCP ports

### Issue: Drawing not syncing
**Solution**:
1. Verify connection status shows "Connected: N"
2. Try disconnecting and reconnecting
3. Check console for error messages

## Performance Tips

1. **Clear canvas** when it gets too cluttered
2. **Reduce stroke width** for better performance
3. **Limit concurrent peers** to 5-10 for best experience
4. **Use solid shapes** instead of complex free-drawing

## Development

### Adding New Tools
Edit `DrawingTool.java` and `Shape.java`:
```java
public enum Tool {
    PENCIL("Pencil"),
    MY_NEW_TOOL("My Tool")  // Add here
}
```

### Modifying Network Protocol
Edit `NetworkProtocol.java` to add new message types:
```java
public enum MessageType {
    HELLO,
    MY_MESSAGE_TYPE  // Add here
}
```

### Building Custom UI
Use `MainFrame.java` as template - uses standard Swing components.

## Advanced Configuration

### Custom Ports
In `PeerDiscovery.java`:
```java
private static final int DISCOVERY_PORT = 55555;  // Change here
private static final int BROADCAST_PORT = 55556;  // And here
```

### Listen Port Range
In `MainFrame.java`:
```java
listenPort = 55550 + (int)(Math.random() * 10);  // Modify range
```

### History Size
In `DrawingCanvas.java`:
```java
new DrawingHistory(1000);  // Change max history size
```

## Command Line Options

Run with custom settings:
```bash
# Standard run
java -jar target/p2p-whiteboard.jar

# With more heap memory
java -Xmx1g -jar target/p2p-whiteboard.jar

# With logging
java -Djava.util.logging.level=FINE -jar target/p2p-whiteboard.jar
```

## File Structure

```
demo/
├── pom.xml                  # Maven configuration
├── README.md               # Main documentation
├── QUICKSTART.md           # This file
├── src/
│   ├── main/java/com/whiteboard/
│   │   ├── network/        # P2P networking
│   │   ├── drawing/        # Drawing engine
│   │   ├── ui/             # User interface
│   │   ├── sync/           # Synchronization
│   │   └── Main.java       # Entry point
│   └── test/               # Test files
└── target/
    └── p2p-whiteboard.jar  # Compiled application
```

## Environment Variables

No special environment variables needed. The application is self-contained.

## System Requirements

| Component | Minimum | Recommended |
|-----------|---------|-------------|
| Java | 17 | 17+ |
| RAM | 512MB | 2GB |
| Disk | 50MB | 100MB |
| Network | Local Network | Gigabit |

## Getting Help

1. **Check console output** for error messages
2. **Review logs** when starting the application
3. **Verify network connectivity** with `ping` command
4. **Restart the application** if issues persist

## Next Steps

1. ✅ Build and run the application
2. ✅ Test with multiple instances
3. ✅ Test on your network
4. ✅ Customize colors and tools
5. ✅ Explore the code and modify as needed

Happy drawing! 🎨
