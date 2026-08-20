# SW-1 Agent Command Model — 07 全量维度审查

## 审查轮次: 第一轮 (2026-08-17, Pass0/1/2/3 + harness)
- [x] `apm-network/src/main/proto` submodule 初始缺失已识别并初始化
- [x] `apm-network` compile 成功，26 个 proto 正常生成 protobuf/grpc Java
- [x] 16 个 main Java command model + 1 个 test Java 数字核对
- [x] 4 个 deserializable command 与 4 个 serialize-only command 边界核对
- [x] CommandService/receiver 消费方 grep 定位完成
- [x] `MiniSW1` 9/9 PASS

## 审查轮次: 第二轮 (2026-08-18, 协议边界/生成代码/残留清零)
- [x] `apm-network` 当前 scope 修正为“通用采集协议 + command envelope”，未夸大为全仓协议总域
- [x] 全仓 53 个 proto 的归属策略已明确：`apm-protocol` 26，其余回到 OAP 消费域
- [x] generated Java 作为协议产物验证，不手工冒充业务域源码
- [x] `CommandDeserializer` 4 项分派不是遗漏，而是反向解析职责边界
- [x] 交付物补齐：`pass0` / `pass2` / `outline` / `review-notes` / harness

## 收敛判定
SW-1 当前无已知问题；已完成可交接。