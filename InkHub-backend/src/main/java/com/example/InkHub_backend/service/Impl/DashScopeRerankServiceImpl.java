package com.example.InkHub_backend.service.Impl;

import com.example.InkHub_backend.config.AiProperties;
import com.example.InkHub_backend.service.AiSearchService.Hit;
import com.example.InkHub_backend.service.RerankService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashScopeRerankServiceImpl implements RerankService {

    private final AiProperties props;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** 连接超时：百炼不可达时必须快速失败，否则请求线程会被一直占住 */
    private static final int CONNECT_TIMEOUT_MS = 3_000;
    /** 读取超时：rerank 正常几百毫秒，超过 15s 直接降级走原顺序 */
    private static final int READ_TIMEOUT_MS = 15_000;

    @Override
    public List<Hit> rerank(String query, List<Hit> hits, int topK) {
        // 候选不足或为空：直接返回（不用调 API）
        if (hits == null || hits.isEmpty() || hits.size() <= topK) {
            return hits;
        }

        try {
            // 1. 构造 documents 列表（只取文本，控制在 API 单条 4K token 内）
            List<String> documents = new ArrayList<>();
            for (Hit hit : hits) {
                documents.add(truncate(hit.content(), 1500));
            }

            // 2. 构造请求体（DashScope 原生格式）--JSON格式
            ObjectNode requestBody = MAPPER.createObjectNode();
            requestBody.put("model", props.getRerankModel());

            ObjectNode input = requestBody.putObject("input");
            input.put("query", truncate(query, 500));
            input.putPOJO("documents", documents);

            ObjectNode parameters = requestBody.putObject("parameters");
            parameters.put("top_n", topK);
            parameters.put("return_documents", false);


            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(CONNECT_TIMEOUT_MS);
            requestFactory.setReadTimeout(READ_TIMEOUT_MS);

            RestClient client = RestClient.builder()
                    .requestFactory(requestFactory)
                    .defaultHeader("Authorization", "Bearer " + props.getRerankApiKey())
                    .defaultHeader("Content-Type", "application/json")
                    .build();

            String response = client.post()
                    .uri(props.getRerankBaseUrl())
                    .body(requestBody.toString())
                    .retrieve()
                    .body(String.class);

            // 4. 解析响应，按 relevance_score 降序重排
            JsonNode root = MAPPER.readTree(response);
            JsonNode results = root.path("output").path("results");

            List<Hit> reranked = new ArrayList<>();
            for (JsonNode node : results) {
                int index = node.path("index").asInt();
                double score = node.path("relevance_score").asDouble();
                if (index >= 0 && index < hits.size()) {
                    Hit original = hits.get(index);
                    reranked.add(new Hit(
                            original.articleId(),
                            original.articleTitle(),
                            original.heading(),
                            original.content(),
                            score
                    ));
                }
            }
            log.info("Rerank 完成: input={}, output={}, model={}", hits.size(), reranked.size(), props.getRerankModel());
            return reranked;

        } catch (Exception e) {

            log.warn("Rerank 调用失败，降级返回原始顺序前 topK: {}", e.getMessage());
            return hits.subList(0, Math.min(topK, hits.size()));
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max);
    }
}