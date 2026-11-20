# Triones Payment Aggregator · 聚合支付

为 Java / Kotlin 应用提供统一的支付渠道抽象，封装微信支付、支付宝的下单、关单、退款和商家转账操作，减少业务系统对不同渠道接口的适配工作。

项目使用 Kotlin 编写，面向 JVM 提供 JAR，可在 Java 项目中使用。渠道适配基于 Triones 的 `payment-wechatpay` 和 `payment-alipay` 基础库。

## 环境与版本

| 项目 | 当前配置 |
| --- | --- |
| JDK | 17 或以上 |
| 构建工具 | Maven |
| Kotlin | 2.1.21 |
| 项目版本 | `0.0.2-SNAPSHOT` |
| Maven groupId | `com.trionesdev.payment` |
| 源码根包 | `com.trionesdev.payment.aggregator` |

当前为开发快照版本，接口与实现仍在完善。下述能力以当前源码为准，不代表所有渠道都支持全部统一接口。

## 模块结构

```text
payment-aggregator                         父 POM，不生成 JAR
├── payment-aggregator-dependencies        BOM，统一管理依赖版本
└── payment-aggregators                    渠道模块聚合 POM，不生成 JAR
    ├── payment-aggregator-core            渠道抽象、公共模型、枚举与回调
    ├── payment-aggregator-wechatpay       微信支付适配
    └── payment-aggregator-alipay          支付宝适配
```

- [微信支付接入说明](payment-aggregators/payment-aggregator-wechatpay/readme.md)
- [支付宝接入说明](payment-aggregators/payment-aggregator-alipay/readme.md)

## 支持范围

### 下单场景

| 场景枚举 | 微信支付 | 支付宝 |
| --- | --- | --- |
| `WEB` | Native 支付，返回二维码链接 | 电脑网站支付 |
| `WAP` | H5 支付 | 未实现 |
| `JSAPI` | JSAPI 支付 | 未实现 |
| `APP` | APP 支付 | 未实现 |
| `MINI_PROGRAM` | 小程序 JSAPI 支付 | 未实现 |
| `ORDER_CODE` | Native 支付，返回二维码链接 | 订单码支付，返回二维码内容 |
| `PAYMENT_CODE` | 未实现 | 未实现 |
| `FACE_PAY` | 未实现 | 未实现 |

微信未实现的场景及空场景目前会返回 `raw = null`；支付宝对应分支使用 `TODO()`，调用会抛出 `NotImplementedError`。请在业务侧校验场景，不要仅凭枚举存在就判断能力已支持。

### 其他操作

| 能力 | 微信支付 | 支付宝 |
| --- | --- | --- |
| 关闭订单 | 已接入 | 已接入 |
| 申请退款 | 已接入 | 已接入，但当前未向底层传递退款金额 |
| 商家转账 | 已接入 | 已接入 |
| 取消转账 | 已接入 | 空实现，不会发起渠道请求 |
| 支付通知处理 | 已接入 | 已接入 |
| 退款通知处理 | 异步通知，仅成功时调用业务回调 | 退款请求返回后同步调用业务回调 |
| 转账通知处理 | 异步通知，处理成功、失败、取消状态 | 转账请求同步返回 `SUCCESS` 时调用业务回调 |

“已接入”表示源码中存在底层调用，实际可用性还取决于商户权限、渠道配置及联调结果。

## Maven 接入

在应用的 `pom.xml` 中添加仓库配置，并通过 BOM 管理版本。示例使用当前开发版本；仓库中是否已有该快照需要实际解析确认，也可以先从源码安装到本地。

```xml
<repositories>
    <repository>
        <id>trionesdev</id>
        <url>https://maven.cnb.cool/trionesdev/mvn/-/packages/</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.trionesdev.payment</groupId>
            <artifactId>payment-aggregator-dependencies</artifactId>
            <version>0.0.2-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- 按需选择渠道；需要同时接入时保留两个依赖 -->
    <dependency>
        <groupId>com.trionesdev.payment</groupId>
        <artifactId>payment-aggregator-wechatpay</artifactId>
    </dependency>
    <dependency>
        <groupId>com.trionesdev.payment</groupId>
        <artifactId>payment-aggregator-alipay</artifactId>
    </dependency>
</dependencies>
```

渠道模块依赖核心模块。仅需要公共接口与数据模型时，可单独引入 `payment-aggregator-core`。

## 快速开始

### 1. 创建渠道实例

先在应用中配置底层 `WechatPay` / `Alipay` 客户端，包括商户信息、应用信息、证书或密钥等，再传入对应渠道构造器。客户端配置由基础支付库负责，本项目不提供自动配置 Starter。

以下 Java 工厂方法接收已经配置好的客户端。回调参数可传 `null`，表示不执行本库的业务回调。

```java
import com.trionesdev.payment.aggregator.PaymentChannel;
import com.trionesdev.payment.aggregator.PaymentNotifyCallback;
import com.trionesdev.payment.aggregator.alipay.AlipayChannel;
import com.trionesdev.payment.aggregator.wechatpay.WechatPayChannel;
import com.trionesdev.payment.alipay.v3.Alipay;
import com.trionesdev.payment.wechatpay.v3.WechatPay;

public final class PaymentChannels {
    public static PaymentChannel wechat(WechatPay client, PaymentNotifyCallback callback) {
        return new WechatPayChannel(client, callback);
    }

    public static PaymentChannel alipay(Alipay client, PaymentNotifyCallback callback) {
        return new AlipayChannel(client, callback);
    }
}
```

虽然构造器允许客户端为 `null`，实际调用支付操作时必须传入可用客户端。

`PaymentChannel` 提供以下统一方法：

