package excel.dsl

import java.io.ByteArrayOutputStream
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.jupiter.api.io.TempDir

class WorkbookTest {
  @TempDir
  lateinit var dir: Path

  private val q1 = invoices.take(1).asSequence()
  private val q2 = invoices.drop(1).asSequence()

  private fun Workbook.bytes() = ByteArrayOutputStream().also(::write).toByteArray()

  @Test
  fun `sheets are written in declaration order`() {
    val report = workbook {
      sheet("Q2", invoiceSheet, q2)
      sheet("Q1", invoiceSheet, q1)
    }
    val written = report.bytes().read()
    assertEquals(listOf("Q2", "Q1"), report.names)
    assertEquals(listOf("Q2", "Q1"), (0 until written.numberOfSheets).map(written::getSheetName))
    assertEquals(3, written.getSheet("Q2").physicalNumberOfRows)
    assertEquals(2, written.getSheet("Q1").physicalNumberOfRows)
  }

  @Test
  fun `each sheet has its own formula rows`() {
    val written = workbook {
      sheet("Q1", invoiceSheet, q1)
      sheet("Q2", invoiceSheet, q2)
    }.bytes().read()
    assertEquals(written.cells(0).drop(1).map { it[4] }, listOf("=B2*C2"))
    assertEquals(written.cells(1).drop(1).map { it[4] }, listOf("=B2*C2", "=B3*C3"))
  }

  @Test
  fun `sheets of different row types compose`() {
    val names = sheet<String> { val name by column { it } }
    val written = workbook {
      sheet("Invoices", invoiceSheet, q1)
      sheet("Names", names, sequenceOf("Ada", "Grace"))
    }.bytes().read()
    assertEquals(listOf(listOf("Name"), listOf("Ada"), listOf("Grace")), written.cells(1))
  }

  @Test
  fun `rows are not pulled until write, then once per write`() {
    var pulled = 0
    val rows = sequence { pulled++; yieldAll(invoices) }
    val report = workbook { sheet("All", invoiceSheet, rows) }
    assertEquals(0, pulled)
    report.bytes()
    report.bytes()
    assertEquals(2, pulled)
  }

  @Test
  fun `the block runs once, in place`() {
    var runs = 0
    workbook { runs++ }
    assertEquals(1, runs)
  }

  @Test
  fun `an empty workbook still writes`() {
    assertEquals(0, workbook { }.bytes().read().numberOfSheets)
  }

  @Test
  fun `sheet names are unique, ignoring case`() {
    val error = assertFailsWith<IllegalArgumentException> {
      workbook {
        sheet("Q1", invoiceSheet, q1)
        sheet("q1", invoiceSheet, q2)
      }
    }
    assertEquals("Duplicate sheet name: q1", error.message)
  }

  @Test
  fun `invalid sheet names fail when declared`() {
    for (name in listOf("", "a/b", "a:b", "x".repeat(32), "'quoted")) {
      assertFailsWith<IllegalArgumentException>(name) { workbook { sheet(name, invoiceSheet, q1) } }
    }
  }

  @Test
  fun `a path writes the same workbook`() {
    val report = workbook { sheet("Q1", invoiceSheet, q1) }
    val file = dir.resolve("report.xlsx")
    report.write(file)
    assertEquals(report.bytes().read().cells(), file.read().cells())
    assertEquals("Q1", file.read().getSheetName(0))
  }
}
