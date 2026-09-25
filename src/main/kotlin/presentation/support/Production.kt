package presentation.support

import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneOffset

/*
 * Extra "A realistic library": what a real invoice carries beyond Part 1's
 * four fields, as extensions so the earlier slides keep their `Invoice`.
 */

enum class Status { Paid, Open }

/** Invoices before June are paid. */
val Invoice.status: Status
  get() = if (issuedOn.monthValue < 6) Status.Paid else Status.Open

val Invoice.paid: Boolean
  get() = status == Status.Paid

/** Money as money: `BigDecimal`, two decimals. */
val Invoice.amount: BigDecimal
  get() = (hours * rate).toBigDecimal().setScale(2)

/** When the invoice was mailed, 09:30 UTC on the day it was issued. */
val Invoice.sentAt: Instant
  get() = issuedOn.atTime(9, 30).toInstant(ZoneOffset.UTC)

/** A reference from the billing system, 16 digits: one more than Excel keeps. */
val Invoice.billingId: Long
  get() = 2026_0000_0000_0000L + issuedOn.dayOfYear

/** Cents in a type of our own, which no `column` overload accepts. */
@JvmInline
value class Money(val cents: Long)

val Invoice.price: Money
  get() = Money(hours * rate * 100L)
