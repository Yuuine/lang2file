package yuuine.lang2file.tool.FileTool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;

@Service
@Slf4j
public class FileCreateTool {

    /**
     * 根据指定的目录、文件名和扩展名创建一个空白文件
     *
     * @param directory 目标目录
     * @param fileName  文件名（不包含扩展名）
     * @param extension 文件扩展名（例如：txt, pdf, doc，不需要加点）
     * @return 创建结果
     */
    @Tool(name = "create_file", description = "在指定目录创建空白文件")
    public String createFile(@ToolParam(description = "目标目录路径") String directory,
                             @ToolParam(description = "文件名（不含扩展名）") String fileName,
                             @ToolParam(description = "文件扩展名") String extension) {

        log.info("调用工具[create_file], 作用[创建文件], directory={}, fileName={}, extension={}",
                directory, fileName, extension);

        // 1. 参数校验
        if (directory == null || directory.trim().isEmpty()) {
            log.warn("文件创建失败：目录路径为空");
            return "创建失败：目录路径不能为空";
        }
        if (fileName == null || fileName.trim().isEmpty()) {
            log.warn("文件创建失败：文件名为空");
            return "创建失败：文件名不能为空";
        }
        if (extension == null || extension.trim().isEmpty()) {
            log.warn("文件创建失败：扩展名为空");
            return "创建失败：扩展名不能为空";
        }

        // 2. 清理参数（去除空格）
        directory = directory.trim();
        fileName = fileName.trim();
        extension = extension.trim().replaceAll("^\\.+", ""); // 移除开头的点

        log.debug("参数清理完成: directory={}, fileName={}, extension={}",
                directory, fileName, extension);

        try {
            // 3. 构建完整的文件路径
            Path dirPath = Paths.get(directory).normalize();
            String safeFileName = sanitizeFileName(fileName);
            String fullFileName = safeFileName + "." + extension;
            Path filePath = dirPath.resolve(fullFileName).normalize();

            log.debug("构建文件路径: dirPath={}, fullFileName={}, filePath={}",
                    dirPath, fullFileName, filePath);

            // 4. 安全检查：防止目录遍历攻击
            if (!filePath.startsWith(dirPath)) {
                log.warn("安全检查失败：文件路径超出指定目录范围: {}", filePath);
                return "创建失败：非法的文件路径";
            }

            // 5. 确保父目录存在
            log.debug("检查并创建父目录: {}", filePath.getParent());
            Files.createDirectories(filePath.getParent());

            // 6. 创建空白文件
            log.info("正在创建文件: {}", filePath);
            Files.createFile(filePath);

            String result = String.format("文件创建成功！\n路径：%s\n大小：0 字节", filePath);
            log.info("文件创建成功: {}", filePath);
            return result;

        } catch (FileAlreadyExistsException e) {
            log.warn("文件已存在: {}", e.getMessage());
            return "创建失败：文件已存在";
        } catch (AccessDeniedException e) {
            log.error("权限不足，无法创建文件: {}", e.getMessage());
            return "创建失败：没有写入权限";
        } catch (NoSuchFileException e) {
            log.error("父目录不存在且无法自动创建: {}", e.getMessage());
            return "创建失败：父目录不存在且无法自动创建";
        } catch (InvalidPathException e) {
            log.error("路径格式不正确: {}", e.getMessage());
            return "创建失败：路径格式不正确 - " + e.getMessage();
        } catch (IOException e) {
            log.error("文件创建过程中发生IO异常", e);
            return "创建失败：系统错误，请稍后重试";
        } catch (Exception e) {
            log.error("文件创建过程中发生未预期的异常", e);
            return "创建失败：未知错误，请稍后重试";
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
