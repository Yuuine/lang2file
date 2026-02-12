package yuuine.lang2file.cli;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import yuuine.lang2file.agent.AgentService;
import yuuine.lang2file.cli.util.ConsoleRepl;

@Component
public class CliRunner implements CommandLineRunner {

    private final AgentService agentService;

    public CliRunner(AgentService agentService) {
        this.agentService = agentService;
    }

    @Override
    public void run(String... args) {

        if (isCiEnvironment()) {
            System.out.println("检测到 CI 环境，跳过启动交互式控制台");
            return;
        }

        new ConsoleRepl(agentService).start();
    }

    /**
     * 检测是否在 CI/CD 环境中
     */
    private boolean isCiEnvironment() {
        String[] ciEnvVars = {
                "CI",
                "GITHUB_ACTIONS",
                "GITLAB_CI",
                "CIRCLECI",
                "JENKINS_HOME",
                "TEAMCITY_VERSION"
        };
        for (String var : ciEnvVars) {
            if (System.getenv(var) != null) {
                return true;
            }
        }
        return false;
    }
}