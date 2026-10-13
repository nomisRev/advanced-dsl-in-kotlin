package presentation.support.custom

import presentation.support.CustomType
import presentation.support.Invoice
import presentation.support.typed.SheetBuilder
import presentation.support.typed.column

/** Imported as `column` only on the deliberate overload-resolution failure slide. */
fun SheetBuilder<Invoice>.customColumn(
  name: String,
  value: (Invoice) -> CustomType,
) = column(name) { value(it).toColumnText() }
