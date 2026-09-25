package presentation.support

/** What a `formula { }` produces for one row: a formula Excel recalculates, or a value. */
sealed interface Cell {
  data class Formula(val text: String) : Cell
  data class Value(val number: Double) : Cell
}

/** `=SUM(B2, C2)`: the cells of [columns] in [row]. */
fun sumFormula(row: Int, columns: Iterable<Column<*>>): Cell =
  Cell.Formula(columns.joinToString(", ", "=SUM(", ")") { it.reference(row) })
