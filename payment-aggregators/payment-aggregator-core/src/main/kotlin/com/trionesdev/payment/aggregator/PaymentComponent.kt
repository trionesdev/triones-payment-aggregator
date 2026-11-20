package com.trionesdev.payment.aggregator

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class PaymentComponent(val channel: String)