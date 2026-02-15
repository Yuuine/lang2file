package yuuine.lang2file.cli.command;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import yuuine.lang2file.agent.AgentService;

@ShellComponent
@RequiredArgsConstructor
@Slf4j
public class ChatCommand {

    private final AgentService agentService;

    @ShellMethod(key = {"chat", ":", "：", "c"},
            value = "与AI助手对话",
            group = "system command")
    public String chat(@ShellOption(help = "对话内容") String message) {
        try {
            log.debug("用户输入: {}", message);
            String response = agentService.process(message);
            log.debug("AI回复: {}", response);
            return response;
        } catch (Exception e) {
            log.error("对话处理失败", e);
            return "抱歉，处理您的请求时出现错误: " + e.getMessage();
        }
    }
}
