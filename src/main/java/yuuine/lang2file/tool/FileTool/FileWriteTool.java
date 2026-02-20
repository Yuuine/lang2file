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
public class FileWriteTool extends FileOperationTool {

    @Tool(name = "write_file", description = "将内容写入文件（覆盖或追加，默认UTF-8编码）")
    public String writeFile(
            @ToolParam(description = "文件完整路径") String filePath,
            @ToolParam(description = "要写入的内容") String content,
            @ToolParam(description = "是否追加（默认false，即覆盖）") Boolean append,
            @ToolParam(description = "文件编码（如UTF-8、GBK，默认UTF-8）") String encoding) {

        logCall("write_file", filePath, append, encoding);

        // 1. 参数校验
        if (filePath == null || filePath.trim().isEmpty()) {
            return "写入失败：文件路径不能为空";
        }
        if (content == null) {
            content = "";
        }

        // 2. 确定编码
        Charset charset = StandardCharsets.UTF_8;
        if (encoding != null && !encoding.trim().isEmpty()) {
            try {
                charset = Charset.forName(encoding.trim());
            } catch (IllegalArgumentException e) {
                return "写入失败：不支持的编码格式 '" + encoding + "'";
            }
        }

        boolean isAppend = (append != null && append);

        try {
            // 3. 路径规范化与安全校验
            Path path = normalizeAndSecure(filePath.trim());
            Path parent = path.getParent();

            // 4. 确保父目录存在
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
                log.debug("已创建父目录: {}", parent);
            }

            // 5. 检查父目录写入权限（如果父目录存在）
            if (parent != null && Files.exists(parent) && !Files.isWritable(parent)) {
                return "写入失败：目标目录没有写入权限";
            }

            // 6. 执行写入
            if (isAppend) {
                Files.writeString(path, content, charset,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.APPEND);
            } else {
                Files.writeString(path, content, charset,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING);
            }

            long size = Files.size(path);
            String action = isAppend ? "追加" : "写入";
            return String.format("文件%s成功！\n路径：%s\n大小：%d 字节", action, path, size);

        } catch (Exception e) {
            return handleException(e, "写入文件");
        }
    }
}