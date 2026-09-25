package presentation.support.flow

import java.io.File
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import presentation.support.Invoice
import presentation.support.invoices

/**
 * The invoices as a database driver would return them: each row arrives
 * after a round trip, and nothing is fetched until someone collects.
 */
fun fetchInvoices(): Flow<Invoice> = flow {
  for (invoice in invoices) {
    delay(100.milliseconds)
    emit(invoice)
  }
}

/** A remote lookup per customer, as a CRM behind HTTP would answer it. */
suspend fun fetchEmail(customer: String): String {
  delay(10.milliseconds)
  return "${customer.lowercase()}@example.com"
}

/** The files SXSSF flushes rows to, left behind unless the workbook is disposed. */
fun sxssfTempFiles(): List<String> =
  File(System.getProperty("java.io.tmpdir"), "poifiles")
    .listFiles { file -> file.name.startsWith("poi-sxssf-sheet") }
    ?.map { it.name }
    .orEmpty()
