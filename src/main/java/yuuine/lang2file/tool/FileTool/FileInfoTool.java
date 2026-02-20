package yuuine.lang2file.tool.FileTool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.text.SimpleDateFormat;
import java.util.Date;

@Service
@Slf4j
public class FileInfoTool extends FileOperationTool {

    @Tool(name = "file_info", description = "获取文件或目录的详细信息")
    public String getFileInfo(
            @ToolParam(description = "文件或目录路径") String pathStr) {

        logCall("file_info", pathStr);

        if (pathStr == null || pathStr.trim().isEmpty()) {
            return "获取失败：路径不能为空";
        }

        try {
            Path path = normalizeAndSecure(pathStr.trim());

            if (!Files.exists(path)) {
                return "获取失败：文件或目录不存在";
            }

            // 收集基本信息
            StringBuilder info = new StringBuilder();
            info.append("路径：").append(path.toAbsolutePath()).append("\n");
            info.append("类型：").append(Files.isDirectory(path) ? "目录" : "文件").append("\n");

            // 大小
            if (Files.isRegularFile(path)) {
                long size = Files.size(path);
                info.append("大小：").append(size).append(" 字节");
                if (size > 1024) info.append(String.format(" (%.2f KB)", size / 1024.0));
                if (size > 1024 * 1024) info.append(String.format(" (%.2f MB)", size / (1024.0 * 1024.0)));
                info.append("\n");
            }

            // 最后修改时间
            FileTime lastModified = Files.getLastModifiedTime(path);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            info.append("最后修改：").append(sdf.format(new Date(lastModified.toMillis()))).append("\n");

            // 权限
            info.append("权限：")
                    .append(Files.isReadable(path) ? "r" : "-")
                    .append(Files.isWritable(path) ? "w" : "-")
                    .append(Files.isExecutable(path) ? "x" : "-")
                    .append("\n");

            // 所有者（如果有）
            try {
                UserPrincipal owner = Files.getOwner(path);
                info.append("所有者：").append(owner.getName()).append("\n");
            } catch (UnsupportedOperationException e) {
                // 文件系统不支持，忽略
            }

            // 如果是符号链接，显示目标
            if (Files.isSymbolicLink(path)) {
                Path target = Files.readSymbolicLink(path);
                info.append("符号链接指向：").append(target).append("\n");
            }

            return info.toString().trim();

        } catch (Exception e) {
            return handleException(e, "获取文件信息");
        }
    }
}