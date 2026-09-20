package com.example.InkHub_backend.service.Impl;

import com.example.InkHub_backend.common.BusinessException;
import com.example.InkHub_backend.config.AiProperties;
import com.example.InkHub_backend.dto.ChatRequest;
import com.example.InkHub_backend.dto.SummarizeRequest;
import com.example.InkHub_backend.entity.Article;
import com.example.InkHub_backend.mapper.ArticleMapper;
import com.example.InkHub_backend.service.AiChatService;
import com.example.InkHub_backend.service.AiSearchService;
import com.example.InkHub_backend.service.AiSearchService.Hit;
import com.example.InkHub_backend.service.HybridSearchService;
import com.example.InkHub_backend.service.RerankService;
import com.example.InkHub_backend.utils.RedisKeys;
import com.example.InkHub_backend.vo.ChatSourceVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * RAG 问答编排
 * 链路：限流 → (文章页上下文：拼当前文章) → 向量检索 → 组装提示词（编号资料+历史）
 *      → ChatClient.stream() 流式 → SseEmitter 逐字转发 → 引用来源
 * 防幻觉：只依据资料 + 编号 [n] + 低分拒答
 */
@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final ChatClient chatClient;
    private final AiSearchService aiSearchService;
    private final HybridSearchService hybridSearchService;
    private final RerankService rerankService;
    private final ArticleMapper articleMapper;
    private final AiProperties props;
    private final RedisTemplate<String, Object> redisTemplate;

    public AiChatServiceImpl(
            @Qualifier("chatClient") ChatClient chatClient,
            AiSearchService aiSearchService,
            HybridSearchService hybridSearchService,
            RerankService rerankService,
            ArticleMapper articleMapper,
            AiProperties props,
            RedisTemplate<String, Object> redisTemplate) {
        this.chatClient = chatClient;
        this.aiSearchService = aiSearchService;
        this.hybridSearchService = hybridSearchService;
        this.rerankService = rerankService;
        this.articleMapper = articleMapper;
        this.props = props;
        this.redisTemplate = redisTemplate;
    }

    /** ⚠️ 项目 Boot 4 的 webmvc starter 不提供 ObjectMapper Bean，自建 static 单例（线程安全） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    @Async   // SSE 必须由异步线程驱动（Controller 返回 emitter 后容器才进异步模式）
    public void chat(Long userId, ChatRequest req, SseEmitter emitter) {
        try {
            checkRate(userId);
            String question = req.getQuestion().strip();
            if (question.isEmpty()) throw new BusinessException("问题不能为空");

            // 1. 混合检索（BM25 + 向量 → RRF 融合）→ Rerank 精排
            List<Hit> rawHits = hybridSearchService.hybridSearch(
                    question, props.getTopK() * 3, props.getMaxChunkPerArticle(), props.getMinScore());
            List<Hit> hits = rerankService.rerank(question, rawHits, props.getTopK());
            // 文章页上下文：把当前文章内容拼进资料（"和文章对话"体验）
            StringBuilder articleContext = new StringBuilder();
            if ("article".equals(req.getContextType()) && req.getArticleId() != null) {
                Article article = articleMapper.selectById(req.getArticleId());
                if (article != null && article.getStatus() != null && article.getStatus() == 1) {
                    articleContext.append("用户正在阅读站内文章《").append(article.getTitle())
                            .append("》，若问题与该文章相关，优先依据下面的文章内容回答：\n")
                            .append(truncate(article.getContentMd(), 4000)).append("\n\n");
                }
            }

            // 2. 组装提示词：有资料走 RAG 严格模式；无资料走宽松模式（自然回应但不编造站内内容）
            boolean hasMaterial = !hits.isEmpty() || articleContext.length() > 0;
            String userPrompt = hasMaterial
                    ? buildPrompt(question, hits, articleContext.toString(), req.getHistory())
                    : buildFreePrompt(question, req.getHistory());

            // 4. 流式回答：Flux → SseEmitter 桥接
            Flux<String> flux = chatClient.prompt()
                    .user(userPrompt)
                    .stream()
                    .content();
            flux.doOnNext(delta -> {
                        sendQuietly(emitter, Map.of("type", "delta", "text", delta));
                        try { Thread.sleep(30); } catch (InterruptedException ignored) {} // 30ms/字
                    })
                    .doOnComplete(() -> {
                        sendQuietly(emitter, Map.of("type", "sources", "sources", toSources(hits)));
                        sendQuietly(emitter, Map.of("type", "done"));
                        emitter.complete();
                    })
                    .doOnError(e -> {
                        log.error("AI 流式问答异常 userId={}", userId, e);
                        sendQuietly(emitter, Map.of("type", "error", "msg", "AI 服务开小差了，请稍后再试"));
                        emitter.complete();
                    })
                    .subscribe();   // 异步订阅，不阻塞
        } catch (BusinessException e) {
            sendQuietly(emitter, Map.of("type", "error", "msg", e.getMessage()));
            emitter.complete();
        } catch (Exception e) {
            log.error("AI 问答异常 userId={}", userId, e);
            sendQuietly(emitter, Map.of("type", "error", "msg", "AI 服务开小差了，请稍后再试"));
            emitter.complete();
        }
    }

    @Override
    public String summarize(SummarizeRequest req) {
        String summary = chatClient.prompt()
                .system("你是资深内容编辑。根据文章标题和正文生成不超过 80 字的中文摘要，"
                        + "只概括正文真实内容，不编造，不要用「本文」「文章」开头。")
                .user("标题：" + req.getTitle() + "\n\n正文：\n" + truncate(req.getContentMd(), 3000))
                .call()
                .content();
        if (summary == null || summary.isBlank()) {
            throw new BusinessException("AI 生成摘要失败，请重试");
        }
        return summary.strip();
    }

    // ========== 私有方法 ==========

    /** 每用户每分钟限流：Redis INCR + 60s 过期 */
    private void checkRate(Long userId) {
        if (userId == null) return;
        String key = RedisKeys.aiChatRate(userId, LocalDateTime.now().format(MINUTE_FMT));
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, Duration.ofSeconds(60));
        }
        if (count != null && count > props.getRateLimit()) {
            throw new BusinessException(429, "提问太频繁啦，歇一分钟再问吧");
        }
    }

    private String buildPrompt(String question, List<Hit> hits, String articleContext,
                               List<ChatRequest.HistoryItem> history) {
        StringBuilder sb = new StringBuilder();
        // 多轮对话历史（最近 6 轮，白名单过滤 + 截断）
        if (history != null && !history.isEmpty()) {
            sb.append("【对话历史】\n");
            int from = Math.max(0, history.size() - 6);
            for (int i = from; i < history.size(); i++) {
                ChatRequest.HistoryItem item = history.get(i);
                if (item == null || item.getContent() == null || item.getContent().isBlank()) {
                    continue;
                }
                String role = "assistant".equals(item.getRole()) ? "墨墨" : "用户";
                sb.append(role).append("：").append(truncate(item.getContent().strip(), 500)).append("\n");
            }
            sb.append("\n");
        }
        sb.append("【站内参考资料】\n");
        if (!articleContext.isEmpty()) {
            sb.append("[1] 《当前阅读文章全文》\n").append(truncate(articleContext, 4000)).append("\n\n");
        }
        int idx = articleContext.isEmpty() ? 1 : 2;
        for (Hit hit : hits) {
            sb.append('[').append(idx++).append("] 《").append(hit.articleTitle()).append('》');
            if (hit.heading() != null && !hit.heading().isBlank()) {
                sb.append("（章节：").append(hit.heading()).append('）');
            }
            sb.append('\n').append(truncate(hit.content(), 600)).append("\n\n");
        }
        sb.append("回答规则：只依据以上资料回答，资料没有的内容明确说站内没有，不要编造；"
                + "引用资料内容时在句末标注编号如 [1]；用中文简洁回答。\n\n");
        sb.append("请回答：").append(question);
        return sb.toString();
    }

    /** 无检索命中时的宽松模式：允许寒暄/自我介绍/通用闲聊，但绝不编造站内内容 */
    private String buildFreePrompt(String question, List<ChatRequest.HistoryItem> history) {
        StringBuilder sb = new StringBuilder();
        if (history != null && !history.isEmpty()) {
            sb.append("【对话历史】\n");
            int from = Math.max(0, history.size() - 6);
            for (int i = from; i < history.size(); i++) {
                ChatRequest.HistoryItem item = history.get(i);
                if (item == null || item.getContent() == null || item.getContent().isBlank()) continue;
                String role = "assistant".equals(item.getRole()) ? "墨墨" : "用户";
                sb.append(role).append("：").append(truncate(item.getContent().strip(), 500)).append("\n");
            }
            sb.append("\n");
        }
        sb.append("站内检索结果为空，没有检索到与用户问题相关的站内文章资料。请这样回应：\n")
                .append("1. 如果是寒暄、打招呼、问你是谁/能做什么 → 自然介绍自己：你是墨墨，InkHub 知识社区的 AI 助手，能基于站内已发布的文章回答问题并标注引用出处；\n")
                .append("2. 如果用户问的是站内文章或社区内容 → 如实告知站内暂时没有相关内容，建议换个说法或去首页看看最新文章；\n")
                .append("3. 如果是日常闲聊或通用知识 → 可以简短正常回应，但不要提及任何站内具体文章；\n")
                .append("4. 红线：绝对不要编造站内存在某篇文章或某段内容。\n\n")
                .append("用户说：").append(question);
        return sb.toString();
    }

    /** 命中块按文章聚合成引用来源 */
    private List<ChatSourceVO> toSources(List<Hit> hits) {
        Map<Long, ChatSourceVO> map = new java.util.LinkedHashMap<>();
        for (Hit hit : hits) {
            ChatSourceVO vo = map.get(hit.articleId());
            if (vo == null) {
                vo = new ChatSourceVO();
                vo.setArticleId(hit.articleId());
                vo.setTitle(hit.articleTitle());
                vo.setScore(hit.score());
                map.put(hit.articleId(), vo);
            } else if (hit.score() > vo.getScore()) {
                vo.setScore(hit.score());
            }
        }
        return new ArrayList<>(map.values());
    }

    private void send(SseEmitter emitter, Map<String, Object> payload) throws IOException {
        emitter.send(SseEmitter.event().data(OBJECT_MAPPER.writeValueAsString(payload), MediaType.APPLICATION_JSON));
    }

    private void sendQuietly(SseEmitter emitter, Map<String, Object> payload) {
        try {
            send(emitter, payload);
        } catch (IOException ignored) {
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}
