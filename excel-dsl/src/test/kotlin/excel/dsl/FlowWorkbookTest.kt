package excel.dsl

import java.io.ByteArrayOutputStream
import java.io.FilterOutputStream
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.io.TempDir

class FlowWorkbookTest {
  @TempDir lateinit var dir: Path

  @Test
  fun `different row types and sources collect sequentially once per write`() = runBlocking {
    val events = mutableListOf<String>()
    val numbers = sheet<Int> { val n by column { events += "write $it"; it }; totals { n.sum() } }
    val strings = sheet<String> { val name by column { it } }
    val rows = flow { events += "collect"; emit(1); emit(2) }
    val report = flowWorkbook {
      sheet("Flow", numbers, rows)
      sheet("List", strings, listOf("Ada"))
      sheet("Sequence", numbers, sequenceOf(3))
    }
    assertEquals(emptyList(), events)
    repeat(2) {
      val output = ByteArrayOutputStream()
      report.write(output)
      output.toByteArray().read().use { workbook ->
        assertEquals(listOf("Flow", "List", "Sequence"), report.names)
        assertEquals(listOf(listOf("Name"), listOf("Ada")), workbook.cells(1))
        assertEquals("SUM(A2:A3)", workbook.getSheetAt(0).getRow(3).getCell(0).cellFormula)
      }
    }
    assertEquals(List(2) { listOf("collect", "write 1", "write 2", "write 3") }.flatten(), events)
  }

  @Test
  fun `flow workbook writes paths and leaves caller streams open`() = runBlocking {
    val report = flowWorkbook { sheet("Invoices", invoiceSheet, invoices.asFlow()) }
    val file = dir.resolve("report.xlsx")
    report.write(file)
    file.read().use { assertEquals("Invoices", it.getSheetName(0)) }
    var closed = false
    val output = object : FilterOutputStream(ByteArrayOutputStream()) {
      override fun close() { closed = true }
    }
    report.write(output)
    assertFalse(closed)
    val single = dir.resolve("a-very-long-name-that-cannot-be-used-for-a-sheet.xlsx")
    invoiceSheet.write(single, invoices.asFlow(), "Short")
    single.read().use { assertEquals("Short", it.getSheetName(0)) }
  }

  @Test
  fun `failure on a later sheet closes temporary files`() = runBlocking {
    val before = sxssfTempFiles()
    val report = flowWorkbook {
      sheet("First", invoiceSheet, invoices)
      sheet("Second", invoiceSheet, flow { emit(invoices[0]); error("upstream failed") })
    }
    val failure = assertFailsWith<IllegalStateException> { report.write(ByteArrayOutputStream()) }
    assertEquals("upstream failed", failure.message)
    assertEquals(before, sxssfTempFiles())
  }

  @Test
  fun `cancellation closes all sheets and does not collect the next source`() = runBlocking {
    val before = sxssfTempFiles()
    val started = CompletableDeferred<Unit>()
    var nextCollected = false
    val report = flowWorkbook {
      sheet("First", invoiceSheet, invoices)
      sheet("Second", invoiceSheet, flow {
        emit(invoices[0]); started.complete(Unit); awaitCancellation()
      })
      sheet("Third", invoiceSheet, flow { nextCollected = true; emit(invoices[0]) })
    }
    val job = launch { report.write(ByteArrayOutputStream()) }
    started.await()
    job.cancelAndJoin()
    assertTrue(job.isCancelled)
    assertFalse(nextCollected)
    assertEquals(before, sxssfTempFiles())
  }

  @Test
  fun `names validate without collecting sources`() {
    assertFailsWith<IllegalArgumentException> {
      flowWorkbook { sheet("bad/name", invoiceSheet, invoices.asFlow()) }
    }
    assertFailsWith<IllegalArgumentException> {
      flowWorkbook {
        sheet("Invoices", invoiceSheet, invoices)
        sheet("INVOICES", invoiceSheet, invoices.asFlow())
      }
    }
  }
}
