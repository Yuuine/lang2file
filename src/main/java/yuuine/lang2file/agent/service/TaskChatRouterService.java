package yuuine.lang2file.agent.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 任务型对话分类服务，专门判断用户输入是否与文件操作相关。
 * <p>
 * 采用混合策略：先通过规则快速过滤，对于规则无法明确判定的模糊输入，
 * 调用外部LLM服务进行智能判断。
 * 规则部分覆盖中英文文件操作典型表达，并包含负向规则排除明显非任务的问候/闲聊。
 *
 * @author (your name)
 * @version 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskChatRouterService {

    // ========== 正向规则：文件操作相关（直接返回 true） ==========

    /**
     * 英文文件操作动词 + 文件名词组合的正则（不区分大小写）
     */
    private static final Pattern POSITIVE_EN_PATTERN = Pattern.compile(
            "(?i).*\\b(create|write|delete|remove|move|rename|copy|open|edit|save|upload|download|compress|extract)\\b.*\\b(file|folder|directory|document|txt|pdf|doc|xls|ppt|zip)\\b.*"
    );

    /**
     * 中文文件操作动词 + 文件名词组合的正则
     */
    private static final Pattern POSITIVE_CN_PATTERN = Pattern.compile(
            ".*(创建|写入|删除|移除|移动|重命名|复制|打开|编辑|保存|上传|下载|压缩|解压|新建|修改).*(文件|文件夹|目录|文档|文本|图片|照片|视频|音乐|压缩包).*"
    );

    /**
     * 简洁指令的正则，如“删文件”、“新建文件夹”
     */
    private static final Pattern POSITIVE_SHORT_PATTERN = Pattern.compile(
            ".*(删|新建|创建|复制|粘贴|移动|重命名)(文件|文件夹|目录).*"
    );

    // ========== 负向规则：明显非文件操作任务（直接返回 false） ==========

    /**
     * 问候语集合（不区分大小写比较时需转为小写）
     */
    private static final Set<String> GREETINGS = new HashSet<>(Arrays.asList(
            "你好", "您好", "在吗", "在不在", "嗨", "hello", "hi", "hey",
            "good morning", "good afternoon", "good evening"
    ));

    /**
     * 闲聊/非文件任务关键词集合（如果句子包含这些词且不包含文件名词，则判定为false）
     */
    private static final Set<String> NON_FILE_TASK_KEYWORDS = new HashSet<>(Arrays.asList(
            "天气", "股票", "新闻", "音乐", "电影", "闹钟", "计时器", "打电话", "发短信",
            "weather", "stock", "news", "music", "movie", "alarm", "timer", "call", "text"
    ));

    // ========== 文件相关名词集合（用于模糊触发） ==========
    private static final Set<String> FILE_NOUNS = new HashSet<>(Arrays.asList(
            "文件", "文件夹", "目录", "文档", "文本", "图片", "照片", "视频", "音乐", "压缩包",
            "file", "folder", "directory", "document", "txt", "pdf", "doc", "xls", "ppt", "zip",
            "image", "photo", "video", "music"
    ));

    /**
     * LLM 分类提示词：要求模型判断输入是否为文件操作任务，仅输出 "true" 或 "false"
     */
    private static final String FILE_OPERATION_CLASSIFIER_PROMPT = """
            You are a file-operation task classifier. Determine if the user's input is a request to perform a file or directory operation (e.g., create, read, write, delete, move, rename, copy, open, edit, save, upload, download, compress, extract). If it is, respond with exactly "true". If the input is casual chat, greeting, or any non-task statement, respond with exactly "false". Do not include any other text, punctuation, or explanation.
            """;

    // 依赖注入
    private final ChatClientFactory clientFactory;

    /**
     * 判断用户输入是否为文件操作相关的任务型对话。
     *
     * @param userInput 用户输入的原始字符串
     * @return true 表示文件操作任务，false 表示非任务或无法确定
     */
    public boolean isTask(final String userInput) {
        if (!StringUtils.hasText(userInput)) {
            log.debug("输入为空，判定为非任务");
            return false;
        }

        final String trimmed = userInput.trim();
        log.debug("开始判断输入: [{}]", trimmed);

        // 1. 负向规则快速排除
        if (isGreeting(trimmed)) {
            log.debug("命中问候语规则，判定为非任务");
            return false;
        }

        // 2. 正向规则快速命中
        if (matchesPositivePattern(trimmed)) {
            log.debug("命中正向文件操作规则，判定为任务");
            return true;
        }

        // 3. 检查是否包含文件相关名词，决定是否需要LLM兜底
        final boolean containsFileNoun = containsAnyFileNoun(trimmed);
        if (!containsFileNoun) {
            // 不包含文件名词，且未命中正向规则，说明不是文件操作任务
            // 但需检查是否包含其他非文件任务关键词（如天气），如果包含，明确返回false
            if (containsNonFileTaskKeyword(trimmed)) {
                log.debug("包含其他非文件任务关键词，判定为非文件任务");
                return false;
            }
            log.debug("不包含文件相关名词，也未命中正向规则，判定为非任务");
            return false;
        }

        // 4. 模糊情况：包含文件名词但未被规则明确覆盖，调用LLM
        log.debug("输入包含文件相关名词但规则无法明确，调用LLM进行判断");
        return callLlmWithFallback(trimmed);
    }

    /**
     * 检查输入是否为问候语（忽略大小写）。
     *
     * @param input 待检查字符串
     * @return 如果匹配任意问候语返回 true
     */
    private boolean isGreeting(final String input) {
        final String lower = input.toLowerCase();
        return containsAnyIgnoreCase(lower, GREETINGS);
    }

    /**
     * 匹配正向规则。
     *
     * @param input 待匹配字符串
     * @return 如果命中任意正向规则返回 true
     */
    private boolean matchesPositivePattern(final String input) {
        // 英文模式
        if (POSITIVE_EN_PATTERN.matcher(input).matches()) {
            return true;
        }
        // 中文模式
        if (POSITIVE_CN_PATTERN.matcher(input).matches()) {
            return true;
        }
        // 简短指令模式
        return POSITIVE_SHORT_PATTERN.matcher(input).matches();
    }

    /**
     * 检查输入是否包含文件相关名词（忽略大小写）。
     *
     * @param input 待检查字符串
     * @return 如果包含任意文件名词返回 true
     */
    private boolean containsAnyFileNoun(final String input) {
        final String lower = input.toLowerCase();
        return containsAnyIgnoreCase(lower, FILE_NOUNS);
    }

    /**
     * 检查输入是否包含非文件任务关键词（忽略大小写）。
     *
     * @param input 待检查字符串
     * @return 如果包含任意非文件任务关键词返回 true
     */
    private boolean containsNonFileTaskKeyword(final String input) {
        final String lower = input.toLowerCase();
        return containsAnyIgnoreCase(lower, NON_FILE_TASK_KEYWORDS);
    }

    /**
     * 通用方法：检查小写字符串是否包含集合中的任意关键词（已小写化）。
     *
     * @param lowerCaseText 已转换为小写的文本
     * @param keywords      关键词集合（原始大小写）
     * @return 如果包含任意关键词返回 true
     */
    private boolean containsAnyIgnoreCase(final String lowerCaseText, final Set<String> keywords) {
        for (final String keyword : keywords) {
            // 将关键词也转为小写进行比较
            if (lowerCaseText.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 调用LLM服务，并处理可能的异常，异常时返回false（保守策略）。
     *
     * @param input 用户输入
     * @return LLM判断结果，异常时返回false
     */
    private boolean callLlmWithFallback(final String input) {
        try {
            final ChatClient client = clientFactory.createClientWithoutMemoryAndTools();
            final String response = client.prompt()
                    .system(FILE_OPERATION_CLASSIFIER_PROMPT)
                    .user(input)
                    .call()
                    .content();
            final boolean result = response != null && response.contains("true");
            log.debug("LLM返回结果: {}", result);
            return result;
        } catch (Exception e) {
            log.error("调用LLM服务失败，降级返回false", e);
            return false;
        }
    }
}