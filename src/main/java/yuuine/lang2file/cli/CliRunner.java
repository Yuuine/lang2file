package yuuine.lang2file.cli;

import lombok.extern.slf4j.Slf4j;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CliRunner implements PromptProvider {

    @Override
    public AttributedString getPrompt() {
        // 如果在 CI 环境中，则不启动 CLI
        if (isCiEnvironment()) {
            return null;
        }
        // 否则启动 CLI 并返回提示符
        return new AttributedString("lang2file> ",
                AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
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