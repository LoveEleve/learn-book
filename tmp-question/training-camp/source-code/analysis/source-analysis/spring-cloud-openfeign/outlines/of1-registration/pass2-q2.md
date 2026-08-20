# OF-1 注册机制 — Pass 2 闭环 Q2: 扫描面

> 核心: getScanner + getBasePackages | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: 扫描哪些包? 怎么找到 @FeignClient 接口?**

## 机制链 (已实证)

```
registerFeignClients 扫描路径 (L172-181):
├── clients 属性空 → 扫描:
│   ├── scanner = getScanner() (L379): new ClassPathScanningCandidateComponentProvider(false, environment)
│   │   ← 匿名子类 (候选组件提供者)
│   ├── scanner.setResourceLoader (L178)
│   ├── scanner.addIncludeFilter(new AnnotationTypeFilter(FeignClient.class)) (L179)
│   │   ← 类型过滤器: 只收 @FeignClient 注解类
│   └── basePackages = getBasePackages(metadata) (L180)
│       └── scanner.findCandidateComponents(basePackage) (L181)
│           ← 类路径扫描 + 候选组件

getBasePackages (L393+) — 四级兜底:
├── value() 包 (L)
├── basePackages() 包 (L)
├── basePackageClasses() → ClassUtils.getPackageName (L)
└── 全空 → ClassUtils.getPackageName(importingClassMetadata.getClassName()) (L)
    ← 兜底: @EnableFeignClients 所在类的包
```

## 关键设计 (why)

1. **ClassPathScanningCandidateComponentProvider**: Spring 标准扫描器 (与 @ComponentScan 同源) — 复用成熟机制
2. **AnnotationTypeFilter**: 类型过滤器只收 @FeignClient — 精确扫描
3. **四级包解析兜底**: value → basePackages → basePackageClasses → **启动类所在包** — 默认行为即"扫描启动类包"
4. **false 参数**: useDefaultFilters=false — 不用默认组件过滤器 (@Component 等), 只收 Feign 客户端

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| getScanner 匿名子类 | FeignClientsRegistrar.java:379 |
| AnnotationTypeFilter | FeignClientsRegistrar.java:179 |
| findCandidateComponents | FeignClientsRegistrar.java:181 |
| getBasePackages 四级兜底 | FeignClientsRegistrar.java:393+ |

## 负面空间 (Q2 面)

- 不做包外扫描 (只扫声明包)
- 不做接口 vs 类区分过滤 (注解标记为主)
- 不做扫描结果缓存 (每次启动全扫)
