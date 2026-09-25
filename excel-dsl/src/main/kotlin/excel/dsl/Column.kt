package excel.dsl

/**
 * A column of a sheet holding values of type [V], returned by
 * `val name by column { }`. Only the sheet that declared it can reference it.
 *
 *
 */
class Column<out V> internal constructor(val name: String, val index: Int, internal val owner: Any, internal val kind: ValueKind = ValueKind.Number) {
  override fun toString(): String = "Column($name, $letter)"
}

/** The column's letters in a cell reference: `A` to `Z`, then `AA`, `AB`, ... */
val Column<*>.letter: String
  get() = buildString {
    var n = index + 1
    while (n > 0) {
      insert(0, 'A' + (n - 1) % 26)
      n = (n - 1) / 26
    }
  }

/** The cell of this column in [row], the 1-based row number Excel shows: `B2`. */
fun Column<*>.reference(row: Int): String {
  require(row >= 1) { "Excel rows start at 1, got $row" }
  return "$letter$row"
}

internal enum class ValueKind { Number, Text, Boolean, Date, DateTime, Instant }
