package presentation.support

/** A column of a sheet, returned by `val name by column { }`, holding values of type [V]. */
class Column<V>(val name: String, val index: Int)

/** The column's letters in a cell reference: `A` to `Z`, then `AA`, `AB`, ... */
val Column<*>.letter: String
  get() = buildString {
    var n = index + 1
    while (n > 0) {
      insert(0, 'A' + (n - 1) % 26)
      n = (n - 1) / 26
    }
  }

/** The cell of this column in [row], the 1-based row number Excel shows. */
fun Column<*>.reference(row: Int): String = "$letter$row"
