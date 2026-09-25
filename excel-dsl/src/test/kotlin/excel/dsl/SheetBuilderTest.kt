package excel.dsl

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class SheetBuilderTest {
  @Test
  fun `headers follow declaration order, capitalised by default`() {
    assertEquals(listOf("Customer", "Hours", "Rate", "IssuedOn", "Total", "Gross"), invoiceSheet.headers)
  }

  @Test
  fun `the config styles property names`() {
    val raw = sheet<Invoice>(ExcelConfig.PropertyNames) { val issuedOn by column { it.issuedOn } }
    val upper = sheet<Invoice>(ExcelConfig { it.uppercase() }) { val issuedOn by column { it.issuedOn } }
    assertEquals(listOf("issuedOn"), raw.headers)
    assertEquals(listOf("ISSUEDON"), upper.headers)
  }

  @Test
  fun `the config styles formula headers too`() {
    val sheet = sheet<Invoice>(ExcelConfig { "#$it" }) {
      val hours by column { it.hours }
      val double by formula { hours * 2 }
    }
    assertEquals(listOf("#hours", "#double"), sheet.headers)
  }

  @Test
  fun `a column registers at by, even when never read`() {
    val sheet = sheet<Invoice> {
      val customer by column { it.customer }
      val hours by column { it.hours }
    }
    assertEquals(listOf("Customer", "Hours"), sheet.headers)
  }

  @Test
  fun `the column carries its header and index`() {
    lateinit var hours: Column<Number>
    sheet<Invoice> {
      val customer by column { it.customer }
      val h by column { it.hours }
      hours = h
    }
    assertEquals("H", hours.name)
    assertEquals(1, hours.index)
    assertEquals("B", hours.letter)
  }

  @Test
  fun `reading a delegated column returns the same instance`() {
    sheet<Invoice> {
      val hours by column { it.hours }
      assertSame(hours, hours)
    }
  }

  @Test
  fun `the lambda return type picks the column type`() {
    sheet<Invoice> {
      val text: Column<String> by column { it.customer }
      val number: Column<Number> by column { it.hours }
      val date: Column<LocalDate> by column { it.issuedOn }
      val nullable: Column<String> by column { it.issuedOn?.toString() }
    }
  }

  @Test
  fun `a string header is used as is`() {
    val sheet = sheet<Invoice> {
      "issued on" { it.issuedOn }
      "Customer" { it.customer }
      "Hours" { it.hours }
    }
    assertEquals(listOf("issued on", "Customer", "Hours"), sheet.headers)
  }

  @Test
  fun `a string header returns a typed column formulas can reference`() {
    val sheet = sheet<Invoice> {
      val hours: Column<Number> = "Hours" { it.hours }
      val date: Column<LocalDate> = "Issued" { it.issuedOn }
      val text: Column<String> = "Customer" { it.customer }
      val double by formula { hours * 2 }
    }
    assertEquals(24.0, sheet.evaluate(invoices[0])["Double"])
  }

  @Test
  fun `duplicate headers fail`() {
    val error = assertFailsWith<IllegalArgumentException> {
      sheet<Invoice> {
        "Hours" { it.hours }
        val hours by column { it.hours }
      }
    }
    assertEquals("Duplicate header: Hours", error.message)
  }

  @Test
  fun `a config mapping two names onto one header fails`() {
    assertFailsWith<IllegalArgumentException> {
      sheet<Invoice>(ExcelConfig { "same" }) {
        val customer by column { it.customer }
        val hours by column { it.hours }
      }
    }
  }

  @Test
  fun `a formula may not reference another sheet`() {
    lateinit var foreign: Column<Number>
    sheet<Invoice> {
      val hours by column { it.hours }
      foreign = hours
    }
    assertFailsWith<IllegalArgumentException> {
      sheet<Invoice> {
        val hours by column { it.hours }
        val broken by formula { hours * foreign }
      }
    }
  }

  @Test
  fun `the block runs exactly once, in place`() {
    var runs = 0
    val customers: Column<String>
    sheet<Invoice> {
      runs++
      val customer by column { it.customer }
      customers = customer
    }
    assertEquals(1, runs)
    assertEquals("Customer", customers.name)
  }

  @Test
  fun `the formula block runs once, not per row`() {
    var runs = 0
    val sheet = sheet<Invoice> {
      val hours by column { it.hours }
      val double by formula {
        runs++
        hours * 2
      }
    }
    sheet.writeToBytes()
    sheet.writeToBytes()
    invoices.forEach(sheet::evaluate)
    assertEquals(1, runs)
  }

  @Test
  fun `an empty sheet has no headers`() {
    assertEquals(emptyList(), sheet<Invoice> { }.headers)
  }

  @Test
  fun `the sheet does not change when a leaked builder grows`() {
    lateinit var builder: SheetBuilder<Invoice>
    val sheet = sheet<Invoice> {
      val hours by column { it.hours }
      builder = this
    }
    with(builder) { "Rate" { it.rate } }
    assertEquals(listOf("Hours"), sheet.headers)
  }
}
