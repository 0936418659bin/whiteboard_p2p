# Testing Guide for P2P Whiteboard

## Test Scenarios

### Scenario 1: Basic Functionality Test (Single Instance)

**Objective**: Verify basic drawing tools work correctly

**Steps**:
1. Launch application: `java -jar target/p2p-whiteboard.jar`
2. Test each drawing tool:
   - Pencil: Draw freehand
   - Line: Draw a straight line
   - Rectangle: Draw a rectangle
   - Circle: Draw a circle
   - Ellipse: Draw an ellipse
3. Test color selection:
   - Click color button
   - Select different colors
   - Verify drawing color changes
4. Test stroke width:
   - Adjust width slider (1-50)
   - Verify stroke changes accordingly
5. Test undo: Click "↶ Undo" button
   - Last drawn shape should disappear
6. Test clear: Click "🗑️ Clear" button
   - Entire canvas should be blank

**Expected Result**: ✅ All drawing tools work, colors update, undo/clear functions work

---

### Scenario 2: Local Network Test (Two Instances)

**Objective**: Verify P2P connection and synchronization on same machine

**Steps**:
1. Open Terminal 1:
   ```bash
   java -jar target/p2p-whiteboard.jar
   ```
   Note: Peer ID and port shown in status bar

2. Open Terminal 2:
   ```bash
   java -jar target/p2p-whiteboard.jar
   ```
   Note: Different Peer ID and port

3. In Instance 2, click "🔌 Connect Peer"
4. Select peer from Instance 1
5. Click "Connect"
6. Verify "Connected: 1" appears in Instance 2 status bar
7. Draw shape in Instance 1
8. Verify shape appears in Instance 2
9. Draw shape in Instance 2
10. Verify shape appears in Instance 1

**Expected Result**: ✅ Peers discover each other, connect successfully, and shapes sync in both directions

---

### Scenario 3: Multi-Peer Test (3+ Instances)

**Objective**: Test synchronization with multiple peers

**Steps**:
1. Launch 3 instances of the application
2. From Instance 2, connect to Instance 1
3. From Instance 3, connect to Instance 1
4. Status bar should show:
   - Instance 1: "Connected: 2"
   - Instance 2: "Connected: 1"
   - Instance 3: "Connected: 1"
5. Draw in Instance 1
6. Verify shape appears in Instance 2 and 3
7. Draw in Instance 2
8. Verify shape appears in Instance 1 and 3 (but NOT in 2)
9. Draw in Instance 3
10. Verify shape appears in Instance 1 and 2

**Expected Result**: ✅ Multi-peer synchronization works correctly with star topology

---

### Scenario 4: Network Test (Multiple Machines)

**Objective**: Verify P2P connection across network

**Prerequisites**:
- 2 machines on same local network
- Java installed on both

**Steps**:
1. On Machine A:
   ```bash
   java -jar target/p2p-whiteboard.jar
   ```
   Note the Peer ID and IP address

2. On Machine B:
   ```bash
   java -jar target/p2p-whiteboard.jar
   ```

3. On Machine B, click "🔌 Connect Peer"
4. Verify you can see Machine A's peer in the list
   (Shows as "Peer-[ID] (IP_ADDRESS)")
5. Click "Connect"
6. Wait for connection (may take 2-3 seconds)
7. Status bar should show "Connected: 1"
8. Draw on Machine A
9. Verify drawing appears on Machine B
10. Draw on Machine B
11. Verify drawing appears on Machine A

**Expected Result**: ✅ Cross-machine synchronization works

---

### Scenario 5: Undo/Clear Synchronization

**Objective**: Verify undo and clear operations sync across peers

**Steps**:
1. Connect two instances (Scenario 2)
2. Draw shape in Instance 1
3. Verify shape in Instance 2
4. Click "↶ Undo" in Instance 1
5. Verify shape disappears in both instances
6. Draw multiple shapes in Instance 2
7. Draw shape in Instance 1
8. Click "🗑️ Clear" in Instance 1
9. Verify ALL shapes disappear in both instances

**Expected Result**: ✅ Undo and clear operations synchronize across all peers

---

### Scenario 6: Stress Test (High Drawing Load)

**Objective**: Test performance with many shapes

**Steps**:
1. Connect two instances
2. Use pencil tool to draw rapidly
3. Create 100+ shapes
4. Draw complex patterns
5. Monitor performance
6. Check CPU and memory usage
7. Verify shapes still sync correctly
8. Test undo with many shapes
9. Test clear with many shapes

**Expected Result**: ✅ Application remains responsive (no UI freezing)

---

### Scenario 7: Connection Stability Test

**Objective**: Test handling of connection interruptions

**Steps**:
1. Connect two instances
2. Draw shape in Instance 1
3. Minimize Instance 2 window
4. Draw more shapes in Instance 1
5. Restore Instance 2 window
6. Verify all shapes appear in Instance 2
7. Terminate Instance 2 process (kill window)
8. Instance 1 should show "Connected: 0"
9. Draw shapes in Instance 1
10. Restart Instance 2
11. Reconnect
12. Verify Instance 2 gets current state from Instance 1

