package yuuine.lang2file.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.*;
import yuuine.lang2file.agent.AgentService;

/**
 * Agent 聊天接口
 */
@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService agentService;

    @PostMapping("/chat")
    public ChatResponse chat(@RequestBody String userInput) {
        return agentService.chat(userInput);
    }

    @PostMapping("/chatText")
    public String chatText(@RequestBody String userInput) {
        return agentService.chat(userInput).getResult().getOutput().getText();
    }

}