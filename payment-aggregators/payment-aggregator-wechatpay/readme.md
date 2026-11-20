# 微信支付适配

模块：`com.trionesdev.payment:payment-aggregator-wechatpay`，当前版本：`0.0.2-SNAPSHOT`。

提供 `com.trionesdev.payment.aggregator.wechatpay.WechatPayChannel`，基于底层 `com.trionesdev.payment.wechatpay.v3.WechatPay` 客户端适配统一支付接口。

仓库配置、BOM、Java 下单示例及构建注意事项见[项目 README](../../readme.md)。

## 初始化

先配置基础库的 `WechatPay` 客户端，再调用构造器 `WechatPayChannel(wechatpay, aggregatedPaymentNotify)`。第二个参数是业务回调 `PaymentNotifyCallback`，可传 `null`。

应用负责客户端配置、实例管理及渠道选择；本模块不提供自动配置 Starter。

## 下单场景与返回值

通过 `CreateOrderResponse.raw` 获取下列字段：

| `Scene` | 渠道接口 | 返回字段 |
| --- | --- | --- |
| `WAP` | H5 | `h5Url` |
| `WEB`、`ORDER_CODE` | Native | `codeUrl` |
| `APP` | APP | `appId`、`timeStamp`、`nonceStr`、`packageStr`、`paySign`、`signType` |
| `JSAPI`、`MINI_PROGRAM` | JSAPI | `appId`、`timeStamp`、`nonceStr`、`packageStr`、`paySign`、`signType` |

`PAYMENT_CODE`、`FACE_PAY` 及空场景尚未实现，当前返回 `raw = null`。

常用请求字段为 `appId`、`merchantId`、`outTradeNo`、`subject`、`amount`、`notifyUrl`。JSAPI / 小程序支付通过 `payer.identity` 传递用户 OpenID。具体场景的字段要求与商户权限需要结合底层客户端及实际渠道校验；当前 H5 转换只映射了通用订单字段。

金额按元传入 `Money`，适配层乘以 100 转为整数分。请在业务侧校验精度及金额范围。

## 其他操作

- `closeOrder`：使用 `merchantId`、`outTradeNo` 关闭订单。
- `createRefund`：映射渠道或商户订单号、`outRefundNo`、`notifyUrl`、退款金额和订单总金额；当前转换未传递 `reason`。
- `createTransfer`：映射应用 ID、商户转账单号、场景 ID、收款人、金额和场景报备信息等；通过 `CreateTransferResponse.extra["packageInfo"]` 获取当前适配层保留的渠道扩展数据。
- `cancelTransfer`：使用 `outBillNo` 发起取消转账。

关单和取消转账请求的 Builder 限制见主 README，可使用 setter 设置字段。

## 异步通知

应用接收渠道 HTTP 通知后，按照基础库要求构造 `WechatPayNotifyParseRequest`，调用具体渠道实例：

| 方法 | 业务回调触发条件 |
| --- | --- |
| `transactionNotify` | 底层解析返回后调用 `transactionNotifyProcess` |
| `refundNotify` | 退款状态为 `SUCCESS` 时调用 `refundNotifyProcess` |
| `transferNotify` | 转账状态为 `SUCCESS`、`FAIL`、`CANCELLED` 时调用 `transferNotifyProcess` |

转账取消状态在公共枚举中表示为 `TransferStatus.CANCELED`。HTTP 通知入口、渠道应答和业务幂等由应用实现。
