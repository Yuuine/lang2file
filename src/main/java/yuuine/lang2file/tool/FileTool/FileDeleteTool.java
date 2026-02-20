package yuuine.lang2file.tool.FileTool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Service
@Slf4j
public class FileDeleteTool {

    /**
     * 根据指定的路径删除文件
     *
     * @param filePath 要删除的文件完整路径
     * @return 删除结果
     */
    @Tool(name = "delete_file", description = "删除指定路径的文件")
    public String deleteFile(@ToolParam(description = "要删除的文件完整路径") String filePath) {

        log.info("调用工具[delete_file], 作用[删除文件], filePath={}", filePath);

        // 1. 参数校验
        if (filePath == null || filePath.trim().isEmpty()) {
            log.warn("文件删除失败：文件路径为空");
            return "删除失败：文件路径不能为空";
        }

        // 2. 清理参数
        filePath = filePath.trim();
        log.debug("参数清理完成: filePath={}", filePath);

        try {
            // 3. 构建路径对象
            Path path = Paths.get(filePath).normalize();
            log.debug("规范化后的路径: {}", path);

            // 4. 检查文件是否存在
            if (!Files.exists(path)) {
                log.warn("文件不存在: {}", path);
                return String.format("删除失败：文件 '%s' 不存在", path);
            }

            // 5. 检查是否为目录
            if (Files.isDirectory(path)) {
                log.warn("指定路径是目录，不是文件: {}", path);
                return String.format("删除失败：'%s' 是目录，请使用目录删除工具", path);
            }

            // 6. 检查是否有写入权限（删除需要父目录的写入权限）
            Path parent = path.getParent();
            if (parent != null && !Files.isWritable(parent)) {
                log.warn("父目录没有写入权限: {}", parent);
                return "删除失败：没有权限删除该文件";
            }

            // 7. 执行删除操作
            log.info("正在删除文件: {}", path);
            Files.delete(path);

            String result = String.format("文件删除成功！\n路径：%s", path);
            log.info("文件删除成功: {}", path);
            return result;

        } catch (NoSuchFileException e) {
            log.warn("文件不存在: {}", e.getMessage());
            return "删除失败：文件不存在";
        } catch (AccessDeniedException e) {
            log.error("权限不足，无法删除文件: {}", e.getMessage());
            return "删除失败：没有删除权限";
        } catch (DirectoryNotEmptyException e) {
            log.warn("目录非空，无法删除: {}", e.getMessage());
            return "删除失败：目录非空（这不应该发生，因为已检查是文件）";
        } catch (InvalidPathException e) {
            log.error("路径格式不正确: {}", e.getMessage());
            return "删除失败：路径格式不正确 - " + e.getMessage();
        } catch (IOException e) {
            log.error("文件删除过程中发生IO异常", e);
            return "删除失败：系统错误，请稍后重试";
        } catch (Exception e) {
            log.error("文件删除过程中发生未预期的异常", e);
            return "删除失败：未知错误，请稍后重试";
        }
    }

    /**
     * 根据目录和文件名删除文件（便捷方法）
     *
     * @param directory 文件所在目录
     * @param fileName  文件名（包含扩展名）
     * @return 删除结果
     */
    @Tool(name = "delete_file_by_parts", description = "根据目录和文件名删除文件")
    public String deleteFileByParts(
            @ToolParam(description = "文件所在目录") String directory,
            @ToolParam(description = "文件名（包含扩展名）") String fileName) {

        log.info("调用工具[delete_file_by_parts], 作用[删除文件], directory={}, fileName={}",
                directory, fileName);

        // 1. 参数校验
        if (directory == null || directory.trim().isEmpty()) {
            log.warn("文件删除失败：目录路径为空");
            return "删除失败：目录路径不能为空";
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("文件删除失败：文件名为空");
            return "删除失败：文件名不能为空";
        }

        // 2. 清理参数
        directory = directory.trim();
        fileName = fileName.trim();
        log.debug("参数清理完成: directory={}, fileName={}", directory, fileName);

        try {
            // 3. 构建完整的文件路径
            Path dirPath = Paths.get(directory).normalize();
            String safeFileName = sanitizeFileName(fileName);
            Path filePath = dirPath.resolve(safeFileName).normalize();

            log.debug("构建文件路径: dirPath={}, fileName={}, filePath={}",
                    dirPath, fileName, filePath);

            // 4. 安全检查：防止目录遍历攻击
            if (!filePath.startsWith(dirPath)) {
                log.warn("安全检查失败：文件路径超出指定目录范围: {}", filePath);
                return "删除失败：非法的文件路径";
            }

            // 5. 调用主方法执行删除
            return deleteFile(filePath.toString());

        } catch (InvalidPathException e) {
            log.error("路径格式不正确: {}", e.getMessage());
            return "删除失败：路径格式不正确 - " + e.getMessage();
        }
    }

    /**
     * 清理文件名中的非法字符
     */
    private String sanitizeFileName(String fileName) {
        log.debug("清理文件名前: {}", fileName);
        // Windows/Linux 通用的非法字符
        String sanitized = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        log.debug("清理文件名后: {}", sanitized);
        return sanitized;
    }
}