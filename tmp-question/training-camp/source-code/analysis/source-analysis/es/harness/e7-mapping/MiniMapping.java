import java.util.*;

/**
 * MiniMapping — E-7 Mapping 极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 ES 8.12.2 源码):
 *   A. 类型推断算法: VALUE_STRING 先 Long 再 Double, 纯数字拒绝 date 检测
 *      (DynamicFieldsBuilder.java:47-152, 纯数字拒绝注释 L75-78)
 *   B. dynamic 四态: TRUE/FALSE/STRICT/RUNTIME
 *      (ObjectMapper.java:45-58; DocumentParser.java:673-693)
 *   C. parseCreateField 类型化解析: text 原样 / keyword ignoreAbove 截断 / number 强转 / date 转 epoch
 *      (TextFieldMapper.java:1243 / KeywordFieldMapper.java:874,905 / NumberFieldMapper.java:1830 / DateFieldMapper.java:899)
 *   D. ignore_malformed 兜底链: 非法值 → 抛错或忽略
 *      (NumberFieldMapper.java:1833-1841; FieldMapper.java:61-64 默认 false)
 *
 * 用简单类模拟字段定义与解析 (机制复现非完整库)。
 */
public class MiniMapping {

    /** 字段定义: 名称 + 类型 + 参数 (对照 Mapper + Parameter) */
    static class FieldDef {
        enum Type { TEXT, KEYWORD, LONG, DOUBLE, DATE, BOOLEAN }
        final String name;
        final Type type;
        final boolean ignoreMalformed;   // ignore_malformed 参数
        final int ignoreAbove;           // keyword ignore_above
        FieldDef(String name, Type type, boolean ignoreMalformed, int ignoreAbove) {
            this.name = name;
            this.type = type;
            this.ignoreMalformed = ignoreMalformed;
            this.ignoreAbove = ignoreAbove;
        }
    }

    /** 索引: 字段表 + dynamic 模式 (对照 MapperService + ObjectMapper.Dynamic) */
    static class Index {
        enum Dynamic { TRUE, FALSE, STRICT, RUNTIME }
        final Map<String, FieldDef> fields = new LinkedHashMap<>();
        Dynamic dynamic = Dynamic.TRUE;
        boolean numericDetection = true;
        boolean dateDetection = true;

        FieldDef get(String name) { return fields.get(name); }
    }

    /** 解析结果: 字段 → 值 (对照 ParsedDocument) */
    static class ParsedDoc {
        final Map<String, Object> values = new LinkedHashMap<>();
        final List<String> ignored = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
        boolean rejected;
    }

    // A. 类型推断算法 (DynamicFieldsBuilder.java:47-152)
    static MiniMapping.FieldDef.Type inferType(String value, Index idx) {
        // VALUE_STRING 分支
        boolean parseableAsLong = false, parseableAsDouble = false;
        try { Long.parseLong(value); parseableAsLong = true; } catch (NumberFormatException ignored) {}
        if (!parseableAsLong) {
            try { Double.parseDouble(value); parseableAsDouble = true; } catch (NumberFormatException ignored) {}
        }
        if (parseableAsLong && idx.numericDetection) return MiniMapping.FieldDef.Type.LONG;
        if (parseableAsDouble && idx.numericDetection) return MiniMapping.FieldDef.Type.DOUBLE;
        if (!parseableAsLong && !parseableAsDouble && idx.dateDetection) {
            // 纯数字已被上面排除 → 这里只可能是"非纯数字"字符串
            // 对照注释 L75-78: "We refuse to match pure numbers, which are too likely to be false
            // positives with date formats that include eg. epoch_millis or YYYY"
            if (looksLikeDate(value)) return MiniMapping.FieldDef.Type.DATE;
        }
        return MiniMapping.FieldDef.Type.TEXT;
    }

    static boolean looksLikeDate(String s) {
        // 简化: YYYY-MM-DD 或 YYYY/MM/DD
        return s.matches("\\d{4}[-/]\\d{1,2}[-/]\\d{1,2}");
    }

    // B+C+D. 文档解析主流程 (DocumentParser.parseDocument + FieldMapper.parseCreateField)
    static ParsedDoc parse(Index idx, Map<String, Object> doc) {
        ParsedDoc result = new ParsedDoc();
        for (Map.Entry<String, Object> e : doc.entrySet()) {
            String name = e.getKey();
            Object value = e.getValue();
            FieldDef def = idx.get(name);

            if (def == null) {
                // 动态字段 (DocumentParser.parseDynamicValue L673-682)
                switch (idx.dynamic) {
                    case STRICT -> { result.rejected = true; return result; } // StrictDynamicMappingException
                    case FALSE -> { /* 忽略 */ }
                    case RUNTIME -> result.values.put(name, value); // runtime: 不固化类型
                    case TRUE -> {
                        MiniMapping.FieldDef.Type t = value instanceof String s
                            ? inferType(s, idx)
                            : value instanceof Integer || value instanceof Long ? MiniMapping.FieldDef.Type.LONG
                            : value instanceof Double || value instanceof Float ? MiniMapping.FieldDef.Type.DOUBLE
                            : value instanceof Boolean ? MiniMapping.FieldDef.Type.BOOLEAN
                            : MiniMapping.FieldDef.Type.TEXT;
                        result.values.put(name, value);
                        idx.fields.put(name, new FieldDef(name, t, false, 0)); // 动态建字段 (createDynamicUpdate)
                    }
                }
                continue;
            }

            // 已定义字段 → parseCreateField 四变体
            switch (def.type) {
                case TEXT -> result.values.put(name, value); // 直接索引 (Text: new Field)
                case KEYWORD -> {
                    String s = String.valueOf(value);
                    if (def.ignoreAbove > 0 && s.length() > def.ignoreAbove) {
                        result.ignored.add(name); // ignoreAbove 截断 (KeywordFieldMapper L905-915)
                    } else {
                        result.values.put(name, s);
                    }
                }
                case LONG -> {
                    try {
                        result.values.put(name, Long.parseLong(String.valueOf(value))); // Number 强转 (coerce)
                    } catch (NumberFormatException ex) {
                        if (def.ignoreMalformed) {
                            result.ignored.add(name); // ignoreMalformed → addIgnoredField (L1833-1841)
                        } else {
                            result.errors.add(name);  // 默认抛 DocumentParsingException
                        }
                    }
                }
                case DATE -> {
                    String s = String.valueOf(value);
                    if (looksLikeDate(s) || s.matches("\\d{13}")) {
                        result.values.put(name, toEpochMillis(s)); // DateFieldMapper: 字符串→epoch millis
                    } else if (def.ignoreMalformed) {
                        result.ignored.add(name);
                    } else {
                        result.errors.add(name);
                    }
                }
                case DOUBLE -> {
                    try {
                        result.values.put(name, Double.parseDouble(String.valueOf(value)));
                    } catch (NumberFormatException ex) {
                        if (def.ignoreMalformed) result.ignored.add(name); else result.errors.add(name);
                    }
                }
                case BOOLEAN -> result.values.put(name, Boolean.valueOf(String.valueOf(value)));
            }
        }
        return result;
    }

    static long toEpochMillis(String s) {
        if (s.matches("\\d{13}")) return Long.parseLong(s); // epoch_millis 原样
        // 简化: 用固定基准模拟日期→毫秒
        return (long) s.hashCode() % 1000000000000L;
    }
}
