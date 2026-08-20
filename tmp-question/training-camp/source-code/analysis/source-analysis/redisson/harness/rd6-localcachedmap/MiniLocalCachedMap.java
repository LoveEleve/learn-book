import java.util.*;
import java.util.concurrent.*;

/**
 * MiniLocalCachedMap — RD-6 RLocalCachedMap 本地缓存 极简复现 (harness)
 *
 * 验证三个核心控制流 (对照 Redisson 4.6.2 源码):
 *   A. 双层级读路径: 本地 hit 零网络; miss → Redis 回填
 *      (RedissonLocalCachedMap.java:285-317)
 *   B. SyncStrategy: INVALIDATE (广播 hash, 接收者清+回源) vs UPDATE (广播全值, 接收者直更)
 *      (LocalCacheListener.java:276-315)
 *   C. excludedId: 广播带 instanceId, 接收者排除自己 (防自处理循环)
 *      (LocalCacheListener.java:263)
 *
 * 机制复现, 非完整库 — 用 HashMap 模拟本地缓存 + 消息队列模拟发布订阅。
 */
public class MiniLocalCachedMap {

    // 模拟 Redis 远端 (Map) + 消息总线 (队列)
    static class Bus {
        final Queue<Msg> queue = new ConcurrentLinkedQueue<>();

        void publish(Msg m) { queue.add(m); }
    }

    static class Msg {
        final int senderId;   // instanceId
        final String keyHash; // INVALIDATE: hash / UPDATE: key
        final String value;   // UPDATE: value / INVALIDATE: null
        Msg(int senderId, String keyHash, String value) { this.senderId = senderId; this.keyHash = keyHash; this.value = value; }
    }

    // 实例: 本地缓存 + 远端引用 + 消息处理
    static class Instance {
        final int id;
        final Map<String, String> remote;   // Redis
        final Map<String, String> local;    // 本地缓存
        final Bus bus;
        int redisReads = 0; // 统计回源次数

        Instance(int id, Map<String, String> remote, Bus bus) {
            this.id = id; this.remote = remote; this.bus = bus; this.local = new ConcurrentHashMap<>();
        }

        // get: 本地优先, miss → Redis 回填 (RedissonLocalCachedMap:285-317)
        String get(String key) {
            String v = local.get(key);
            if (v != null) return v; // 本地 hit 零网络
            redisReads++;
            String rv = remote.get(key);
            if (rv != null) local.put(key, rv); // 回填
            return rv;
        }

        // put (SYNC 模式): 本地写 + Redis 写 + 广播 (L103-125,361-401)
        void put(String key, String value, boolean useUpdate) {
            local.put(key, value);               // 本实例直接更新
            remote.put(key, value);              // Redis 主存
            // 广播给其他实例 (带 excludedId = 自己)
            Msg m = useUpdate
                    ? new Msg(id, key, value)    // UPDATE: 带值
                    : new Msg(id, key, null);    // INVALIDATE: 只 hash
            bus.publish(m);
        }

        // 处理一条消息 (LocalCacheListener:263-315)
        void onMessage(Msg m) {
            if (m.senderId == id) return; // excludedId 排除自己 (L263)
            if (m.value == null) {
                local.remove(m.keyHash);  // INVALIDATE: 清本地 → 下次 miss 回源
            } else {
                local.put(m.keyHash, m.value); // UPDATE: 直更本地
            }
        }

        // 处理所有待处理消息
        void drain() {
            Msg m;
            while ((m = bus.queue.poll()) != null) onMessage(m);
        }
    }
}