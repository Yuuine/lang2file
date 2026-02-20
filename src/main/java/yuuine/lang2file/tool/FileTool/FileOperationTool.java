package yuuine.lang2file.tool.FileTool;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.*;

@Slf4j
public abstract class FileOperationTool {

    /**
     * 安全考虑，默认排除 Windows C盘操作
     */
    protected Path normalizeAndSecure(String pathStr) throws InvalidPathException {
        Path path = Paths.get(pathStr).normalize();
        
        // 检查是否在C盘根目录下
        if (path.isAbsolute() && path.getRoot().toString().toLowerCase().startsWith("c:")) {
            throw new SecurityException("禁止操作C盘根目录下的文件");
        }
        
        return path;
    }

    /**
     * 清理文件名中的非法字符
     */
    protected String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    /**
     * 统一异常处理，返回用户友好的错误信息
     */
    protected String handleException(Exception e, String operation) {
        switch (e) {
            case NoSuchFileException noSuchFileException -> {
                log.warn("{}失败：文件不存在", operation);
                return "操作失败：文件不存在";
            }
            case AccessDeniedException accessDeniedException -> {
                log.warn("{}失败：权限不足", operation);
                return "操作失败：权限不足";
            }
            case FileAlreadyExistsException fileAlreadyExistsException -> {
                log.warn("{}失败：文件已存在", operation);
                return "操作失败：文件已存在";
            }
            case DirectoryNotEmptyException directoryNotEmptyException -> {
                log.warn("{}失败：目录非空", operation);
                return "操作失败：目录非空";
            }
            case InvalidPathException invalidPathException -> {
                log.warn("{}失败：路径格式错误", operation);
                return "操作失败：路径格式错误";
            }
            case SecurityException securityException -> {
                log.warn("{}失败：安全限制", operation);
                return "操作失败：非法路径";
            }
            case IOException ioException -> {
                log.error("{}发生IO异常", operation, e);
                return "操作失败：系统IO错误，请稍后重试";
            }
            case null, default -> {
                log.error("{}发生未知异常", operation, e);
                return "操作失败：未知错误";
            }
        }
    }

    /**
     * 记录工具调用日志
     */
    protected void logCall(String toolName, Object... args) {
        log.info("调用工具[{}], 参数: {}", toolName, args);
    }
}