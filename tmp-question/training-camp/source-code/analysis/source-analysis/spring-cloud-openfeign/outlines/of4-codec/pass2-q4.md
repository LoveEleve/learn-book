# OF-4 编解码 — Pass 2 闭环 Q4: 分页面 (Pageable 编码 + Page 反序列化)

> 核心: PageableSpringEncoder + PageJacksonModule | 锚点已 grep 验证 (2026-08-16)

## 闭环问题

**Q: Spring Data 分页请求/响应怎么编解码?**

## 机制链 (已实证)

```
请求面 — PageableSpringEncoder (L38-110, 组合模式):
├── encode: supports(object) → **Pageable 特判** (L87-99):
│   ├── isPaged → template.query(pageParameter, pageNumber) + query(sizeParameter, pageSize)
│   └── Sort → applySort (sortParameter 可配 L80)
├── 不支持 → **delegate.encode** (组合 fallback)
└── PageableSpringQueryMapEncoder: QueryMap 面 (OF-3 queryMapIndex 消费)

响应面 — PageJacksonModule (L39-88):
├── 注册: @JsonDeserialize(as = **SimplePageImpl.class**) (L76)
├── SimplePageImpl<T> implements Page<T> (L88):
│   ├── @JsonProperty("content") List<T> + @JsonProperty("pageable") Pageable
│   └── 构造 → PageImpl 实现
└── SortJacksonModule + SortJsonComponent: Sort 参数序列化
```

## 关键设计 (why)

1. **组合模式**: PageableSpringEncoder 委托 delegate — 只处理 Pageable, 其余透传
2. **请求分页**: Pageable → page/size/sort query 参数 (可配参数名) — 服务端 Spring Data 直接消费
3. **响应分页**: PageJacksonModule 注册 Jackson 反序列化 — **Page 接口无法直接反序列化, 用 SimplePageImpl 实现类** (经典技巧)
4. **与 OF-3 联动**: Contract 的 queryMapIndex (Pageable 特判) → QueryMapEncoder 面

## 锚点清单

| 锚点 | 位置 |
|:--|--|
| Pageable → page/size query | PageableSpringEncoder.java:87-99 |
| applySort + sortParameter | PageableSpringEncoder.java:80 |
| delegate fallback | PageableSpringEncoder.java:38 |
| @JsonDeserialize SimplePageImpl | PageJacksonModule.java:76,88 |

## 负面空间 (Q4 面)

- 不 Pageable 嵌套 (单层)
- 不自定义 Page 实现 (SimplePageImpl 固定)
- 不做分页协议协商 (page/size 参数名固定可配)
