# OF-4 编解码 — 时空溯源 (代码内注释锚)

## 时期线

| 时期 | 机制演变 |
|:--|:--|
| 2.0 (早期) | SpringEncoder/SpringDecoder 骨架: HttpMessageConverter 编解码 + FeignResponseAdapter |
| 2.1+ | ResponseEntityDecoder (ResponseEntity 支持); FeignErrorDecoderFactory |
| 3.x | PageableSpringEncoder (组合模式) + PageJacksonModule (SimplePageImpl) — Spring Data 分页; FeignEncoderProperties (charsetFromContentType); HttpMessageConverterCustomizer |
| 4.x | 分页族完善 (PageableSpringQueryMapEncoder/SortJacksonModule); DecodeException 类型白名单 |

## 痕迹证据

- SpringEncoder.java:75-81: ObjectFactory<HttpMessageConverters> + Customizer (2.x/3.x 锚)
- SpringEncoder.java:120-138: encodeWithMessageConverter + EncodeException "Error converting request body" (2.x 锚)
- SpringDecoder.java:64-70: decode + HttpMessageConverterExtractor (2.x 锚)
- SpringDecoder.java:84+: FeignResponseAdapter 内部类 (2.x 锚)
- ResponseEntityDecoder.java:41-88: 三分支 + createResponse (2.1+ 锚)
- PageableSpringEncoder.java:38-110: 组合模式 + page/size query (3.x 锚)
- PageJacksonModule.java:76,88: @JsonDeserialize(SimplePageImpl) (3.x 锚)

## 推断标注

- "2.x 骨架" — Spring Cloud OpenFeign 公知版本线 (标注)
- "2.1+ ResponseEntity" — 类实证 (实证)
- "3.x 分页" — PageableSpringEncoder/PageJacksonModule 类实证 (实证)
- **源码浅克隆 (单 commit)**: git log 时空考古受限 — 以注释锚 + 常量实证为主 (降级说明)

## 对照线 (已交付/待交付)

- Feign 本体 F-4 Codec: Encoder/Decoder 接口 + DefaultErrorDecoder — 底座对照
- Spring MVC HttpMessageConverter: 服务端同款转换器 — 生态对照
- 分页: Spring Data Page/Pageable vs 自定义 — 语义对照
