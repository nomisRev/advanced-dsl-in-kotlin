package excel.dsl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TotalsTest {
  private val totalsSheet = sheet<Invoice> {
    val customer by column { it.customer }
    val hours by column { it.hours }
    val rate by column { it.rate }
    val total by formula { hours * rate }
    totals {
      customer.label("Total")
      hours.sum()
      rate.average()
      total.sum()
    }
  }

  @Test
  fun `the totals row sits under the data, over the data rows`() {
    assertEquals(
      listOf("Total", "=SUM(B2:B4)", "=AVERAGE(C2:C4)", "=SUM(D2:D4)"),
      totalsSheet.writeToBytes().read().cells().last(),
    )
  }

  @Test
  fun `the range follows the number of rows`() {
    val cells = totalsSheet.writeToBytes(invoices.take(1).asSequence()).read().cells()
    assertEquals(3, cells.size)
    assertEquals("=SUM(B2:B2)", cells.last()[1])
  }

  @Test
  fun `no rows, no totals row`() {
    val cells = totalsSheet.writeToBytes(emptySequence()).read().cells()
    assertEquals(listOf(listOf("Customer", "Hours", "Rate", "Total")), cells)
    assertEquals(emptyMap(), totalsSheet.totals(emptySequence()))
  }

  @Test
  fun `an empty schema has no totals with or without rows`() {
    val sheet = sheet<Invoice> {}
    assertEquals(emptyMap(), sheet.totals(emptySequence()))
    assertEquals(emptyMap(), sheet.totals(invoices.asSequence()))
  }

  @Test
  fun `columns without a total stay empty`() {
    val sheet = sheet<Invoice> {
      val customer by column { it.customer }
      val hours by column { it.hours }
      totals { hours.sum() }
    }
    assertEquals(listOf(null, "=SUM(B2:B4)"), sheet.writeToBytes().read().cells().last())
  }

  @Test
  fun `the totals row is bold`() {
    val workbook = totalsSheet.writeToBytes().read()
    val row = workbook.getSheetAt(0).getRow(4)
    assertTrue(row.all { workbook.getFontAt(it.cellStyle.fontIndex).bold })
  }

  @Test
  fun `every shorthand and any formula`() {
    val sheet = sheet<Invoice> {
      val customer by column { it.customer }
      val hours by column { it.hours }
      val rate by column { it.rate }
      val total by formula { hours * rate }
      val gross by formula { total * 1.21 }
      totals {
        customer.count()
        hours.min()
        rate.max()
        total.total { sum(total) gt 5000 then sum(total) * 0.9 otherwise sum(total) }
        gross.total { round(sum(gross) / count(customer), 2) }
      }
    }
    assertEquals(
      listOf(
        "=COUNTA(A2:A4)",
        "=MIN(B2:B4)",
        "=MAX(C2:C4)",
        "=IF(SUM(D2:D4)>5000, SUM(D2:D4)*0.9, SUM(D2:D4))",
        "=ROUND(SUM(E2:E4)/COUNTA(A2:A4), 2)",
      ),
      sheet.writeToBytes().read().cells().last(),
    )
  }

  @Test
  fun `totals evaluates without Excel`() {
    assertEquals(
      mapOf("Customer" to "Total", "Hours" to 40.0, "Rate" to 280.0 / 3, "Total" to 3920.0),
      totalsSheet.totals(invoices.asSequence()),
    )
  }

  @Test
  fun `a comparison total is a Boolean`() {
    val sheet = sheet<Invoice> {
      val hours by column { it.hours }
      totals { hours.total { sum(hours) gt 30 } }
    }
    assertEquals(mapOf("Hours" to true), sheet.totals(invoices.asSequence()))
  }

  @Test
  fun `totals skip empty cells, text and booleans like Excel`() {
    val sheet = sheet<Invoice> {
      val bonus by column { if (it.hours > 10) it.hours else null }
      val long by formula { bonus gt 15 }
      totals {
        bonus.average()
        long.count()
      }
    }
    assertEquals(mapOf("Bonus" to 16.0, "Long" to 3.0), sheet.totals(invoices.asSequence()))
    assertExcelAgrees(sheet, invoices)
  }

  @Test
  fun `Excel computes what totals computes`() {
    val sheet = sheet<Invoice> {
      val customer by column { it.customer }
      val hours by column { it.hours }
      val rate by column { it.rate }
      val total by formula { hours * rate }
      val empty by column { null as Int? }
      totals {
        customer.label("Total")
        hours.sum()
        rate.average()
        total.total { round(sum(total) / count(customer), 1) }
        empty.average()
      }
    }
    assertExcelAgrees(sheet, invoices)
    assertEquals(Double.NaN, sheet.totals(invoices.asSequence())["Empty"])
  }

  @Test
  fun `one total per column`() {
    val error = assertFailsWith<IllegalArgumentException> {
      sheet<Invoice> {
        val hours by column { it.hours }
        totals {
          hours.sum()
          hours.average()
        }
      }
    }
    assertEquals("Hours already has a total", error.message)
  }

  @Test
  fun `totals may be declared in several blocks`() {
    val sheet = sheet<Invoice> {
      val customer by column { it.customer }
      totals { customer.label("Total") }
      val hours by column { it.hours }
      totals { hours.sum() }
    }
    assertEquals(listOf("Total", "=SUM(B2:B4)"), sheet.writeToBytes().read().cells().last())
  }

  @Test
  fun `a total of another sheet's column fails`() {
    lateinit var foreign: Column<Number>
    sheet<Invoice> {
      val hours by column { it.hours }
      foreign = hours
    }
    assertFailsWith<IllegalArgumentException> { sheet<Invoice> { totals { foreign.sum() } } }
    assertFailsWith<IllegalArgumentException> {
      sheet<Invoice> {
        val hours by column { it.hours }
        totals { hours.total { sum(foreign) } }
      }
    }
  }

  @Test
  fun `a total cannot reference a single row`() {
    assertFailsWith<IllegalArgumentException> {
      sheet<Invoice> {
        val hours by column { it.hours }
        totals { hours.total { Expr.Ref(hours) } }
      }
    }
  }

  @Test
  fun `a row formula cannot hold a range`() {
    assertFailsWith<IllegalArgumentException> {
      sheet<Invoice> {
        val hours by column { it.hours }
        val broken by formula { Expr.Range(hours) }
      }
    }
  }

  @Test
  fun `the totals block runs once, in place`() {
    var runs = 0
    sheet<Invoice> { totals { runs++ } }
    assertEquals(1, runs)
  }
}
