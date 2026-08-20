# OF-4 编解码 — completeness-questions (全视角提问验证)

## 开发者视角

1. 请求体怎么编码? (HttpMessageConverter)
2. 响应怎么解码? (HttpMessageConverterExtractor)
3. FeignResponseAdapter? (桥接)
4. ResponseEntity 返回? (装饰器)
5. 分页请求? (page/size query)
6. 分页响应? (SimplePageImpl)
7. 错误解码? (FeignErrorDecoderFactory)
8. Sort 参数? (sortParameter)

## 架构师视角

9. 为什么复用 HttpMessageConverter? (生态一致)
10. 为什么 Generic 分支? (泛型请求体)
11. 为什么适配器? (双生态桥接)
12. 为什么类型白名单? (防静默失败)
13. 为什么装饰器透传? (组合非侵入)
14. 为什么组合模式? (Pageable 只处理分页)
15. 为什么 SimplePageImpl? (接口不能反序列化)
16. 为什么 headers 回流? (转换器决策尊重)

## 学生视角

17. 什么是编码/解码? (请求/响应转换)
18. 什么是适配器? (接口桥接)
19. 什么是装饰器? (委托包装)
20. 什么是 Pageable? (分页参数)
