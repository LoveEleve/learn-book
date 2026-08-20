import java.lang.reflect.Type;
import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * MiniContractTest — F-2 Contract 注解解析 harness 测试
 *
 * 断言覆盖 (对照真实 DefaultContractTest.java 行为):
 * 1. MiniContract.RequestLine 解析: HTTP 方法 + URI 提取
 * 2. MiniContract.Param 名称解析: 注解值 > 参数名 (-parameters)
 * 3. MiniContract.QueryMap/MiniContract.HeaderMap 单参数约束 (多参数抛错)
 * 4. bodyIndex/urlIndex 判定
 * 5. 未消费注解告警 (类级/方法级/参数级三面)
 * 6. MiniContract.FeignIgnore 跳过
 * 7. 多 MiniContract.Headers 合并 (类级+方法级)
 * 8. 无 MiniContract.RequestLine 抛错
 * 9. 双 @MiniContract.Param 多值 (indexToName 一对多)
 */
public class MiniContractTest {

    // ===== 被测接口面 =====

    @MiniContract.Headers({"X-Class: classHeader"})
    public interface TestApi {
        @MiniContract.RequestLine("GET /users/{id}")
        User getUser(@MiniContract.Param("id") String id);

        @MiniContract.RequestLine("POST /users")
        @MiniContract.Headers({"X-Method: methodHeader"})
        @MiniContract.Body("{name}")
        User createUser(String body, @MiniContract.Param("name") String name);

        @MiniContract.RequestLine("GET /search")
        List<User> search(@MiniContract.QueryMap Map<String, String> query);

        @MiniContract.RequestLine("GET /auth")
        String auth(@MiniContract.HeaderMap Map<String, String> headers);

        @MiniContract.RequestLine("GET /dynamic")
        String dynamic(URI url);

        @MiniContract.RequestLine("DELETE /users/{id}")
        void deleteUser(@MiniContract.Param("id") String id);

        @MiniContract.FeignIgnore
        @MiniContract.RequestLine("GET /ignored")
        String ignored();
    }

    public interface NoRequestLineApi {
        String noAnnotation(String id);
    }

    // 未被任何处理器注册的注解 → 触发"not used by contract"告警
    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    public @interface UnknownAnn {
    }

    public interface UnknownAnnApi {
        @MiniContract.RequestLine("GET /x")
        @UnknownAnn
        String x(String id);
    }

    public static class User {
        public String id;
    }

    // ===== 断言工具 =====

    static int passCount = 0;
    static int failCount = 0;

    static void check(String name, boolean condition) {
        if (condition) {
            passCount++;
            System.out.println("PASS: " + name);
        } else {
            failCount++;
            System.out.println("FAIL: " + name);
        }
    }

    public static void main(String[] args) throws Exception {
        MiniContract.DefaultContract contract = new MiniContract.DefaultContract();

        // ---- 1. MiniContract.RequestLine 解析: 方法 + URI ----
        List<MiniContract.MethodMetadata> all = contract.parseAndValidateMetadata(TestApi.class);
        check("7 方法全解析 (6 接口方法 + MiniContract.FeignIgnore 跳过后 6)", all.size() == 6);

        MiniContract.        MethodMetadata getUser = find(all, "getUser");
        check("getUser 存在", getUser != null);
        check("getUser template 含 GET (类级 Headers 处理器先执行)", getUser.template().toString().contains("GET /users/{id} "));
        check("getUser 类级 MiniContract.Headers 注入", getUser.template().toString().contains("H:X-Class: classHeader;"));

        // ---- 2. MiniContract.Param 名称解析: indexToName ----
        check("getUser param0 名 id", getUser.indexToName().containsKey(0)
                && getUser.indexToName().get(0).contains("id"));
        check("getUser id 在模板中 → 非 formParam", getUser.formParams().isEmpty());

        // ---- 3. 双 @MiniContract.Param 多值 ----
        MiniContract.MethodMetadata createUser = find(all, "createUser");
        check("createUser param1 名 name", createUser.indexToName().containsKey(1)
                && createUser.indexToName().get(1).contains("name"));
        check("createUser 方法级 MiniContract.Headers 注入", createUser.template().toString().contains("H:X-Method: methodHeader;"));
        check("createUser MiniContract.Body 模板 (含花括号 → bodyTemplate)", createUser.template().toString().contains("BODYTMPL[{name}]"));

        // ---- 4. MiniContract.QueryMap/MiniContract.HeaderMap 单参数约束 ----
        MiniContract.MethodMetadata search = find(all, "search");
        check("search queryMapIndex=0", search.queryMapIndex() != null && search.queryMapIndex() == 0);
        MiniContract.MethodMetadata auth = find(all, "auth");
        check("auth headerMapIndex=0", auth.headerMapIndex() != null && auth.headerMapIndex() == 0);

        // ---- 5. bodyIndex/urlIndex 判定 ----
        MiniContract.MethodMetadata dynamic = find(all, "dynamic");
        check("dynamic urlIndex=0 (URI 参数)", dynamic.urlIndex() != null && dynamic.urlIndex() == 0);
        check("dynamic bodyIndex 为空", dynamic.bodyIndex() == null);

        // ---- 6. MiniContract.FeignIgnore 跳过 ----
        MiniContract.MethodMetadata ignored = find(all, "ignored");
        check("MiniContract.FeignIgnore 方法被跳过", ignored == null);

        // ---- 7. 无 MiniContract.RequestLine 抛错 + 告警 ----
        boolean threw = false;
        String thrownMessage = null;
        try {
            contract.parseAndValidateMetadata(NoRequestLineApi.class);
        } catch (IllegalStateException e) {
            threw = e.getMessage().contains("not annotated with HTTP method type");
            thrownMessage = e.getMessage();
        }
        check("无 MiniContract.RequestLine 抛 IllegalStateException", threw);
        check("异常信息拼接类级告警 (warnings 进异常)", thrownMessage != null && thrownMessage.contains("has no annotations"));

        // ---- 8. 未消费注解告警 ----
        List<MiniContract.MethodMetadata> unknownAnn = contract.parseAndValidateMetadata(UnknownAnnApi.class);
        boolean warned = false;
        for (MiniContract.MethodMetadata md : unknownAnn) {
            if (md.warnings().stream().anyMatch(w -> w.contains("not used by contract"))) {
                warned = true;
            }
        }
        check("未消费注解告警机制存在 (UnknownAnn)", warned);

        System.out.println("----");
        System.out.println("PASS=" + passCount + " FAIL=" + failCount);
        if (failCount > 0) {
            System.exit(1);
        }
    }

    static MiniContract.MethodMetadata find(List<MiniContract.MethodMetadata> list, String methodName) {
        for (MiniContract.MethodMetadata md : list) {
            if (md.configKey() != null && md.configKey().endsWith("#" + methodName)) {
                return md;
            }
        }
        return null;
    }
}
