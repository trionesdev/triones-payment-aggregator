package com.trionesdev.payment.aggregator

import com.trionesdev.payment.aggregator.shared.model.RefundNotifyArgs
import com.trionesdev.payment.aggregator.shared.model.TransactionNotifyArgs
import com.trionesdev.payment.aggregator.shared.model.TransferNotifyArgs

/**
 * 支付回调处理
 */
abstract class PaymentNotifyCallback {
    /**
     * 支付回调
     */
    abstract fun transactionNotifyProcess(args: TransactionNotifyArgs)

    /**
     * 退款回调
     */
    abstract fun refundNotifyProcess(args: RefundNotifyArgs)

    /**
     * 商家转账回调
     */
    abstract fun transferNotifyProcess(args: TransferNotifyArgs)
}
