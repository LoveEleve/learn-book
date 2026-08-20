# 闭环笔记 q5: 认证一句话 — SignAuthentication + ACL 迁移

## 假设
5.x 认证 = 签名验证 (认证) + 资源权限 (授权) 两段; acl 旧体系可迁移。

## 验证过程
- **管线执行序** (q2 已证): ContextInit → **AuthenticationPipeline** → **AuthorizationPipeline** (GrpcMessagingApplication:153-155)
- **AuthenticationPipeline** (grpc/pipeline): execute → AuthenticationEvaluator → DefaultAuthenticationContext → **username 写入 GrpcConstants.AUTHORIZATION_AK header** (L74-76) — 认证结果透传后续面
- **auth 模块** (main 5025 行):
  - **DefaultAuthenticationHandler** (chain): `AclSigner.calSignature(context.getContent(), user.getPassword())` 比对 context.getSignature() (L64-66) — **签名验证核心**
  - **签名算法**: acl/common/AclSigner:30 — **DEFAULT_ALGORITHM = SigningAlgorithm.HmacSHA1** (AWS 风格); calSignature(data, key)
  - **策略双实现**: StatefulAuthenticationStrategy (会话) / StatelessAuthenticationStrategy (每请求签名)
  - **元数据**: AuthenticationMetadataProvider/LocalAuthenticationMetadataProvider; manager: AuthenticationMetadataManagerImpl (222)
  - **授权面**: authorization/ (27 文件) — DefaultAuthorizationContextBuilder (490) + AuthorizationMetadataManagerImpl (284) + LocalAuthorizationMetadataProvider (199) + Resource 模型 (168)
- **迁移**: AuthMigrator (migration, 229 行) — **acl 旧配置 → auth 新体系平滑迁移**
- **broker 侧同构**: RM-5 已见 AuthorizationPipeline/AuthenticationPipeline (broker 也挂链)

## 代码类型
Implementation (认证链 + 迁移)

## 跨域关联
- RM-5 (Broker): 认证管线双面 (proxy+broker) 同构
- RM-1 (协议): RPCHook 认证挂点 (老式) vs 管线 (5.x) 并存

## 结论
认证一句话: **HmacSHA1 签名 (无状态认证) + 资源表授权 (有状态) 两段式**; acl 经 AuthMigrator 迁移; authConfig 驱动 (默认关)。
源码位置: AuthenticationPipeline.java:47-76; DefaultAuthenticationHandler.java:64-66; AclSigner.java:28-39; AuthMigrator.java
