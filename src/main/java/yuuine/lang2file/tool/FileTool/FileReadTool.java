package yuuine.lang2file.tool.FileTool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class FileReadTool extends FileOperationTool {

    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    @Tool(name = "read_file", description = "读取文本文件内容（默认UTF-8编码，最大支持10MB）")
    public String readFile(
            @ToolParam(description = "文件完整路径") String filePath,
            @ToolParam(description = "文件编码（如UTF-8、GBK，默认UTF-8）") String encoding) {

        logCall("read_file", filePath, encoding);

        // 1. 参数校验
        if (filePath == null || filePath.trim().isEmpty()) {
            return "读取失败：文件路径不能为空";
        }

        // 2. 确定编码
        Charset charset = StandardCharsets.UTF_8;
        if (encoding != null && !encoding.trim().isEmpty()) {
            try {
                charset = Charset.forName(encoding.trim());
            } catch (IllegalArgumentException e) {
                return "读取失败：不支持的编码格式 '" + encoding + "'";
            }
        }

        try {
            // 3. 路径规范化与安全校验
            Path path = normalizeAndSecure(filePath.trim());

            // 4. 检查文件是否存在
            if (!Files.exists(path)) {
                return "读取失败：文件不存在";
            }

            // 5. 检查是否为文件
            if (!Files.isRegularFile(path)) {
                return "读取失败：路径不是文件";
            }

            // 6. 检查可读权限
            if (!Files.isReadable(path)) {
                return "读取失败：没有读取权限";
            }

            // 7. 检查文件大小
            long size = Files.size(path);
            if (size > MAX_FILE_SIZE) {
                return String.format("读取失败：文件过大（%.2f MB > 10 MB）", size / (1024.0 * 1024.0));
            }

            // 8. 读取内容
            String content = Files.readString(path, charset);
            return String.format("文件读取成功（%d 字符）：\n%s", content.length(), content);

        } catch (Exception e) {
            return handleException(e, "读取文件");
        }
    }
}