| 方法 | 请求模型 | 返回模型 |
| --- | --- | --- |
| `createOrder` | `CreateOrderRequest` | `CreateOrderResponse` |
| `closeOrder` | `CloseOrderRequest` | `CloseOrderResponse` |
| `createRefund` | `CreateRefundRequest` | `CreateRefundResponse` |
| `createTransfer` | `CreateTransferRequest` | `CreateTransferResponse` |
| `cancelTransfer` | `CancelTransferRequest` | `CancelTransferResponse` |

### 2. 发起微信 Native 支付

以下方法接收上一步创建的微信渠道，返回用于生成支付二维码的链接。

```java
import com.trionesdev.payment.aggregator.PaymentChannel;
import com.trionesdev.payment.aggregator.shared.enums.Channel;
import com.trionesdev.payment.aggregator.shared.enums.Currency;
import com.trionesdev.payment.aggregator.shared.enums.Scene;
import com.trionesdev.payment.aggregator.shared.model.CreateOrderRequest;
import com.trionesdev.payment.aggregator.shared.model.CreateOrderResponse;
import com.trionesdev.payment.aggregator.shared.model.Money;
import java.math.BigDecimal;

public final class NativePaymentExample {
    public static String createOrder(PaymentChannel wechatChannel) {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .channel(Channel.WECHAT_PAY.name())
                .scene(Scene.WEB)
                .appId("your-app-id")
                .merchantId("your-merchant-id")
                .outTradeNo("ORDER_20261120_0001")
                .subject("示例商品")
                .amount(new Money(new BigDecimal("0.01"), Currency.CNY))
                .notifyUrl("https://your-domain.example/payment/wechat/notify")
                .build();

        CreateOrderResponse response = wechatChannel.createOrder(request);
        if (response.getRaw() == null || response.getRaw().get("codeUrl") == null) {
            throw new IllegalStateException("未获取到支付二维码链接");
        }
        return (String) response.getRaw().get("codeUrl");
    }
}
```

应用应为每笔订单生成自己的 `outTradeNo`，并替换示例中的配置和回调地址。

### 3. 处理渠道差异

- **金额**：公共模型使用 `Money` 和 `BigDecimal`，人民币按元传入，例如 `0.01` 表示 1 分。微信适配层转换为整数分；请在业务侧校验金额精度和范围，当前转换使用 `toInt()`，不会主动拒绝超出分精度的金额。
- **下单结果**：通过 `CreateOrderResponse.raw` 获取渠道数据，具体键名见各渠道说明。下单成功不等于支付成功。
- **渠道选择**：`request.channel` 是渠道标识字段，当前没有自动路由器。应用需要自行选择 `WechatPayChannel` 或 `AlipayChannel` 实例。
- **组件标记**：`@PaymentComponent` 仅提供运行时渠道元数据，本身不会注册 Spring Bean 或自动扫描路由。
- **扩展参数**：`CreateOrderRequest.extra` 用于渠道差异参数；目前支付宝电脑网站支付读取 `qrPayMode`、`returnUrl`，值均应为字符串。

## 通知回调

继承 `PaymentNotifyCallback`，实现三个业务处理方法，并在创建渠道时传入：

| 方法 | 参数 | 用途 |
| --- | --- | --- |
| `transactionNotifyProcess` | `TransactionNotifyArgs` | 支付通知 |
| `refundNotifyProcess` | `RefundNotifyArgs` | 退款通知 |
| `transferNotifyProcess` | `TransferNotifyArgs` | 转账通知 |

模型位于 `com.trionesdev.payment.aggregator.shared.model`，包含业务单号、渠道单号、金额或状态，以及 `raw` 渠道数据，具体字段以对应模型为准。

应用自行提供 HTTP 通知入口，并将通知交给具体渠道：

- 微信：构造底层库的 `WechatPayNotifyParseRequest`，分别调用 `WechatPayChannel.transactionNotify`、`refundNotify`、`transferNotify`。
- 支付宝：将表单参数作为 `MutableMap<String, Array<String>>`（Java 对应 `Map<String, String[]>`）传入 `AlipayChannel.transactionNotify`。

当前聚合层将通知解析委托给底层客户端，不提供 Controller，也不负责生成渠道要求的 HTTP 应答。通知入口需按基础库要求传递验签、解密所需的请求信息，并由应用处理应答、业务幂等和订单状态更新。支付通知还应结合渠道交易状态和本地订单金额进行业务判断。

## 从源码构建

在项目根目录执行：

```bash
mvn clean install
```

构建需要能够解析仓库中的基础支付库及其 BOM `com.trionesdev.payment:payment-dependencies:0.0.2-SNAPSHOT`。生成的渠道 JAR 位于各渠道模块的 `target/` 目录。

### 当前源码注意事项

当前 `CloseOrderRequest.Builder.outTradeNo()` 错误地赋值给了 `merchantId`；调用关单接口时可先使用 `new CloseOrderRequest()` 和属性 setter 设置字段。`CancelTransferRequest.Builder` 尚未提供 `outBillNo()`，微信取消转账时需通过 setter 设置商户转账单号。

## 扩展支付渠道

1. 新建渠道模块并依赖 `payment-aggregator-core`。
2. 继承 `PaymentChannel`，实现公共请求、响应与底层渠道模型之间的转换。
3. 使用 `@PaymentComponent(channel = "YOUR_CHANNEL")` 标记渠道，按需适配通知回调。
4. 在聚合 POM 和 BOM 中登记新模块，并由应用配置实例及渠道选择逻辑。

## 关注我们，一起交流

留言回复不及时，可以通过关注公众号联系我们。

<div style="text-align: center">
    <img src="images/shuque_wx.jpg" width="200" alt="TrionesDev 交流公众号二维码">
</div>
