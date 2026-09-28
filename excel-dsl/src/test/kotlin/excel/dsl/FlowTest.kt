package excel.dsl

import java.io.ByteArrayOutputStream
import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.io.TempDir

class FlowTest {
  @TempDir
  lateinit var dir: Path

  @Test
  fun `a flow writes the same cells as a sequence`() = runBlocking {
    val output = ByteArrayOutputStream()
    invoiceSheet.write(output, invoices.asFlow())
    assertEquals(invoiceSheet.writeToBytes().read().cells(), output.toByteArray().read().cells())
    assertEquals("Sheet1", output.toByteArray().read().getSheetName(0))
  }

  @Test
  fun `a flow to a path names the sheet after the file`() = runBlocking {
    val file = dir.resolve("streamed.xlsx")
    invoiceSheet.write(file, invoices.asFlow())
    assertEquals("streamed", file.read().getSheetName(0))
  }

  @Test
  fun `the next row is requested only after the previous one is written`() = runBlocking {
    val events = mutableListOf<String>()
    val sheet = sheet<Invoice> {
      val customer by column { events += "write ${it.customer}"; it.customer }
    }
    val rows = invoices.asFlow().onEach { events += "emit ${it.customer}" }
    sheet.write(ByteArrayOutputStream(), rows)
    assertEquals(
      listOf("emit Ada", "write Ada", "emit Linus", "write Linus", "emit Grace", "write Grace"),
      events,
    )
  }

  @Test
  fun `rows are collected and written on the caller's dispatcher`() = runBlocking {
    val threads = mutableSetOf<String>()
    val sheet = sheet<Invoice> {
      val customer by column { threads += Thread.currentThread().name; it.customer }
    }
    val executor = Executors.newSingleThreadExecutor { Thread(it, "excel-writer") }
    executor.asCoroutineDispatcher().use { dispatcher ->
      withContext(dispatcher) {
        sheet.write(ByteArrayOutputStream(), flow {
          threads += Thread.currentThread().name
          emit(invoices[0])
        })
      }
    }
    // Coroutine debug mode appends " @coroutine#N" to the thread name.
    assertEquals(setOf("excel-writer"), threads.map { it.substringBefore(" @") }.toSet())
  }

  @Test
  fun `an upstream failure propagates and leaves no temp files`() = runBlocking {
    val before = sxssfTempFiles()
    val rows = flow {
      emit(invoices[0])
      error("database down")
    }
    val error = assertFailsWith<IllegalStateException> { invoiceSheet.write(ByteArrayOutputStream(), rows) }
    assertEquals("database down", error.message)
    assertEquals(before, sxssfTempFiles())
  }

  @Test
  fun `cancellation stops collecting and leaves no temp files`() = runBlocking {
    val before = sxssfTempFiles()
    val started = CompletableDeferred<Unit>()
    val job = launch {
      invoiceSheet.write(ByteArrayOutputStream(), flow {
        emit(invoices[0])
        started.complete(Unit)
        awaitCancellation()
      })
    }
    started.await()
    job.cancelAndJoin()
    assertTrue(job.isCancelled)
    assertEquals(before, sxssfTempFiles())
  }

  @Test
  fun `a flow writes the totals and the filter too`() = runBlocking {
    val sheet = sheet<Invoice> {
      autoFilter = true
      val hours by column { it.hours }
      totals { hours.sum() }
    }
    val output = ByteArrayOutputStream()
    sheet.write(output, invoices.asFlow())
    val workbook = output.toByteArray().read()
    assertEquals(listOf("=SUM(A2:A4)"), workbook.cells().last())
    assertEquals("A1:A4", workbook.getSheetAt(0).ctWorksheet.autoFilter.ref)
  }

  @Test
  fun `an invalid sheet name fails`() = runBlocking<Unit> {
    assertFailsWith<IllegalArgumentException> {
      invoiceSheet.write(ByteArrayOutputStream(), invoices.asFlow(), "a/b")
    }
  }
}
