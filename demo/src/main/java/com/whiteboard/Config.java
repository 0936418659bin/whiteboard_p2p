package com.whiteboard;

import java.io.*;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();
    private static final String CONFIG_FILE = "config.properties";

    static {
        loadConfig();
    }

    private static void loadConfig() {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input != null) {
                properties.load(input);
            } else {
                // Try to load from current directory
                try (FileInputStream fis = new FileInputStream(CONFIG_FILE)) {
                    properties.load(fis);
                } catch (FileNotFoundException e) {
                    System.out.println("Config file not found, using defaults");
                }
            }
        } catch (IOException e) {
            System.err.println("Error loading config: " + e.getMessage());
        }
    }

    // Network Configuration
    public static int getDiscoveryPort() {
        return getInt("discovery.port", 55555);
    }

    public static int getBroadcastPort() {
        return getInt("broadcast.port", 55556);
    }

    public static int getListenPortBase() {
        return getInt("listen.port.base", 55550);
    }

    public static int getListenPortRange() {
        return getInt("listen.port.range", 10);
    }

    public static int getConnectionTimeout() {
        return getInt("connection.timeout", 5000);
    }

    public static int getSocketTimeout() {
        return getInt("socket.timeout", 30000);
    }

    // Application Configuration
    public static int getMaxHistorySize() {
        return getInt("canvas.max_history_size", 1000);
    }

    public static int getDefaultCanvasWidth() {
        return getInt("canvas.default_width", 1200);
    }

    public static int getDefaultCanvasHeight() {
        return getInt("canvas.default_height", 800);
    }

    public static int getDefaultStrokeWidth() {
        return getInt("tool.stroke_default", 2);
    }

    public static int getMinStrokeWidth() {
        return getInt("tool.stroke_min", 1);
    }

    public static int getMaxStrokeWidth() {
        return getInt("tool.stroke_max", 50);
    }

    // Performance Settings
    public static int getThreadPoolSize() {
        return getInt("thread.pool_size", 10);
    }

    public static boolean isAntialiasingEnabled() {
        return getBoolean("render.antialiasing", true);
    }

    public static int getDiscoveryInterval() {
        return getInt("discovery.interval", 3000);
    }

    // Logging Configuration
    public static String getLogLevel() {
        return getString("log.level", "INFO");
    }

    public static boolean isConsoleLogging() {
        return getBoolean("log.console", true);
    }

    // Helper methods
    private static String getString(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private static int getInt(String key, int defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("Invalid integer value for " + key + ": " + value);
            }
        }
        return defaultValue;
    }

    private static boolean getBoolean(String key, boolean defaultValue) {
        String value = properties.getProperty(key);
        if (value != null) {
            return Boolean.parseBoolean(value);
        }
        return defaultValue;
    }

    public static void printConfig() {
        System.out.println("=== P2P Whiteboard Configuration ===");
        System.out.println("Discovery Port: " + getDiscoveryPort());
        System.out.println("Listen Port Base: " + getListenPortBase());
        System.out.println("Max History Size: " + getMaxHistorySize());
        System.out.println("Default Canvas Size: " + getDefaultCanvasWidth() + "x" + getDefaultCanvasHeight());
        System.out.println("Antialiasing: " + isAntialiasingEnabled());
    }
}
