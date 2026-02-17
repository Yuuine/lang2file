package yuuine.lang2file.cli.command;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;
import reactor.core.scheduler.Schedulers;
import yuuine.lang2file.agent.AgentService;

import java.util.concurrent.CountDownLatch;

@ShellComponent
@RequiredArgsConstructor
@Slf4j
public class ChatCommand {

    private final AgentService agentService;

    @ShellMethod(key = {"chat", ":", "：", "`", "c"},
            value = "与AI助手对话",
            group = "system command")
    public void chat(
            @ShellOption(help = "对话内容")
            @NotNull String message) {

        newChat(message);

    }

    /**
     * 处理用户与AI助手的对话交互
     * 
     * @param message 用户输入的对话内容
     * <p>
     * 该方法通过以下步骤处理用户输入：
     * 1. 创建CountDownLatch用于同步等待异步处理完成
     * 2. 使用agentService.processStream()处理用户输入的消息流
     * 3. 在doOnNext回调中实时输出AI响应内容到控制台
     * 4. 在doOnComplete回调中处理对话完成后的清理工作
     * 5. 在doOnError回调中处理异常情况
     * 6. 使用latch.await()阻塞主线程直到异步处理完成
     * <p>
     * 异常处理包括：
     * - InterruptedException：线程被中断的情况
     * - 其他Exception：处理过程中的其他异常
     */
    private void newChat(String message) {
        CountDownLatch latch = new CountDownLatch(1);
        StringBuilder fullResponse = new StringBuilder();

        try {
            log.debug("用户输入: {}", message);

            agentService.chatStream(message)
                    .subscribeOn(Schedulers.boundedElastic())
                    .doOnNext(chunk -> {
                        System.out.print(chunk);
                        System.out.flush();
                        fullResponse.append(chunk);
                    })
                    .doOnComplete(() -> {
                        System.out.println("\n");
                        log.debug("对话处理完成，总字符数: {}", fullResponse.length());
                        latch.countDown();
                    })
                    .doOnError(error -> {
                        System.err.println("\n处理失败: " + error.getMessage());
                        log.error("处理输入失败", error);
                        latch.countDown();
                    })
                    .subscribe();
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("\n对话被中断");
            log.error("对话被中断", e);
        } catch (Exception e) {
            System.err.println("\n对话处理失败: " + e.getMessage());
            log.error("对话处理失败", e);
        }
    }
}