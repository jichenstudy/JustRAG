package com.shujichen.rag.builtin.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shujichen.rag.builtin.BuiltinTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OperatingSystem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务器系统监控工具
 * <p>
 * 获取 CPU、内存、磁盘、网络等实时运行状态
 */
@Slf4j
@Component
public class SystemMonitorTool implements BuiltinTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final SystemInfo systemInfo = new SystemInfo();

    @Override
    public String getName() {
        return "system_monitor";
    }

    @Override
    public String getDescription() {
        return "获取服务器实时运行状态，包括CPU使用率、内存使用情况、磁盘占用、网络状态等。参数type可选值：cpu/memory/disk/network/all，默认all";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "type": {
                      "type": "string",
                      "enum": ["cpu", "memory", "disk", "network", "all"],
                      "description": "监控类型，默认all"
                    }
                  }
                }
                """;
    }

    @Override
    public String execute(String input) {
        try {
            String type = "all";
            if (input != null && !input.isBlank()) {
                Map<String, Object> params = MAPPER.readValue(input, Map.class);
                if (params.containsKey("type")) {
                    type = params.get("type").toString().toLowerCase();
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            HardwareAbstractionLayer hal = systemInfo.getHardware();

            if ("all".equals(type) || "cpu".equals(type)) {
                result.put("cpu", getCpuInfo(hal.getProcessor()));
            }
            if ("all".equals(type) || "memory".equals(type)) {
                result.put("memory", getMemoryInfo(hal.getMemory()));
            }
            if ("all".equals(type) || "disk".equals(type)) {
                result.put("disk", getDiskInfo(hal.getDiskStores()));
            }
            if ("all".equals(type) || "network".equals(type)) {
                result.put("network", getNetworkInfo(hal.getNetworkIFs()));
            }

            // 添加系统信息
            OperatingSystem os = systemInfo.getOperatingSystem();
            result.put("system", Map.of(
                    "hostname", os.getNetworkParams().getHostName(),
                    "os", os.toString(),
                    "uptime", formatUptime(os.getSystemUptime())
            ));

            return MAPPER.writeValueAsString(result);
        } catch (Exception e) {
            log.error("系统监控工具执行失败", e);
            return "{\"error\": \"系统监控失败: " + e.getMessage() + "\"}";
        }
    }

    private Map<String, Object> getCpuInfo(CentralProcessor processor) {
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        // 等待一小段时间获取更准确的CPU使用率
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {
        }
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;

        return Map.of(
                "usage", Math.round(cpuLoad * 10.0) / 10.0,
                "cores", processor.getLogicalProcessorCount(),
                "model", processor.getProcessorIdentifier().getName().trim()
        );
    }

    private Map<String, Object> getMemoryInfo(GlobalMemory memory) {
        long total = memory.getTotal();
        long available = memory.getAvailable();
        long used = total - available;
        double usagePercent = (double) used / total * 100;

        return Map.of(
                "total", formatBytes(total),
                "used", formatBytes(used),
                "available", formatBytes(available),
                "usage", Math.round(usagePercent * 10.0) / 10.0
        );
    }

    private List<Map<String, Object>> getDiskInfo(List<HWDiskStore> diskStores) {
        List<Map<String, Object>> disks = new ArrayList<>();
        for (HWDiskStore disk : diskStores) {
            long total = disk.getSize();
            long used = 0;
            for (var partition : disk.getPartitions()) {
                used += partition.getSize();
            }
            if (total > 0) {
                disks.add(Map.of(
                        "name", disk.getName().trim(),
                        "model", disk.getModel().trim(),
                        "total", formatBytes(total),
                        "reads", disk.getReads(),
                        "writes", disk.getWrites()
                ));
            }
        }
        return disks;
    }

    private Map<String, Object> getNetworkInfo(List<NetworkIF> networkIFs) {
        List<Map<String, Object>> interfaces = new ArrayList<>();
        long totalRx = 0, totalTx = 0;

        for (NetworkIF net : networkIFs) {
            if (net.getIPv4addr().length > 0) {
                long rx = net.getBytesRecv();
                long tx = net.getBytesSent();
                totalRx += rx;
                totalTx += tx;
                interfaces.add(Map.of(
                        "name", net.getDisplayName(),
                        "ip", String.join(",", net.getIPv4addr()),
                        "mac", net.getMacaddr(),
                        "rxBytes", formatBytes(rx),
                        "txBytes", formatBytes(tx),
                        "speed", net.getSpeed() / 1000000 + " Mbps"
                ));
            }
        }

        return Map.of(
                "interfaces", interfaces,
                "totalRx", formatBytes(totalRx),
                "totalTx", formatBytes(totalTx)
        );
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        if (bytes < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", bytes / (1024.0 * 1024));
        }
        if (bytes < 1024L * 1024 * 1024 * 1024) {
            return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
        }
        return String.format("%.1f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
    }

    private String formatUptime(long seconds) {
        long days = seconds / 86400;
        long hours = (seconds % 86400) / 3600;
        long minutes = (seconds % 3600) / 60;
        if (days > 0) {
            return days + "天 " + hours + "小时 " + minutes + "分钟";
        }
        if (hours > 0) {
            return hours + "小时 " + minutes + "分钟";
        }
        return minutes + "分钟";
    }
}
