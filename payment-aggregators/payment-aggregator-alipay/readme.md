# 支付宝适配

模块：`com.trionesdev.payment:payment-aggregator-alipay`，当前版本：`0.0.2-SNAPSHOT`。

提供 `com.trionesdev.payment.aggregator.alipay.AlipayChannel`，基于底层 `com.trionesdev.payment.alipay.v3.Alipay` 客户端适配统一支付接口。

仓库配置、BOM、公共模型和构建注意事项见[项目 README](../../readme.md)。

## 初始化

先配置基础库的 `Alipay` 客户端，再调用构造器 `AlipayChannel(alipay, aggregatedPaymentNotify)`。第二个参数是业务回调 `PaymentNotifyCallback`，可传 `null`。

应用负责客户端配置、实例管理及渠道选择；本模块不提供自动配置 Starter。

## 下单场景与返回值

通过 `CreateOrderResponse.raw` 获取下列字段：

| `Scene` | 渠道接口 | 返回字段 |
| --- | --- | --- |
| `WEB` | 电脑网站支付 | `pageRedirectionData` |
| `ORDER_CODE` | 订单码支付 | `qrCode`、`outTradeNo`、`shareCode` |

其他场景及空场景当前均为 `TODO()`，调用会抛出 `NotImplementedError`。

公共下单字段包括 `outTradeNo`、`subject`、`amount`、`goodsDetail`。金额按元传入 `Money`，转换后作为十进制字符串传给支付宝。

电脑网站支付还映射 `notifyUrl`，并读取 `extra` 中的字符串参数 `qrPayMode` 和 `returnUrl`。订单码支付当前仅映射商户订单号、金额、标题和商品列表，未传递 `notifyUrl`。

## 其他操作与限制

| 方法 | 当前行为 |
| --- | --- |
| `closeOrder` | 传递 `outTradeNo`、`tradeNo`、`operatorId`，返回空的 `CloseOrderResponse` |
| `createRefund` | 传递订单号、退款原因和商户退款单号；未传递 `refundAmount`，使用前需完善并联调 |
| `createTransfer` | 传递商户转账单号、金额、场景、标题、收款人、备注和业务扩展参数 |
| `cancelTransfer` | 空实现，直接返回空响应，不会向支付宝发起请求 |

退款返回模型当前只设置 `refundNo`（值来自请求的 `outRefundNo`）及 `refundAmount`，不要将该 `refundNo` 当作已经获取的渠道退款单号。

## 通知与回调

- **支付通知**：将 HTTP 表单参数以 `MutableMap<String, Array<String>>`（Java 对应 `Map<String, String[]>`）传入 `transactionNotify`，适配层调用底层 `notifyParseFromMaps` 后触发 `transactionNotifyProcess`。
- **退款回调**：`createRefund` 返回后同步触发 `refundNotifyProcess`，当前没有单独的退款异步通知入口。
- **转账回调**：`createTransfer` 同步返回状态为 `SUCCESS` 时触发 `transferNotifyProcess`，当前没有单独的转账异步通知入口。

支付通知适配层没有额外筛选交易状态，业务方应结合 `TransactionNotifyArgs.raw` 中的渠道数据进行判断。HTTP 通知入口、渠道应答和业务幂等由应用实现。
