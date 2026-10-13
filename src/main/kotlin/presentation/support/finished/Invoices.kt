package presentation.support.finished

import excel.dsl.sheet
import excel.dsl.times
import presentation.support.Invoice

/** The AST-based schema from the talk's finale, reused by the file-writing slide. */
val invoiceSheet = sheet<Invoice> {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val gross by formula { hours * rate * 1.21 }
}
