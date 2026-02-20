package yuuine.lang2file.tool.FileTool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class DirectoryListTool extends FileOperationTool {

    @Tool(name = "list_directory", description = "列出指定目录下的文件和子目录")
    public String listDirectory(
            @ToolParam(description = "目录路径") String dirPath,
            @ToolParam(description = "是否递归列出所有内容（默认false）") Boolean recursive) {

        logCall("list_directory", dirPath, recursive);

        if (dirPath == null || dirPath.trim().isEmpty()) {
            return "列出失败：目录路径不能为空";
        }

        boolean isRecursive = (recursive != null && recursive);

        try {
            Path path = normalizeAndSecure(dirPath.trim());

            if (!Files.exists(path)) {
                return "列出失败：目录不存在";
            }

            if (!Files.isDirectory(path)) {
                return "列出失败：路径不是目录";
            }

            if (!Files.isReadable(path)) {
                return "列出失败：没有读取权限";
            }

            StringBuilder result = new StringBuilder();
            if (isRecursive) {
                // 递归列出所有文件和目录
                try (Stream<Path> walk = Files.walk(path)) {
                    String list = walk
                            .map(p -> path.relativize(p).toString())
                            .filter(p -> !p.isEmpty()) // 排除根目录自身
                            .map(p -> Files.isDirectory(path.resolve(p)) ? "[DIR] " + p : "[FILE] " + p)
                            .collect(Collectors.joining("\n"));
                    result.append("目录 ").append(path).append(" 的内容（递归）：\n");
                    result.append(list.isEmpty() ? "（空目录）" : list);
                }
            } else {
                // 仅列出直接子项
                try (Stream<Path> list = Files.list(path)) {
                    String listStr = list
                            .map(p -> Files.isDirectory(p) ? "[DIR] " + p.getFileName() : "[FILE] " + p.getFileName())
                            .collect(Collectors.joining("\n"));
                    result.append("目录 ").append(path).append(" 的内容：\n");
                    result.append(listStr.isEmpty() ? "（空目录）" : listStr);
                }
            }

            return result.toString();

        } catch (Exception e) {
            return handleException(e, "列出目录");
        }
    }
}
