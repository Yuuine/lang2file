package yuuine.lang2file.tool.otherTool;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Spring AI 工具类，用于查询当前服务器的公网 IP 地址及相关地理位置和 ISP 信息。
 * 调用 freeipapi.com 的公共 API：<a href="https://free.freeipapi.com/api/json">...</a>
 * <p>
 * 作为独立的 Spring 服务，提供三个独立的 AI 可调用方法：
 * <ul>
 *   <li>get_current_ip_address - 返回 IP 地址</li>
 *   <li>get_ip_location - 返回地理位置信息（国家、地区、城市）</li>
 *   <li>get_ip_isp - 返回 ISP 和组织信息</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class IpAddressTool {

    private static final String API_URL = "https://free.freeipapi.com/api/json";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 内部记录类，映射 API 返回的 JSON 字段。
     */
    private record IpInfo(
            String ip,
            String country,
            String countryCode,
            String region,
            String regionName,
            String city,
            String zip,
            double lat,
            double lon,
            String timezone,
            String isp,
            String org,
            String as,
            String asname,
            boolean mobile,
            boolean proxy,
            boolean hosting,
            String query
    ) {
    }

    /**
     * 获取当前服务器的公网 IP 地址。
     *
     * @return 包含 IP 地址的友好消息，若获取失败则返回错误描述
     */
    @Tool(name = "get_current_ip_address", description = "获取当前服务器的公网IP地址")
    public String getCurrentIpAddress() {
        log.info("调用工具: get_current_ip_address");
        try {
            IpInfo info = fetchIpInfo();
            return String.format("当前公网IP地址是：%s", info.ip());
        } catch (Exception e) {
            log.error("获取IP地址时发生异常", e);
            return "获取IP地址失败：" + e.getMessage();
        }
    }

    /**
     * 获取当前服务器的地理位置信息。
     *
     * @return 包含国家、地区、城市的友好消息，若获取失败则返回错误描述
     */
    @Tool(name = "get_ip_location", description = "获取当前服务器的地理位置信息（国家、地区、城市）")
    public String getIpLocation() {
        log.info("调用工具: get_ip_location");
        try {
            IpInfo info = fetchIpInfo();
            return String.format("地理位置：%s，%s，%s", info.country(), info.regionName(), info.city());
        } catch (Exception e) {
            log.error("获取地理位置时发生异常", e);
            return "获取地理位置失败：" + e.getMessage();
        }
    }

    /**
     * 获取当前服务器的 ISP 和组织信息。
     *
     * @return 包含 ISP 和组织的友好消息，若获取失败则返回错误描述
     */
    @Tool(name = "get_ip_isp", description = "获取当前服务器的ISP（互联网服务提供商）和组织信息")
    public String getIpIsp() {
        log.info("调用工具: get_ip_isp");
        try {
            IpInfo info = fetchIpInfo();
            return String.format("ISP：%s，组织：%s", info.isp(), info.org());
        } catch (Exception e) {
            log.error("获取ISP信息时发生异常", e);
            return "获取ISP信息失败：" + e.getMessage();
        }
    }

    /**
     * 调用 API 获取 IP 详细信息，并解析为 IpInfo 对象。
     *
     * @return IpInfo 对象
     * @throws Exception 任何网络、IO 或解析异常
     */
    private IpInfo fetchIpInfo() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .timeout(TIMEOUT)
                .header("User-Agent", "IpAddressTool/1.0")
                .GET()
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("API 返回非成功状态码: %d", response.statusCode()));
        }

        return OBJECT_MAPPER.readValue(response.body(), IpInfo.class);
    }
}