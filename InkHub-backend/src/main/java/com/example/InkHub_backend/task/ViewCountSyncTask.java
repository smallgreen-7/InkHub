package com.example.InkHub_backend.task;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.InkHub_backend.entity.Article;
import com.example.InkHub_backend.mapper.ArticleMapper;
import com.example.InkHub_backend.utils.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

// 每 10 分钟把 Redis 里的浏览量增量写回 MySQL
@Slf4j
@Component
@EnableScheduling
@RequiredArgsConstructor
public class ViewCountSyncTask {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ArticleMapper articleMapper;

    @Scheduled(fixedDelay = 600000)   // 10 分钟一次
    public void sync() {
        // SCAN 匹配所有 article:view:* 的 key
        try (Cursor<byte[]> cursor = redisTemplate
                .getConnectionFactory()
                .getConnection()
                .scan(ScanOptions.scanOptions().match("article:view:*").count(100).build())) {
            while (cursor.hasNext()) {
                String key = new String(cursor.next());
                Long articleId = Long.valueOf(key.substring(key.lastIndexOf(':') + 1));
                /* key.lastIndexOf(':') + 1)：查最后一个‘；’，然后+1（不取冒号本身，取后一个），返回对应索引下标
                 * substring从对应下标开始取字符串到末尾，返回新的字符串
                 * Long.valueOf：强转；把数字字符串转成Long包装对象
                 */
                Object val = redisTemplate.opsForValue().get(key);
                /* opsForValue() 拿到操作 Redis String 的工具，但只能读写 String 类型的 key。*/
                if (val instanceof Number n && n.longValue() > 0) {
                    // 增量写回：view_count = view_count + 当前 Redis 值
                    articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                            .setSql("view_count = view_count + " + n.longValue())
                            .eq(Article::getId, articleId));
                    // 写回后清零，下次继续累计增量
                    redisTemplate.opsForValue().set(key, 0);
                }
            }
        } catch (Exception e) {
            log.error( "浏览量批量落库失败，下轮重试" , e);
            // 落库失败不要影响主流程，下轮再试
        }
    }
}
