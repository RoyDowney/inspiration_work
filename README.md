# inspiration_work

Java 21 / Maven 多模块项目：

| 模块 | 用途 | 默认端口 | 路径前缀 |
| --- | --- | --- | --- |
| `yltq-gateway` | Spring Cloud Gateway 统一入口 | 8081 | 保留服务原路径 |
| `yltq-admin` | 后台服务（Excel 报表解析、可选 AI 集成） | 8080 | `/adminservice` |
| `yltq-provider-payment` | 支付服务（支付类型接口） | 8088 | `/paymentservice` |

`yltq-security` 提供共享 JWT 角色映射与校验规则；`yltq-common` 和 `yltq-common-core` 是共享代码模块，不独立启动。

## 身份与权限

项目不签发令牌，需接入现有 OAuth2/OIDC 身份系统。网关和两个子服务**各自校验签名、签发方、有效期和受众**，并根据 JWT 中的角色授权。三个服务均需配置相同的环境变量（不要将真实令牌、私钥或密码提交到仓库）：

| 环境变量 | 说明 |
| --- | --- |
| `SECURITY_ISSUER_URI` | JWT 的 `iss`，如身份系统的 issuer 地址 |
| `SECURITY_JWKS_URI` | 身份系统的 JWKS 地址，用于验证 JWT 签名 |
| `SECURITY_AUDIENCE` | JWT `aud` 必须包含的服务受众 |
| `SECURITY_ROLES_CLAIM` | 角色字段，默认 `roles`（字符串列表或以逗号/空格分隔的字符串） |
| `SECURITY_UPLOAD_ROLE` | Excel 上传角色，默认 `ADMIN_UPLOAD` |
| `SECURITY_PAYMENT_DELETE_ROLE` | 支付删除角色，默认 `PAYMENT_DELETE` |

上述三个身份系统地址/受众变量缺失时服务启动失败，不会退回匿名访问。JWT 需使用 JWKS 可验证的签名；生产环境使用 HTTPS 的 issuer/JWKS，并限制 admin、payment 的网络入口，仅允许受控网络或网关访问。网关与子服务都必须使用一致的角色配置。

网关授权规则：Excel 上传需要 `ADMIN_UPLOAD`，支付删除需要 `PAYMENT_DELETE`，支付查询需要登录；未列出的路由默认拒绝。子服务独立执行相同的权限检查，不能通过直连跳过网关。此前的 `ADMIN_UPLOAD_TOKEN`、`PAYMENT_DELETE_TOKEN` 临时令牌已移除，不再生效；`Authorization: Bearer <JWT>` 是请求使用的身份凭据。

## 构建与运行

```bash
mvn clean package
java -jar yltq-admin/target/yltq-admin-1.0-SNAPSHOT.jar
java -jar yltq-provider-payment/target/yltq-provider-payment-1.0-SNAPSHOT.jar
java -jar yltq-gateway/target/yltq-gateway-1.0-SNAPSHOT.jar
```

网关通过 `ADMIN_SERVICE_URL`、`PAYMENT_SERVICE_URL` 配置子服务地址，默认分别为 `http://localhost:8080`、`http://localhost:8088`。网关保留原始路径和 `Authorization` 头，例如：

- `POST /adminservice/execl/operate/reportImportExcelAnalysis`：Excel 上传。
- `GET /paymentservice/payment/listPage/{pageNum}/{pageSize}`、`GET /paymentservice/payment/findById/{payId}`：支付类型查询。
- `DELETE /paymentservice/payment/removeById/{payId}`：支付类型删除；旧 GET 删除接口已移除。

支付服务默认使用 `pay-dev`，需提供 `PAYMENT_DB_URL`、`PAYMENT_DB_USERNAME`、`PAYMENT_DB_PASSWORD`。部署其他环境时显式设置 `SPRING_PROFILES_ACTIVE`；不要沿用开发环境配置。后台服务默认不启用通义千问或 MiniMax，启用时另行提供各模型配置和 API Key。

本地测试：`mvn clean test`。按项目约定，`src/test` 不纳入 Git。未提供真实身份系统参数或数据库时，可运行本地模拟 JWT 与内存数据库测试；实际环境仍需联调。
