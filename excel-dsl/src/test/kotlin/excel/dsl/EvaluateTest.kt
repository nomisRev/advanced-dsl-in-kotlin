package excel.dsl

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class EvaluateTest {
  @Test
  fun `data columns keep their values, formulas compute doubles`() {
    assertEquals(
      mapOf(
        "Customer" to "Ada",
        "Hours" to 12,
        "Rate" to 90,
        "IssuedOn" to LocalDate.of(2026, 1, 5),
        "Total" to 1080.0,
        "Gross" to 1080.0 * 1.21,
      ),
      invoiceSheet.evaluate(invoices[0]),
    )
  }

  @Test
  fun `keys follow column order`() {
    assertEquals(invoiceSheet.headers, invoiceSheet.evaluate(invoices[0]).keys.toList())
  }

  @Test
  fun `nulls stay null and count as zero in formulas`() {
    val sheet = sheet<Invoice> {
      val bonus by column { if (it.hours > 10) it.hours else null }
      val customer by column { it.customer.takeIf { false } }
      val plusOne by formula { bonus + 1 }
    }
    assertEquals(mapOf("Bonus" to null, "Customer" to null, "PlusOne" to 1.0), sheet.evaluate(invoices[1]))
    assertEquals(mapOf("Bonus" to 12, "Customer" to null, "PlusOne" to 13.0), sheet.evaluate(invoices[0]))
  }

  @Test
  fun `every operator evaluates`() {
    val sheet = sheet<Invoice> {
      val hours by column { it.hours }
      val rate by column { it.rate }
      val plus by formula { hours + rate }
      val minus by formula { rate - hours }
      val div by formula { rate / hours }
      val sum by formula { sum([hours, rate, plus]) }
      val large by formula { plus gt 100 }
    }
    assertEquals(
      mapOf("Hours" to 8, "Rate" to 80, "Plus" to 88.0, "Minus" to 72.0, "Div" to 10.0, "Sum" to 176.0, "Large" to false),
      sheet.evaluate(invoices[1]),
    )
  }

  @Test
  fun `a comparison is a Boolean, and a number once used in arithmetic`() {
    val sheet = sheet<Invoice> {
      val hours by column { it.hours }
      val long by formula { hours gt 10 }
      val bonus by formula { long * 100 }
      val flag by formula { hours gt 10 then 1 otherwise 0 }
    }
    assertEquals(mapOf("Hours" to 12, "Long" to true, "Bonus" to 100.0, "Flag" to 1.0), sheet.evaluate(invoices[0]))
    assertEquals(mapOf("Hours" to 8, "Long" to false, "Bonus" to 0.0, "Flag" to 0.0), sheet.evaluate(invoices[1]))
  }

  @Test
  fun `if, round and row aggregates evaluate`() {
    val sheet = sheet<Invoice> {
      val hours by column { it.hours }
      val rate by column { it.rate }
      val discounted by formula { hours * rate gt 1000 then round(hours * rate * 0.95, 1) otherwise hours * rate }
      val average by formula { average([hours, rate]) }
      val min by formula { min([hours, rate]) }
      val max by formula { max([hours, rate]) }
    }
    assertEquals(
      mapOf("Hours" to 12, "Rate" to 90, "Discounted" to 1026.0, "Average" to 51.0, "Min" to 12.0, "Max" to 90.0),
      sheet.evaluate(invoices[0]),
    )
    assertEquals(640.0, sheet.evaluate(invoices[1])["Discounted"])
  }

  @Test
  fun `each column lambda runs once per evaluate`() {
    var reads = 0
    val sheet = sheet<Invoice> {
      val hours by column { reads++; it.hours }
      val a by formula { hours * 2 }
      val b by formula { hours * 3 }
    }
    sheet.evaluate(invoices[0])
    assertEquals(1, reads)
  }
}
