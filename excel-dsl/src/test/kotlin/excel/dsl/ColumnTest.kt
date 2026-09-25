package excel.dsl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ColumnTest {
  private fun column(index: Int) = Column<Number>("c", index, Unit)

  @Test
  fun `letters count like Excel columns`() {
    val letters = listOf(0, 1, 25, 26, 27, 51, 52, 701, 702, 16383).map { column(it).letter }
    assertEquals(listOf("A", "B", "Z", "AA", "AB", "AZ", "BA", "ZZ", "AAA", "XFD"), letters)
  }

  @Test
  fun `reference joins letter and 1-based row`() {
    assertEquals("C2", column(2).reference(2))
    assertEquals("AA10", column(26).reference(10))
  }

  @Test
  fun `reference rejects rows before the first`() {
    assertFailsWith<IllegalArgumentException> { column(0).reference(0) }
  }

  @Test
  fun `toString shows name and letter`() {
    assertEquals("Column(c, B)", column(1).toString())
  }
}
