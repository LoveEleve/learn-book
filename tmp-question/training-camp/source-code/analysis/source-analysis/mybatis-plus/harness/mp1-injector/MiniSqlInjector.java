import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * MiniSqlInjector — MP-1 SQL 自动注入 极简复现 (harness)
 *
 * 验证四个核心控制流 (对照 MyBatis-Plus 源码):
 *   A. 12 默认方法面 + havePK 降级: 7 无条件 + 5 xxById 条件 (DefaultSqlInjector.java:39-60)
 *   B. inspectInject 装配循环 + mapperRegistryCache 防重复 (AbstractSqlInjector.java:44-65)
 *   C. 用户自定义优先: hasMappedStatement 已存在 → warn 跳过 (AbstractMethod.java:252-273)
 *   D. SqlMethod 模板 String.format 填充 (SelectById 模式)
 */
public class MiniSqlInjector {

    static class TableInfo {
        final String tableName, keyColumn, keyProperty;
        final boolean havePK;
        TableInfo(String tn, String kc, String kp, boolean pk) { tableName = tn; keyColumn = kc; keyProperty = kp; havePK = pk; }
        String getLogicDeleteSql() { return ""; }
        String getAllSqlSelect() { return "id,name"; }
    }

    // ===== D. SqlMethod 模板 =====
    enum SqlMethod {
        SELECT_BY_ID("selectById", "SELECT %s FROM %s WHERE %s=#{%s}%s"),
        INSERT_ONE("insert", "INSERT INTO %s %s VALUES %s");
        final String method;
        final String sql;
        SqlMethod(String m, String sql) { method = m; this.sql = sql; }
        String getMethod() { return method; }
        String getSql() { return sql; }
    }

    // ===== A. 方法面 =====
    static class DefaultSqlInjector {
        List<String> getMethodList(TableInfo tableInfo) {
            List<String> list = new ArrayList<>();
            list.add("insert"); list.add("delete"); list.add("update");
            list.add("selectCount"); list.add("selectMaps"); list.add("selectObjs"); list.add("selectList");
            if (tableInfo.havePK) {
                list.add("deleteById"); list.add("deleteByIds"); list.add("updateById");
                list.add("selectById"); list.add("selectBatchByIds");
            } else {
                System.out.println("    (warn) Not found @TableId, Cannot use 'xxById' Method");
            }
            return list;
        }
    }

    // ===== B/C. 装配 + 用户优先 =====
    static class MiniConfiguration {
        final Set<String> statements = new ConcurrentSkipListSet<>();  // StrictMap 简化
        final Set<String> mapperRegistryCache = new ConcurrentSkipListSet<>();
        int injected = 0;

        boolean hasStatement(String name) { return statements.contains(name); }
        void addMappedStatement(String name) {
            if (hasStatement(name)) {
                System.out.println("    (warn) [" + name + "] Has been loaded by XML or SqlProvider or Annotation, ignoring this injection");
                return;                                            // 用户自定义优先
            }
            statements.add(name);
            injected++;
        }

        void inspectInject(String mapperClass, TableInfo tableInfo, boolean alreadyInjected) {
            String key = mapperClass;
            if (mapperRegistryCache.contains(key)) {
                System.out.println("    (skip) mapper already injected");
                return;                                            // 防重复
            }
            for (String method : new DefaultSqlInjector().getMethodList(tableInfo)) {
                addMappedStatement(mapperClass + "." + method);    // inject 循环
            }
            mapperRegistryCache.add(key);
        }
    }

    static int passed = 0, failed = 0;
    static void check(String name, boolean cond) {
        if (cond) { passed++; System.out.println("  PASS  " + name); }
        else { failed++; System.out.println("  FAIL  " + name); }
    }

    public static void main(String[] args) {
        System.out.println("== A. 12 方法面 + havePK 降级 ==");
        DefaultSqlInjector injector = new DefaultSqlInjector();
        List<String> withPk = injector.getMethodList(new TableInfo("user", "id", "id", true));
        check("有主键 12 方法: " + withPk.size(), withPk.size() == 12 && withPk.contains("selectById"));
        List<String> noPk = injector.getMethodList(new TableInfo("log", null, null, false));
        check("无主键 7 方法无 xxById: " + noPk.size(), noPk.size() == 7 && !noPk.contains("selectById"));

        System.out.println("== B. 装配循环 + 防重复 ==");
        MiniConfiguration cfg = new MiniConfiguration();
        cfg.inspectInject("UserMapper", new TableInfo("user", "id", "id", true), false);
        check("注入 12 语句: " + cfg.injected, cfg.injected == 12);
        int before = cfg.injected;
        cfg.inspectInject("UserMapper", new TableInfo("user", "id", "id", true), false);
        check("重复 inspect 跳过: " + (cfg.injected == before), cfg.injected == before);

        System.out.println("== C. 用户自定义优先 ==");
        MiniConfiguration cfg2 = new MiniConfiguration();
        cfg2.statements.add("UserMapper.selectById");   // 模拟 XML 已定义
        cfg2.inspectInject("UserMapper", new TableInfo("user", "id", "id", true), false);
        check("XML 已有 selectById → 注入 11 条 (跳过 1): " + cfg2.injected, cfg2.injected == 11);
        check("selectById 仍是用户版本: " + cfg2.statements.contains("UserMapper.selectById"), cfg2.statements.contains("UserMapper.selectById"));

        System.out.println("== D. SqlMethod 模板填充 ==");
        String sql = String.format(SqlMethod.SELECT_BY_ID.getSql(),
            "id,name", "user", "id", "id", " AND deleted=0");   // 5 占位: 列/表/keyColumn/keyProperty/逻辑删除
        check("模板填充: " + sql, "SELECT id,name FROM user WHERE id=#{id} AND deleted=0".equals(sql));
        check("INSERT 模板: " + String.format(SqlMethod.INSERT_ONE.getSql(), "user", "(id,name)", "(#{id},#{name})"),
            "INSERT INTO user (id,name) VALUES (#{id},#{name})".equals(String.format(SqlMethod.INSERT_ONE.getSql(), "user", "(id,name)", "(#{id},#{name})")));

        System.out.println("\n== 结果: " + passed + " passed / " + failed + " failed ==");
        if (failed > 0) { System.exit(1); }
    }
}
