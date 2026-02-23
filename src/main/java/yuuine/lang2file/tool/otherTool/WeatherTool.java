package yuuine.lang2file.tool.otherTool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Spring AI 工具类，用于查询 Open-Meteo 天气 API 获取指定经纬度的天气预报。
 * <p>
 * 提供三个 AI 可调用的方法：
 * <ul>
 *   <li>get_current_temperature - 获取当前实时温度</li>
 *   <li>get_hourly_forecast - 获取未来24小时逐小时温度预报</li>
 *   <li>get_weather_summary - 获取天气概要（位置、时区、海拔及温度范围）</li>
 * </ul>
 * </p>
 */
@Slf4j
@Service
public class WeatherTool {

    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(TIMEOUT).build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /**
     * 获取指定经纬度当前位置的实时温度（最接近当前UTC时间的小时温度）。
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return 包含当前温度的友好消息，若获取失败则返回错误描述
     */
    @Tool(name = "get_current_temperature", description = "获取指定经纬度当前位置的实时温度")
    public String getCurrentTemperature(double latitude, double longitude) {
        log.info("调用工具: get_current_temperature, 纬度={}, 经度={}", latitude, longitude);
        try {
            WeatherResponse data = fetchWeatherData(latitude, longitude);
            Double currentTemp = findCurrentTemperature(data);
            if (currentTemp == null) {
                return "无法获取当前温度数据。";
            }
            return String.format("当前位置（纬度 %.2f, 经度 %.2f）的实时温度约为 %.1f°C", data.latitude(), data.longitude(), currentTemp);
        } catch (Exception e) {
            log.error("获取当前温度时发生异常", e);
            return "获取当前温度失败：" + e.getMessage();
        }
    }

    /**
     * 获取指定经纬度未来24小时的逐小时温度预报。
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return 包含24小时温度列表的友好消息，若获取失败则返回错误描述
     */
    @Tool(name = "get_hourly_forecast", description = "获取指定经纬度未来24小时的逐小时温度预报")
    public String getHourlyForecast(double latitude, double longitude) {
        log.info("调用工具: get_hourly_forecast, 纬度={}, 经度={}", latitude, longitude);
        try {
            WeatherResponse data = fetchWeatherData(latitude, longitude);
            List<String> times = data.hourly().time();
            List<Double> temps = data.hourly().temperature2m();

            if (times.isEmpty() || temps.isEmpty()) {
                return "未获取到逐小时温度数据。";
            }

            // 只取前24小时（API默认返回多天，这里截取前24个点）
            int count = Math.min(24, times.size());
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("未来%d小时温度预报（UTC时间）：\n", count));
            for (int i = 0; i < count; i++) {
                String time = times.get(i).substring(11, 16); // 提取 HH:mm
                sb.append(String.format("  %s: %.1f°C\n", time, temps.get(i)));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取逐小时预报时发生异常", e);
            return "获取逐小时预报失败：" + e.getMessage();
        }
    }

    /**
     * 获取指定经纬度的天气概要信息，包括地理位置、时区、海拔以及未来24小时的温度范围。
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return 包含概要信息的友好消息，若获取失败则返回错误描述
     */
    @Tool(name = "get_weather_summary", description = "获取指定经纬度的天气概要信息（位置、时区、海拔及未来24小时温度范围）")
    public String getWeatherSummary(double latitude, double longitude) {
        log.info("调用工具: get_weather_summary, 纬度={}, 经度={}", latitude, longitude);
        try {
            WeatherResponse data = fetchWeatherData(latitude, longitude);
            List<Double> temps = data.hourly().temperature2m();

            if (temps.isEmpty()) {
                return "未获取到温度数据，无法生成概要。";
            }

            // 取前24小时计算最小/最大温度
            int count = Math.min(24, temps.size());
            double minTemp = temps.stream().limit(count).min(Double::compare).orElse(Double.NaN);
            double maxTemp = temps.stream().limit(count).max(Double::compare).orElse(Double.NaN);

            return String.format("位置：纬度 %.2f, 经度 %.2f\n时区：%s (%s)\n海拔：%.1f 米\n未来24小时温度范围：%.1f°C ～ %.1f°C", data.latitude(), data.longitude(), data.timezone(), data.timezoneAbbreviation(), data.elevation(), minTemp, maxTemp);
        } catch (Exception e) {
            log.error("获取天气概要时发生异常", e);
            return "获取天气概要失败：" + e.getMessage();
        }
    }

    /**
     * 调用 Open-Meteo API 获取天气数据，并解析为 WeatherResponse 对象。
     *
     * @param latitude  纬度
     * @param longitude 经度
     * @return WeatherResponse 对象
     * @throws Exception 任何网络、IO 或解析异常
     */
    private WeatherResponse fetchWeatherData(double latitude, double longitude) throws Exception {
        String url = BASE_URL + "?latitude=" + latitude + "&longitude=" + longitude + "&hourly=temperature_2m";
        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).timeout(TIMEOUT).header("User-Agent", "WeatherTool/1.0").GET().build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException(String.format("API 返回非成功状态码: %d", response.statusCode()));
        }

        return OBJECT_MAPPER.readValue(response.body(), WeatherResponse.class);
    }

    /**
     * 从天气数据中找出最接近当前UTC时间的小时温度。
     *
     * @param data 天气响应数据
     * @return 最接近当前时间的温度，若数据为空则返回 null
     */
    private Double findCurrentTemperature(WeatherResponse data) {
        List<String> times = data.hourly().time();
        List<Double> temps = data.hourly().temperature2m();
        if (times.isEmpty() || temps.isEmpty()) {
            return null;
        }

        Instant now = Instant.now();
        double minDiff = Double.MAX_VALUE;
        int bestIndex = 0;

        for (int i = 0; i < times.size(); i++) {
            LocalDateTime dateTime = LocalDateTime.parse(times.get(i), TIME_FORMATTER);
            Instant instant = dateTime.atZone(ZoneOffset.UTC).toInstant();
            long diffMillis = Math.abs(instant.toEpochMilli() - now.toEpochMilli());
            if (diffMillis < minDiff) {
                minDiff = diffMillis;
                bestIndex = i;
            }
        }

        return temps.get(bestIndex);
    }

    /**
     * Open-Meteo API 响应顶层结构。
     *
     * @param latitude             纬度
     * @param longitude            经度
     * @param generationtimeMs     生成时间（毫秒）
     * @param utcOffsetSeconds     UTC 偏移秒数
     * @param timezone             时区名称
     * @param timezoneAbbreviation 时区缩写
     * @param elevation            海拔（米）
     * @param hourlyUnits          小时数据单位
     * @param hourly               小时数据
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record WeatherResponse(double latitude, double longitude,
                                   @JsonProperty("generationtime_ms") double generationtimeMs,
                                   @JsonProperty("utc_offset_seconds") int utcOffsetSeconds, String timezone,
                                   @JsonProperty("timezone_abbreviation") String timezoneAbbreviation, double elevation,
                                   @JsonProperty("hourly_units") HourlyUnits hourlyUnits, HourlyData hourly) {
    }

    /**
     * 小时数据单位。
     *
     * @param time          时间字段单位
     * @param temperature2m 温度字段单位
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record HourlyUnits(String time, @JsonProperty("temperature_2m") String temperature2m) {
    }

    /**
     * 小时数据。
     *
     * @param time          ISO 8601 时间字符串列表
     * @param temperature2m 对应时间的温度列表（°C）
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record HourlyData(List<String> time, @JsonProperty("temperature_2m") List<Double> temperature2m) {
    }
}