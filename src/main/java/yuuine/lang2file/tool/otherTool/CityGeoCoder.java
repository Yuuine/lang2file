package yuuine.lang2file.tool.otherTool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * 城市名转经纬度坐标的工具类。
 * <p>
 * 基于 OpenStreetMap Nominatim API（<a href="https://nominatim.openstreetmap.org/">...</a>）实现。
 * 调用示例：https://nominatim.openstreetmap.org/search?city=Beijing&format=json&limit=1
 * </p>
 *
 * <p>
 * <strong>注意：</strong> Nominatim 服务有使用条款，要求提供有效的 User-Agent 并控制请求频率（建议每秒最多1次）。
 * 本工具默认设置了 User-Agent 和 5 秒超时，但请在生产环境中遵守官方政策。
 * </p>
 */
@Slf4j
public class CityGeoCoder {

    private static final String NOMINATIM_ENDPOINT = "https://nominatim.openstreetmap.org/search";
    private static final String USER_AGENT = "CityGeoCoder/1.0 (your-email@example.com)"; // 请替换为真实联系方式
    private static final Duration TIMEOUT = Duration.ofSeconds(5);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(TIMEOUT)
            .build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * 坐标记录类，包含纬度和经度。
     *
     * @param latitude  纬度
     * @param longitude 经度
     */
    public record Coordinate(double latitude, double longitude) {
        @Override
        public String toString() {
            return String.format("纬度: %.6f, 经度: %.6f", latitude, longitude);
        }
    }

    /**
     * Nominatim API 返回的单个地点信息。
     * 只映射需要的字段：lat 和 lon。
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record Place(
            String lat,
            String lon
    ) {
    }

    /**
     * 根据城市名查询经纬度坐标。
     *
     * @param cityName 城市名称（例如 "Beijing"）
     * @return 包含坐标的 {@link Optional} 对象，若未找到或发生错误则返回 {@link Optional#empty()}
     */
    public static Optional<Coordinate> getCoordinates(String cityName) {
        log.info("开始查询城市经纬度: {}", cityName);
        try {
            String encodedCity = URLEncoder.encode(cityName, StandardCharsets.UTF_8);
            String url = NOMINATIM_ENDPOINT + "?city=" + encodedCity + "&format=json&limit=1";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", USER_AGENT)
                    .GET()
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.warn("API 返回非成功状态码: {}, 城市: {}", response.statusCode(), cityName);
                return Optional.empty();
            }

            List<Place> places = OBJECT_MAPPER.readValue(
                    response.body(),
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, Place.class)
            );

            if (places.isEmpty()) {
                log.info("未找到城市: {}", cityName);
                return Optional.empty();
            }

            Place first = places.getFirst();
            double latitude = Double.parseDouble(first.lat());
            double longitude = Double.parseDouble(first.lon());

            Coordinate coord = new Coordinate(latitude, longitude);
            log.info("成功获取坐标: {} -> {}", cityName, coord);
            return Optional.of(coord);

        } catch (Exception e) {
            log.error("查询城市经纬度时发生异常, 城市: {}", cityName, e);
            return Optional.empty();
        }
    }

    /**
     * 根据城市名查询经纬度坐标，并返回格式化的友好字符串。
     * 适用于直接向用户展示的场景。
     *
     * @param cityName 城市名称
     * @return 包含坐标信息的字符串，若失败则返回错误描述
     */
    public static String getCoordinatesAsString(String cityName) {
        Optional<Coordinate> opt = getCoordinates(cityName);
        if (opt.isPresent()) {
            Coordinate coord = opt.get();
            return String.format("城市“%s”的经纬度坐标为：纬度 %.6f，经度 %.6f",
                    cityName, coord.latitude(), coord.longitude());
        } else {
            return String.format("无法获取城市“%s”的经纬度信息，请检查城市名称或稍后重试。", cityName);
        }
    }
}