# OF-8 HTTP 客户端与压缩 — completeness-questions (全视角提问验证)

## 开发者视角

1. 底层客户端怎么选? (HttpClient5/Http2/OkHttp)
2. 连接池怎么配? (Pooling)
3. SSL 校验? (disableSslValidation)
4. Http2 重定向? (ALWAYS/NEVER)
5. 请求什么时候压缩? (双条件)
6. 阈值? (minRequestSize=2048)
7. MIME? (3 个默认)
8. 响应压缩? (Accept-Encoding)

## 架构师视角

9. 为什么三选型? (场景选择)
10. 为什么连接池显式? (容量控制)
11. 为什么 SSL 可关? (内网)
12. 为什么双条件压缩? (收益权衡)
13. 为什么 OkHttp 特判? (自带解压)
14. 为什么开关驱动? (可关闭)
15. 为什么默认值明确? (开箱即用)
16. 为什么与 OF-5 装饰链? (职责分离)

## 学生视角

17. 什么是连接池? (复用连接)
18. 什么是 Gzip? (压缩格式)
19. 什么是 Accept-Encoding? (响应协商)
20. 什么是 HTTP/2? (多路复用)
