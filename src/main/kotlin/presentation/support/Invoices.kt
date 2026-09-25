package presentation.support

import java.time.LocalDate

data class Invoice(
  val customer: String,
  val hours: Int,
  val rate: Int,
  val issuedOn: LocalDate,
)

/** The rows every `generateExcel("invoices.xlsx", invoices)` on the slides writes. */
val invoices: Sequence<Invoice> = sequenceOf(
  Invoice("Ada", 12, 90, LocalDate.of(2026, 9, 1)),
  Invoice("Linus", 8, 80, LocalDate.of(2026, 2, 12)),
  Invoice("Grace", 20, 110, LocalDate.of(2026, 3, 30)),
  Invoice("Olivia", 6, 95, LocalDate.of(2026, 5, 4)),
  Invoice("Linus", 14, 80, LocalDate.of(2026, 6, 18)),
)

val q1: Sequence<Invoice> = invoices.inQuarter(1)
val q2: Sequence<Invoice> = invoices.inQuarter(2)

private fun Sequence<Invoice>.inQuarter(quarter: Int): Sequence<Invoice> =
  filter { (it.issuedOn.monthValue - 1) / 3 + 1 == quarter }

/** Part 6 sums regular and overtime hours, which an [Invoice] does not track. */
data class Timesheet(
  val customer: String,
  val hours: Int,
  val overtime: Int,
)

val timesheets: Sequence<Timesheet> = sequenceOf(
  Timesheet("Ada", 12, 3),
  Timesheet("Grace", 20, 0),
  Timesheet("Linus", 14, 2),
)
