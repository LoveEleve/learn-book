import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MiniTemplate — F-6 模板引擎核心逻辑极简复现 (harness)
 *
 * 纯逻辑模拟, 对照 OpenFeign 13.14 源码验证:
 *   A. {var} 解析: 简单变量替换 (Template.java:202-330 ChunkTokenizer)
 *   B. 嵌套花括号: 内层当字面量 (Template.java:289-300)
 *   C. URL 编码: 空格/中文 pct-encode; 已编码值不二次编码 (UriUtils.java:37-45, 111-141)
 *   D. 四位置策略: uri 编码 / header 不编码 / body 保留未解析 (策略矩阵)
 *   E. CollectionFormat: EXPLODED 重复键 vs CSV 分隔符 (CollectionFormat.java:64-89)
 */
public class MiniTemplate {

    /** B. 嵌套花括号: 逐字符切块, 层级计数 (Template.java:289-300) */
    static List<String> tokenize(String template) {
        List<String> chunks = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        int depth = 0;
        boolean inExpr = false;
        for (char c : template.toCharArray()) {
            if (c == '{') {
                if (!inExpr) {
                    if (cur.length() > 0) {
                        chunks.add(cur.toString());
                        cur.setLength(0);
                    }
                    inExpr = true;
                    cur.append(c);
                    depth = 1;
                } else {
                    depth++; // 内层 { → 嵌套
                    cur.append(c);
                }
            } else if (c == '}') {
                cur.append(c);
                if (inExpr) {
                    depth--;
                    if (depth == 0) {
                        chunks.add(cur.toString()); // 表达式整块 (含嵌套)
                        cur.setLength(0);
                        inExpr = false;
                    }
                }
            } else {
                cur.append(c);
            }
        }
        if (cur.length() > 0) {
            chunks.add(cur.toString());
        }
        return chunks;
    }

    /** C. 已编码检测 (UriUtils.isEncoded L37-45) + 幂等 pct-encode */
    static String pctEncode(String value, boolean allowReserved) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '%' && i + 2 < value.length() && isHex(value.charAt(i + 1)) && isHex(value.charAt(i + 2))) {
                sb.append(c).append(value.charAt(i + 1)).append(value.charAt(i + 2)); // %XX 段跳过
                i += 2;
            } else if (isUnreserved(c) || (allowReserved && isReserved(c))) {
                sb.append(c);
            } else {
                sb.append('%').append(String.format("%02X", (int) c));
            }
        }
        return sb.toString();
    }

    static boolean isHex(char c) {
        return (c >= '0' && c <= '9') || (c >= 'A' && c <= 'F') || (c >= 'a' && c <= 'f');
    }

    static boolean isUnreserved(char c) {
        return Character.isLetterOrDigit(c) || c == '-' || c == '.' || c == '_' || c == '~';
    }

    static boolean isReserved(char c) {
        return ":/?#[]@!$&'()*+,;=".indexOf(c) >= 0;
    }

    /** A/D. 展开: 按位置策略处理块列表 */
    static String expand(List<String> chunks, Map<String, String> vars, Position pos) {
        StringBuilder sb = new StringBuilder();
        for (String chunk : chunks) {
            if (chunk.startsWith("{") && chunk.endsWith("}")) {
                // 简单表达式: 检查内层是否有嵌套 (含 { 则整块字面量)
                String inner = chunk.substring(1, chunk.length() - 1);
                if (inner.contains("{")) {
                    sb.append(chunk); // 嵌套 → 字面量 (B)
                    continue;
                }
                String name = inner;
                int colon = inner.indexOf(':');
                if (colon > 0) {
                    name = inner.substring(0, colon);
                }
                String val = vars.get(name);
                if (val == null) {
                    if (pos == Position.BODY) {
                        sb.append(chunk); // body 保留未解析 (D)
                    }
                    continue; // uri/query/header: 未解析消失或忽略
                }
                if (pos == Position.HEADER) {
                    sb.append(val.replace("\r", "").replace("\n", "")); // 不编码 + stripCrlf
                } else if (pos == Position.BODY) {
                    sb.append(val);
                } else {
                    sb.append(pctEncode(val, false)); // uri/query 全编码
                }
            } else {
                sb.append(chunk);
            }
        }
        return sb.toString();
    }

    enum Position {
        URI, HEADER, BODY
    }

    /** E. CollectionFormat (CollectionFormat.java:64-89) */
    static String joinExploded(String field, List<String> values) {
        StringBuilder sb = new StringBuilder();
        for (String v : values) {
            if (sb.length() > 0) {
                sb.append('&');
            }
            sb.append(pctEncode(field, false)).append('=').append(pctEncode(v, false)); // 重复键
        }
        return sb.toString();
    }

    static String joinCsv(String field, List<String> values, char sep) {
        StringBuilder sb = new StringBuilder();
        sb.append(pctEncode(field, false)).append('=');
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sb.append(sep);
            }
            sb.append(pctEncode(values.get(i), false));
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        int pass = 0, fail = 0;
        String[] names = {"A.{var}解析", "B.嵌套花括号", "C.幂等编码", "D.位置策略", "E.集合格式"};
        boolean[] r = new boolean[5];

        // ---------- A. {var} 解析 ----------
        {
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("id", "42");
            String uri = expand(tokenize("/users/{id}"), vars, Position.URI);
            r[0] = uri.equals("/users/42");
            System.out.println("[A] {var}: /users/{id} + id=42 → " + uri);
        }

        // ---------- B. 嵌套花括号 ----------
        {
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("foo", "X");
            List<String> chunks = tokenize("foo{bar{baz}}");
            String result = expand(chunks, vars, Position.URI);
            r[1] = chunks.size() == 2 && chunks.get(1).equals("{bar{baz}}") && result.equals("foo{bar{baz}}");
            System.out.println("[B] 嵌套: chunks=" + chunks + " → " + result);
        }

        // ---------- C. 幂等编码 ----------
        {
            String encoded = pctEncode("a b%20c", false); // 空格编码, %20 已编码跳过
            r[2] = encoded.equals("a%20b%20c");
            System.out.println("[C] 幂等编码: \"a b%20c\" → " + encoded);
        }

        // ---------- D. 位置策略 ----------
        {
            Map<String, String> vars = new LinkedHashMap<>();
            vars.put("token", "Bearer a=b");
            String header = expand(tokenize("Authorization: {token}"), vars, Position.HEADER);
            String uri = expand(tokenize("/x/{token}"), vars, Position.URI);
            Map<String, String> empty = new LinkedHashMap<>();
            String body = expand(tokenize("{unresolved}"), empty, Position.BODY);
            String uriMissing = expand(tokenize("/x/{unresolved}"), empty, Position.URI);
            r[3] = header.equals("Authorization: Bearer a=b") // 不编码 = 保留
                    && uri.equals("/x/Bearer%20a%3Db") // 编码
                    && body.equals("{unresolved}") // body 保留未解析
                    && uriMissing.equals("/x/"); // uri 未解析消失
            System.out.println("[D] 策略: header=" + header + " uri=" + uri + " body缺失=" + body + " uri缺失=" + uriMissing);
        }

        // ---------- E. 集合格式 ----------
        {
            List<String> ids = List.of("1", "2", "3");
            String exploded = joinExploded("id", ids);
            String csv = joinCsv("id", ids, ',');
            r[4] = exploded.equals("id=1&id=2&id=3") && csv.equals("id=1,2,3");
            System.out.println("[E] 集合: EXPLODED=" + exploded + " CSV=" + csv);
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
