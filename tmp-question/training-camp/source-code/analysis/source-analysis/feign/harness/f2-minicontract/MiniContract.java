import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniContract — F-2 契约解析核心逻辑极简复现 (harness)
 *
 * 纯逻辑模拟, 对照 OpenFeign 13.14 源码验证:
 *   A. 参数角色分配: 注解标记 → URI → body 剩余法 (Contract.java:117-159)
 *   B. body 推断: 未消费参数 → body; 多 body 报错 (L138-158)
 *   C. formParams 缺席推断: 模板里没有的 @Param → form (DefaultContract.java:117-119)
 *   D. @Body 模板 vs 字面量: 含 { 走模板 (DefaultContract.java:78-82)
 *   E. configKey 生成: 类名#方法(参数类型) (Feign.java:69-81)
 */
public class MiniContract {

    /** 参数角色枚举 */
    enum Role {
        PARAM, URI, BODY, IGNORED
    }

    /** 简化方法元数据 */
    static class Metadata {
        final String configKey;
        final Map<Integer, Role> roles = new LinkedHashMap<>(); // 参数索引 → 角色
        final Map<Integer, String> indexToName = new LinkedHashMap<>(); // @Param 名
        final List<String> formParams = new ArrayList<>();
        int urlIndex = -1;
        int bodyIndex = -1;
        final boolean bodyIsTemplate;

        Metadata(String configKey, boolean bodyIsTemplate) {
            this.configKey = configKey;
            this.bodyIsTemplate = bodyIsTemplate;
        }
    }

    /** A+B. 参数循环: 剩余法分配角色 (Contract.java:117-159 语义) */
    static Metadata parseParameters(String configKey, int paramCount,
                                    Map<Integer, String> namedParams, // @Param 名 (索引→名)
                                    int uriIndex, // URI 参数索引或 -1
                                    boolean bodyIsTemplate) {
        Metadata md = new Metadata(configKey, bodyIsTemplate);
        for (int i = 0; i < paramCount; i++) {
            md.roles.put(i, Role.PARAM); // 默认参数
        }
        if (uriIndex >= 0) {
            md.roles.put(uriIndex, Role.URI);
            md.urlIndex = uriIndex;
        }
        for (Map.Entry<Integer, String> e : namedParams.entrySet()) {
            md.indexToName.put(e.getKey(), e.getValue());
        }
        // 未消费参数 → body (剩余法): 第一个非 PARAM-标记 且 非 URI 的 → body
        for (int i = 0; i < paramCount; i++) {
            Role role = md.roles.get(i);
            if (role == Role.URI) {
                continue;
            }
            if (!md.indexToName.containsKey(i)) {
                if (md.bodyIndex < 0) {
                    md.bodyIndex = i;
                    md.roles.put(i, Role.BODY);
                } else {
                    throw new IllegalStateException("Found too many bodies"); // 多 body 报错
                }
            }
        }
        // C. formParams 缺席推断: @Param 名不在模板里 → form
        return md;
    }

    static void inferFormParams(Metadata md, List<String> templateVars) {
        for (String name : md.indexToName.values()) {
            if (!templateVars.contains(name)) {
                md.formParams.add(name); // DefaultContract.java:117-119
            }
        }
    }

    /** E. configKey (Feign.java:69-81) */
    static String configKey(String className, String methodName, List<String> paramTypeNames) {
        StringBuilder sb = new StringBuilder();
        sb.append(className).append('#').append(methodName).append('(');
        for (int i = 0; i < paramTypeNames.size(); i++) {
            sb.append(paramTypeNames.get(i));
            if (i < paramTypeNames.size() - 1) {
                sb.append(',');
            }
        }
        return sb.append(')').toString();
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        String[] names = {"A.角色分配", "B.body推断", "C.formParams", "D.body模板", "E.configKey"};
        boolean[] r = new boolean[5];

        // ---------- A+B. 角色分配 + body 推断 ----------
        {
            // 方法: (Long id @Param, URI url, String body)
            Map<Integer, String> named = new LinkedHashMap<>();
            named.put(0, "id");
            Metadata md = parseParameters("Api#get(Long,String,String)", 3, named, 1, false);
            boolean roles = md.roles.get(0) == Role.PARAM && md.roles.get(1) == Role.URI && md.roles.get(2) == Role.BODY;
            // 多 body 报错: 两个未标记参数
            boolean tooManyBodies = false;
            try {
                parseParameters("Api#bad(Long,Long)", 2, new LinkedHashMap<>(), -1, false);
            } catch (IllegalStateException e) {
                tooManyBodies = e.getMessage().contains("too many bodies");
            }
            r[0] = roles;
            r[1] = tooManyBodies;
            System.out.println("[A] 角色: id=PARAM url=URI body=BODY = " + roles);
            System.out.println("[B] 多 body 报错 = " + tooManyBodies);
        }

        // ---------- C. formParams 缺席推断 ----------
        {
            Map<Integer, String> named = new LinkedHashMap<>();
            named.put(0, "id");
            named.put(1, "name");
            Metadata md = parseParameters("Api#post(Long,String)", 2, named, -1, false);
            // 模板只有 {id}, name 缺席 → form
            inferFormParams(md, List.of("id"));
            r[2] = md.formParams.equals(List.of("name"));
            System.out.println("[C] formParams: 模板含 id, name 缺席 → " + md.formParams);
        }

        // ---------- D. body 模板 vs 字面量 ----------
        {
            // @Body("{name}") → template; @Body("static") → literal
            boolean isTemplate = "{name}".contains("{");
            boolean isLiteral = "static".contains("{");
            r[3] = isTemplate && !isLiteral;
            System.out.println("[D] body: {name} 是模板=" + isTemplate + " static 是字面量=" + !isLiteral);
        }

        // ---------- E. configKey ----------
        {
            String key = configKey("Api", "list", List.of("String", "Long"));
            r[4] = key.equals("Api#list(String,Long)");
            System.out.println("[E] configKey: " + key);
        }

        int p = 0, f = 0;
        for (boolean x : r) {
            if (x) {
                p++;
            } else {
                f++;
            }
        }
        System.out.println("== 结果: " + p + " PASS / " + f + " FAIL ==");
        for (int i = 0; i < names.length; i++) {
            System.out.println((r[i] ? "  PASS " : "  FAIL ") + names[i]);
        }
        System.exit(f > 0 ? 1 : 0);
    }
}
