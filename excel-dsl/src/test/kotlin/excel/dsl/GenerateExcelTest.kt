package excel.dsl

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.io.TempDir

class GenerateExcelTest {
  @TempDir
  lateinit var dir: Path

  @Test
  fun `writes a sheet named after the file`() {
    val file = dir.resolve("invoices.xlsx")
    generateExcel(file.toString(), invoices.asSequence()) {
      val customer by column { it.customer }
      val hours by column { it.hours }
      val rate by column { it.rate }
      val total by formula { hours * rate }
    }
    val workbook = file.read()
    assertEquals("invoices", workbook.getSheetName(0))
    assertEquals(
      listOf(
        listOf("Customer", "Hours", "Rate", "Total"),
        listOf("Ada", "12", "90", "=B2*C2"),
        listOf("Linus", "8", "80", "=B3*C3"),
        listOf("Grace", "20", "110", "=B4*C4"),
      ),
      workbook.cells(),
    )
  }

  @Test
  fun `the config styles the headers`() {
    val file = dir.resolve("raw.xlsx")
    generateExcel(file.toString(), invoices.asSequence(), ExcelConfig.PropertyNames) {
      val issuedOn by column { it.issuedOn }
    }
    assertEquals(listOf("issuedOn"), file.read().cells().first())
  }

  @Test
  fun `the contract lets the block assign a val`() {
    val customers: Column<String>
    generateExcel(dir.resolve("x.xlsx").toString(), emptySequence<Invoice>()) {
      val customer by column { it.customer }
      customers = customer
    }
    assertEquals("Customer", customers.name)
  }

  @Test
  fun `a flow writes the same file`() = runBlocking {
    val file = dir.resolve("flow.xlsx")
    val customers: Column<String>
    generateExcel(file.toString(), invoices.asFlow()) {
      val customer by column { it.customer }
      customers = customer
    }
    assertEquals("flow", file.read().getSheetName(0))
    assertEquals(listOf("Customer", "Ada", "Linus", "Grace"), file.read().cells().map { it.single() })
    assertEquals(0, customers.index)
  }
}
