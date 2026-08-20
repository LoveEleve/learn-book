import java.util.*;

/**
 * MiniMappingTest — E-7 harness 验证入口
 *
 * 跑法: javac MiniMapping.java MiniMappingTest.java && java MiniMappingTest
 * 全部 PASS = Mapping 类型推断/四态/解析四变体/兜底链理解到位
 * (对照 DynamicFieldsBuilder.java:47-152 / ObjectMapper.java:45-58 / KeywordFieldMapper.java:874,905)。
 */
public class MiniMappingTest {

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("PASS " + name); }
        else { failed++; System.out.println("FAIL " + name); }
    }

    // A. 类型推断算法 (DynamicFieldsBuilder.java:47-152)
    static void testInferType() {
        MiniMapping.Index idx = new MiniMapping.Index();
        check("A1 纯数字字符串→LONG", MiniMapping.inferType("25", idx) == MiniMapping.FieldDef.Type.LONG);
        check("A2 小数→DOUBLE", MiniMapping.inferType("3.14", idx) == MiniMapping.FieldDef.Type.DOUBLE);
        check("A3 日期格式→DATE", MiniMapping.inferType("2020-01-01", idx) == MiniMapping.FieldDef.Type.DATE);
        check("A4 普通字符串→TEXT", MiniMapping.inferType("hello", idx) == MiniMapping.FieldDef.Type.TEXT);
        check("A5 纯数字拒绝 date (注释 L75-78)", MiniMapping.inferType("2024", idx) == MiniMapping.FieldDef.Type.LONG);
        idx.numericDetection = false;
        check("A6 numericDetection=false → 数字不再转 LONG", MiniMapping.inferType("25", idx) == MiniMapping.FieldDef.Type.TEXT);
        idx.numericDetection = true; idx.dateDetection = false;
        check("A7 dateDetection=false → 日期转 TEXT", MiniMapping.inferType("2020-01-01", idx) == MiniMapping.FieldDef.Type.TEXT);
    }

    // B. dynamic 四态 (ObjectMapper.java:45-58)
    static void testDynamicModes() {
        // TRUE: 动态建字段
        MiniMapping.Index idx = new MiniMapping.Index();
        idx.dynamic = MiniMapping.Index.Dynamic.TRUE;
        MiniMapping.ParsedDoc doc = MiniMapping.parse(idx, Map.of("newfield", "25"));
        check("B1 TRUE: 动态字段被建", idx.fields.containsKey("newfield"));
        check("B2 TRUE: 推断为 LONG", idx.fields.get("newfield").type == MiniMapping.FieldDef.Type.LONG);
        check("B3 TRUE: 值保留", doc.values.containsKey("newfield"));

        // FALSE: 忽略
        idx = new MiniMapping.Index();
        idx.dynamic = MiniMapping.Index.Dynamic.FALSE;
        doc = MiniMapping.parse(idx, Map.of("newfield", "25"));
        check("B4 FALSE: 不建字段", !idx.fields.containsKey("newfield"));

        // STRICT: 拒绝
        idx = new MiniMapping.Index();
        idx.dynamic = MiniMapping.Index.Dynamic.STRICT;
        doc = MiniMapping.parse(idx, Map.of("newfield", "25"));
        check("B5 STRICT: 整篇拒绝", doc.rejected);

        // RUNTIME: 值保留不固化
        idx = new MiniMapping.Index();
        idx.dynamic = MiniMapping.Index.Dynamic.RUNTIME;
        doc = MiniMapping.parse(idx, Map.of("newfield", "25"));
        check("B6 RUNTIME: 值保留", doc.values.containsKey("newfield"));
        check("B7 RUNTIME: 不建具体字段", !idx.fields.containsKey("newfield"));
    }

    // C. parseCreateField 四变体
    static void testParseVariants() {
        MiniMapping.Index idx = new MiniMapping.Index();
        idx.fields.put("name", new MiniMapping.FieldDef("name", MiniMapping.FieldDef.Type.TEXT, false, 0));
        idx.fields.put("tag", new MiniMapping.FieldDef("tag", MiniMapping.FieldDef.Type.KEYWORD, false, 5));
        idx.fields.put("age", new MiniMapping.FieldDef("age", MiniMapping.FieldDef.Type.LONG, false, 0));
        idx.fields.put("birth", new MiniMapping.FieldDef("birth", MiniMapping.FieldDef.Type.DATE, false, 0));

        MiniMapping.ParsedDoc doc = MiniMapping.parse(idx, Map.of(
            "name", "张三", "tag", "abcdefgh", "age", "25", "birth", "2020-01-01"
        ));
        check("C1 TEXT 原样", doc.values.get("name").equals("张三"));
        check("C2 KEYWORD 超 ignoreAbove 被忽略", doc.ignored.contains("tag"));
        check("C3 NUMBER 字符串强转", doc.values.get("age").equals(25L));
        check("C4 DATE 转 epoch millis", doc.values.get("birth") instanceof Long);
    }

    // D. ignore_malformed 兜底链
    static void testIgnoreMalformed() {
        // 默认 false → 抛错
        MiniMapping.Index idx = new MiniMapping.Index();
        idx.fields.put("age", new MiniMapping.FieldDef("age", MiniMapping.FieldDef.Type.LONG, false, 0));
        MiniMapping.ParsedDoc doc = MiniMapping.parse(idx, Map.of("age", "abc"));
        check("D1 ignoreMalformed=false 非法值报错", doc.errors.contains("age"));

        // true → 忽略
        idx = new MiniMapping.Index();
        idx.fields.put("age", new MiniMapping.FieldDef("age", MiniMapping.FieldDef.Type.LONG, true, 0));
        doc = MiniMapping.parse(idx, Map.of("age", "abc"));
        check("D2 ignoreMalformed=true 忽略", doc.ignored.contains("age"));
        check("D3 忽略后无值", !doc.values.containsKey("age"));
    }

    public static void main(String[] args) {
        testInferType();
        testDynamicModes();
        testParseVariants();
        testIgnoreMalformed();
        System.out.println("----");
        System.out.println("PASS=" + passed + " FAIL=" + failed);
        if (failed > 0) System.exit(1);
    }
}
