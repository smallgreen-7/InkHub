package com.example.InkHub_backend.ai;

import com.example.InkHub_backend.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Markdown 分块器：按标题层级切成语义完整的块
 * 标题路径随块记录；代码块整体保留；超长按断句符切 + 重叠
 */
@Component
@RequiredArgsConstructor
public class MarkdownChunker {

    private final AiProperties props;

    public record Chunk(String heading, String content) {
    }

    public List<Chunk> chunk(String markdown) {
        List<Chunk> result = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return result;
        }
        List<String> headingStack = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inCode = false;

        for (String rawLine : markdown.split("\n")) {
            String line = rawLine.strip();
            if (inCode) {
                cur.append(line).append('\n');
                if (line.startsWith("```") || line.startsWith("~~~")) {
                    flush(result, cur, headingStack);
                    cur.setLength(0);
                    inCode = false;
                }
                continue;
            }
            if (line.startsWith("```") || line.startsWith("~~~")) {
                flush(result, cur, headingStack);
                cur.setLength(0);
                cur.append(line).append('\n');
                inCode = true;
                continue;
            }
            if (line.matches("^#{1,6}\\s+.*")) {   // 标题：1~6 个 # + 空格
                flush(result, cur, headingStack);
                cur.setLength(0);
                int level = 0;
                while (level < line.length() && line.charAt(level) == '#') level++;
                String text = cleanLine(line.substring(level));
                while (headingStack.size() >= level) {
                    headingStack.remove(headingStack.size() - 1);
                }
                headingStack.add(text);
                continue;
            }
            String cleaned = cleanLine(line);
            if (!cleaned.isBlank()) {
                cur.append(cleaned).append('\n');
            }
        }
        flush(result, cur, headingStack);
        return result;
    }

    private void flush(List<Chunk> result, StringBuilder cur, List<String> headingStack) {
        String text = cur.toString().strip();
        if (text.isEmpty()) return;
        String heading = String.join(" / ", headingStack);
        for (String part : splitLong(text)) {
            result.add(new Chunk(heading, part));
        }
    }

    /** 超长块按断句符切 + 重叠 */
    private List<String> splitLong(String text) {
        int size = props.getChunkSize();
        int overlap = props.getChunkOverlap();
        if (text.length() <= size) {
            return List.of(text);
        }
        List<String> parts = new ArrayList<>();
        String rest = text;
        while (rest.length() > size) {
            int cut = findCut(rest, size);
            parts.add(rest.substring(0, cut).strip());
            rest = rest.substring(Math.max(0, cut - overlap));
        }
        if (!rest.isBlank()) parts.add(rest.strip());
        return parts;
    }

    private int findCut(String s, int limit) {
        int from = Math.max(0, (int) (limit * 0.6));
        for (int i = limit; i > from && i < s.length(); i--) {
            char c = s.charAt(i);
            if (c == '。' || c == '！' || c == '？' || c == '；' || c == '，' || c == ',' || c == '.' || c == '\n') {
                return i + 1;
            }
        }
        return Math.min(limit, s.length());
    }

    /** 行内 Markdown 符号清洗 */
    private String cleanLine(String line) {
        String s = line.strip();
        while (s.startsWith(">")) s = s.substring(1).strip();
        s = s.replaceFirst("^([-*+]|\\d+[.、)])\\s+", "");
        s = s.replaceAll("!\\[[^\\]]*\\]\\([^)]*\\)", "");
        s = s.replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1");
        if (s.matches("^\\|?[\\s:|-]+\\|?$")) return "";
        s = s.replace('|', ' ');
        s = s.replaceAll("`([^`]*)`", "$1");
        s = s.replaceAll("\\*\\*([^*]+)\\*\\*", "$1");
        s = s.replaceAll("__([^_]+)__", "$1");
        s = s.replaceAll("\\*([^*]+)\\*", "$1");
        s = s.replaceAll("_([^_]+)_", "$1");
        if (s.matches("^[-*_]{3,}$")) return "";
        s = s.replaceAll("<[^>]+>", "");
        return s.strip();
    }
}
