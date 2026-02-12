package yuuine.tool.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import yuuine.tool.agent.AgentService;
import yuuine.tool.cli.util.ConsoleRepl;

@Component
public class CliRunner implements CommandLineRunner {

    private final AgentService agentService;

    public CliRunner(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public void run(String... args) {
        // 启动 REPL 循环（阻塞主线程）
        new ConsoleRepl(agentService).start();

        // 应用将在 REPL 退出后自然结束
    }
}