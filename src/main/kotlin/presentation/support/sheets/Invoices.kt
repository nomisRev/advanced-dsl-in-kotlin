package presentation.support.sheets

import presentation.support.Invoice
import presentation.support.contexts.times

/*
 * The invoice sheet the extra lesson writes to files, HTTP and workbooks.
 * A plain immutable value: sharing it needs no global builder.
 */

val invoiceSheet: Sheet<Invoice> = sheet {
  val customer by column { it.customer }
  val hours by column { it.hours }
  val rate by column { it.rate }
  val total by formula { hours * rate }
}
