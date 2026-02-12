package yuuine.lang2file.cli.util;

import yuuine.lang2file.agent.AgentService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConsoleRepl {
    private final AgentService agentService;
    private final BufferedReader reader;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public ConsoleRepl(AgentService agentService) {
        this.agentService = agentService;
        this.reader = new BufferedReader(new InputStreamReader(System.in));
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public void start() {
        System.out.println("📁 AI 文件助手启动 | 输入 'exit' 或 'quit' 退出");
        System.out.println("💡 提示：输入 '/help' 查看命令帮助");

        while (running.get()) {
            try {
                System.out.print("> ");
                String input = reader.readLine();

                if (input == null) {
                    System.out.println("\n收到 EOF，正在退出...");
                    break;
                }

                input = input.trim();
                if (input.isEmpty()) continue;

                // 命令处理
                if (handleCommand(input)) {
                    continue;
                }

                // AI 处理
                String reply = agentService.process(input);
                System.out.println("🤖 " + reply);

            } catch (IOException e) {
                if (running.get()) {
                    System.err.println("⚠️ 读取输入时发生错误: " + e.getMessage());
                }
                break;
            } catch (Exception e) {
                System.err.println("❌ 执行指令时出错: " + e.getMessage());
            }
        }
        shutdown();
    }

    private boolean handleCommand(String input) {
        switch (input.toLowerCase()) {
            case "/help", "help", "?","-h":
                showHelp();
                return true;
            case "/clear":
                System.out.print("\033[H\033[2J");
                System.out.flush();
                return true;
            case "/history":
                showHistory();
                return true;
            case "exit":
            case "quit":
                running.set(false);
                return true;
            default:
                return false;
        }
    }

    private void showHelp() {
        System.out.println("""
            📋 命令帮助:
            /help    - 显示此帮助信息
            /clear   - 清屏
            /history - 查看历史记录
            exit     - 退出程序
            """);
    }

    private void showHistory() {
        System.out.println("📋 历史记录功能待实现");
    }

    private void shutdown() {
        if (running.compareAndSet(true, false)) {
            System.out.println("\n👋 再见！");
            try {
                reader.close();
            } catch (IOException ignored) {}
        }
    }
}
