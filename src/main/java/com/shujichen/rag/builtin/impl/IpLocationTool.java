package com.shujichen.rag.builtin.impl;

import cn.hutool.http.HttpRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shujichen.rag.builtin.BuiltinTool;
import com.shujichen.rag.common.util.AddressUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;

/**
 * IP归属地查询工具
 * <p>
 * 查询指定IP或本机公网IP的归属地信息（国家、省份、城市、运营商）
 * 使用 ip2region 离线数据库，无需联网即可查询归属地
 */
@Slf4j
@Component
public class IpLocationTool implements BuiltinTool {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * 公网IP查询服务列表
     */
    private static final List<String> PUBLIC_IP_APIS = List.of(
            "https://ifconfig.me/ip",          // 国际通用，返回纯文本IP
            "https://myip.ipip.net",           // 国内，返回 "IP：x.x.x.x 地区"
            "https://ip.3322.net"              // 国内，返回纯文本IP
    );

    @Override
    public String getName() {
        return "ip_location";
    }

    @Override
    public String getDescription() {
        return "查询IP地址的归属地信息（国家、省份、城市、运营商）。传入 localhost 或 127.0.0.1 可查询本机IP归属地";
    }

    @Override
    public String getInputSchema() {
        return """
                {
                  "type": "object",
                  "properties": {
                    "ip": {
                      "type": "string",
                      "description": "要查询的IP地址，输入 localhost 或 127.0.0.1 可查询本机IP"
                    }
                  }
                }
                """;
    }

    @Override
    public String execute(String input) {
        try {
            String ip = null;
            if (input != null && !input.isBlank()) {
                Map<String, Object> params = MAPPER.readValue(input, Map.class);
                ip = params.getOrDefault("ip", "").toString().trim();
            }

            // localhost/127.0.0.1/本机 触发查询本机IP
            if (ip == null || ip.isBlank() || ip.equalsIgnoreCase("localhost")
                    || ip.equals("127.0.0.1") || ip.equals("本机") || ip.equals("本地")) {
                ip = getPublicIp();
            }

            // 使用 ip2region 离线查询归属地
            String location = AddressUtils.getRealAddressByIP(ip);
            log.info("IP查询: {} -> {}", ip, location);

            return MAPPER.writeValueAsString(Map.of(
                    "success", true,
                    "ip", ip,
                    "location", location
            ));
        } catch (Exception e) {
            log.error("IP查询失败", e);
            return error("IP查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取本机公网IP（多服务备用 + 本地网卡兜底）
     */
    private String getPublicIp() {
        // 依次尝试各个公网IP服务
        for (String apiUrl : PUBLIC_IP_APIS) {
            try {
                String body = HttpRequest.get(apiUrl)
                        .timeout(3000)
                        .execute()
                        .body()
                        .trim();

                if (body.isBlank()) {
                    continue;
                }

                // myip.ipip.net 返回格式 "IP：x.x.x.x 中国 北京"，提取IP
                if (body.contains("：") || body.contains(":")) {
                    String ip = body.replaceAll(".*?[:：]\\s*([\\d.]+).*", "$1");
                    if (ip.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                        return ip;
                    }
                }

                // 纯文本IP
                if (body.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    return body;
                }

                // 提取IP
                String extracted = body.replaceAll(".*?(\\d+\\.\\d+\\.\\d+\\.\\d+).*", "$1");
                if (extracted.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                    return extracted;
                }
            } catch (Exception e) {
                log.debug("公网IP服务 {} 不可用: {}", apiUrl, e.getMessage());
            }
        }

        // 全部失败，回退到本地网卡IP
        String localIp = getLocalIp();
        log.warn("所有公网IP服务不可用，使用本地IP: {}", localIp);
        return localIp;
    }

    /**
     * 获取本机局域网IP（非127.x.x.x的第一个IPv4地址）
     */
    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = ni.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        String ip = addr.getHostAddress();
                        if (!ip.startsWith("127.")) {
                            return ip;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取本地IP失败", e);
        }
        return "127.0.0.1";
    }

    private String error(String msg) {
        try {
            return MAPPER.writeValueAsString(Map.of("success", false, "error", msg));
        } catch (Exception ex) {
            return "{\"success\":false,\"error\":\"" + msg + "\"}";
        }
    }
}
