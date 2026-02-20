package yuuine.lang2file.agent.dto;

import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class ToolRegistry {

    private final Map<String, ToolMeta> toolMap = new HashMap<>();

    public void register(ToolMeta meta) {
        toolMap.put(meta.getName(), meta);
    }

    public Collection<ToolMeta> getAll() {
        return toolMap.values();
    }

    public List<ToolMeta> getByNames(List<String> names) {
        return names.stream()
                .map(toolMap::get)
                .filter(Objects::nonNull)
                .toList();
    }
}