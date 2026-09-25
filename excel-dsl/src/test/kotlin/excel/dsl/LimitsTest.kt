package excel.dsl

import java.io.OutputStream
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class LimitsTest {
  private fun <V> write(value: () -> V, block: SheetBuilder<Invoice>.(() -> V) -> Unit) =
    sheet<Invoice> { block(value) }.writeToBytes()

  private fun failure(block: SheetBuilder<Invoice>.() -> Unit): String? =
    assertFailsWith<IllegalArgumentException> { sheet(block = block).writeToBytes() }.message

  @Test
  fun `text up to 32767 characters`() {
    sheet<Invoice> { "Text" { "x".repeat(32_767) } }.writeToBytes()
    assertEquals(
      "Text, row 2: text longer than Excel's 32767 characters",
      failure { "Text" { "x".repeat(32_768) } },
    )
  }

  @Test
  fun `numbers up to 15 significant digits`() {
    sheet<Invoice> {
      "A" { 999_999_999_999_999L }
      "B" { 1_000_000_000_000_000_000L }
      "C" { BigDecimal("12345678901234.5") }
      "D" { BigDecimal("1.2345678901234500000") }
      "E" { BigDecimal.ZERO.setScale(40) }
      "F" { 0.1 + 0.2 }
    }.writeToBytes()
    assertEquals(
      "Id, row 2: 1234567890123456 has more than 15 significant digits, which Excel cannot store; write it as text",
      failure { "Id" { 1_234_567_890_123_456L } },
    )
    assertFailsWith<IllegalArgumentException> { sheet<Invoice> { "A" { BigDecimal("1234567890123.456") } }.writeToBytes() }
    assertFailsWith<IllegalArgumentException> { sheet<Invoice> { "A" { BigInteger("1234567890123456") } }.writeToBytes() }
  }

  @Test
  fun `decimal overflow and underflow fail with the cell location`() {
    for (text in listOf("1E+400", "-1E+400", "1E-400", "-1E-400")) {
      val value = BigDecimal(text)
      assertEquals(
        "Amount, row 2: $value is outside the supported numeric range; write it as text",
        failure { "Amount" { value } },
      )
    }
    for (value in listOf(BigInteger.TEN.pow(400), -BigInteger.TEN.pow(400))) {
      assertEquals(
        "Amount, row 2: $value is outside the supported numeric range; write it as text",
        failure { "Amount" { value } },
      )
    }
  }

  @Test
  fun `representable decimals and scaled zero remain numeric`() {
    for (value in listOf(BigDecimal("1E+300"), BigDecimal("-1E-300"), BigDecimal.ZERO.setScale(400))) {
      sheet<Invoice> { "Amount" { value } }.writeToBytes().read().use { workbook ->
        assertEquals(value.toDouble(), workbook.getSheetAt(0).getRow(1).getCell(0).numericCellValue)
      }
    }
  }

  @Test
  fun `NaN and infinity have no cell`() {
    assertEquals("A, row 2: NaN has no Excel equivalent", failure { "A" { Double.NaN } })
    assertEquals("A, row 2: Infinity has no Excel equivalent", failure { "A" { Float.POSITIVE_INFINITY } })
  }

  @Test
  fun `the error names the row`() {
    val sheet = sheet<Invoice> { "Hours" { if (it.customer == "Grace") Double.NaN else it.hours } }
    val error = assertFailsWith<IllegalArgumentException> { sheet.writeToBytes() }
    assertEquals("Hours, row 4: NaN has no Excel equivalent", error.message)
  }

  @Test
  fun `at most 16384 columns`() {
    sheet<Invoice> { repeat(16_384) { "c$it" { "" } } }
    val error = assertFailsWith<IllegalArgumentException> { sheet<Invoice> { repeat(16_385) { "c$it" { "" } } } }
    assertEquals("Excel sheets hold at most 16384 columns", error.message)
  }

  @Test
  fun `at most 1048576 rows, the header included`() {
    val sheet = sheet<Int> { val n by column { it } }
    var pulled = 0
    val rows = generateSequence(1) { it + 1 }.onEach { pulled++ }
    val error = assertFailsWith<IllegalStateException> { sheet.write(OutputStream.nullOutputStream(), rows) }
    assertEquals("Excel sheets hold at most 1048576 rows, header and totals included", error.message)
    assertEquals(1_048_576, pulled)
  }
}