**Expected Result**: ✅ Application handles disconnections gracefully

---

### Scenario 8: Concurrent Drawing Test

**Objective**: Test drawing simultaneously on different peers

**Setup**: Connect two instances (Scenario 2)

**Steps**:
1. Have two people/terminals ready
2. Both draw at the same time
3. Instance A: Draw circle with red color
4. Instance B: Draw rectangle with blue color (simultaneously)
5. Verify both shapes appear on both instances
6. Change colors
7. Both draw different shapes simultaneously
8. Verify all shapes appear with correct colors

**Expected Result**: ✅ Concurrent drawings don't conflict, all appear correctly

---

## Performance Metrics

### Baseline Metrics (Single Instance)
| Metric | Expected | Acceptable |
|--------|----------|-----------|
| Memory Usage | ~100MB | <300MB |
| CPU Usage (idle) | <5% | <10% |
| CPU Usage (drawing) | 20-30% | <50% |
| FPS while drawing | 30+ | >20 |
| Startup Time | <2 sec | <5 sec |

### Network Metrics (Two Instances)
| Metric | Expected | Acceptable |
|--------|----------|-----------|
| Discovery Time | <5 sec | <10 sec |
| Connection Time | <2 sec | <5 sec |
| Sync Latency | <100ms | <500ms |
| Network Usage (per draw) | ~10KB | <50KB |

---

## Test Cases

### Test Case 1: Tool Switching
```
1. Select Pencil tool
2. Draw shape A
3. Select Line tool
4. Draw shape B
5. Verify shape A is pencil, shape B is line
Expected: PASS
```

### Test Case 2: Color Persistence
```
1. Set color to Red
2. Draw shape A
3. Set color to Blue
4. Draw shape B
5. Verify shape A is red, shape B is blue
Expected: PASS
```

### Test Case 3: Width Persistence
```
1. Set width to 5
2. Draw shape A
3. Set width to 20
4. Draw shape B
5. Verify width difference is visible
Expected: PASS
```

### Test Case 4: Peer Discovery
```
1. Launch Instance A (note peer ID)
2. Launch Instance B (note peer ID)
3. Click "Connect Peer" in B
4. Verify A appears in list
Expected: PASS (both show different IDs)
```

### Test Case 5: Shape Serialization
```
1. Draw all shape types in Instance A
2. Connect Instance B
3. Verify all shapes appear in Instance B
Expected: PASS (all shapes synchronized)
```

---

## Debugging Tests

### Enable Verbose Logging
```bash
java -Djava.util.logging.level=FINE -jar target/p2p-whiteboard.jar
```

### Monitor Network Activity
```bash
# Windows
netstat -an | findstr 55555

# Linux/Mac
lsof -i :55555
```

### Check Port Availability
```bash
# Windows
netstat -ano | findstr :55550

# Linux/Mac
lsof -i :55550
```

---

## Regression Test Checklist

Before each release, verify:

- [ ] All drawing tools work
- [ ] Color selection works
- [ ] Stroke width adjustment works
- [ ] Undo functionality works
- [ ] Clear canvas works
- [ ] Single instance launches without errors
- [ ] Peer discovery works
- [ ] Connection to peer works
- [ ] Drawing synchronizes
- [ ] Undo/Clear synchronize
- [ ] Multiple peers can draw simultaneously
- [ ] Disconnections handled gracefully
- [ ] No memory leaks with extended use
- [ ] Performance acceptable under load
- [ ] Works on Windows
- [ ] Works on Linux
- [ ] Works on macOS

---

## Known Issues & Limitations

1. **Broadcast Limited to Local Network**
   - Peers must be on same network
   - Won't work across internet without VPN

2. **No Persistence**
   - Drawings lost when application closes
   - No save/load functionality

3. **No Authentication**
   - Any peer can connect
   - No access control

4. **Drawing Order**
   - Depends on network message arrival
   - High latency may cause reordering

5. **Scalability**
   - Best with 5-10 concurrent peers
   - May degrade with 20+ peers

---

## Test Results Template

```
Test Date: ____________________
Tester: ______________________
Build Version: ________________
OS: ___________________________

Scenario 1: Basic Functionality
Result: [ ] PASS [ ] FAIL [ ] PARTIAL
Notes: _________________________

Scenario 2: Local Network
Result: [ ] PASS [ ] FAIL [ ] PARTIAL
Notes: _________________________

[Continue for other scenarios...]

Overall Result: [ ] PASS [ ] FAIL

Issues Found:
1. ___________________________
2. ___________________________
```

---

## Continuous Testing

### Automated Smoke Tests
Run these commands regularly:
```bash
# Build
mvn clean package

# Run briefly
timeout 5 java -jar target/p2p-whiteboard.jar || true
```

### Performance Monitoring
Monitor over 30 minutes:
```bash
java -Xms256m -Xmx1g -jar target/p2p-whiteboard.jar
```

Watch for:
- Memory leak (increasing memory)
- CPU spikes
- Connection drops
- UI unresponsiveness

---

## Conclusion

Complete all test scenarios before considering the application production-ready. Document any failures and create issues for fixes.
