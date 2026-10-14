import excel.dsl.CellFormat
import excel.dsl.Column
import excel.dsl.ColumnDelegate
import excel.dsl.SheetBuilder
import excel.dsl.column
import kotlin.experimental.ExperimentalTypeInference

data class Custom(val raw: String)

data class Invoice(
  val name: String,
  val custom: Custom
)

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
@JvmName("textColumn")
context(_: SheetBuilder<T>)
fun <T> customColumn(
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> Custom
): ColumnDelegate<String> =
  column(format, width) { value(it).raw }

val  x =  excel.dsl.sheet<Invoice> {
  val name by column { it.name }
  val custom by customColumn { it.custom }
}

sealed interface Expr<out T> {
  data class Ref<T>(val column: Column<T>) : Expr<T>
  data class Times(val left: Expr<Number>, val right: Expr<Number>) : Expr<Double>
  data class Eq<T>(val left: Expr<T>, val right: Expr<T>) : Expr<Boolean>
  data class In<T>(val target: Expr<T>, val values: Iterable<T>) : Expr<Boolean>
  data class Contains(val target: Expr<String>, val substring: String) : Expr<Boolean>
}

class FormulaScope internal constructor()

context(_: FormulaScope)
infix fun Column<String>.contains(other: String): Expr<Boolean> =
  Expr.Contains(Expr.Ref(this), other)

@Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "ERROR_SUPPRESSION")
context(_: FormulaScope)
infix fun <@kotlin.internal.OnlyInputTypes A>  Iterable<A>.contains(other: Column<A>): Expr<Boolean> =
  Expr.In(Expr.Ref(other), this)

fun <A> formula(
  block: FormulaScope.() -> Expr<A>
): Expr<A> = block(FormulaScope())

val company: Column<String> = TODO()
val hours: Column<Long> = TODO()
val rate: Column<Double> = TODO()

val xx = formula { company contains "ABC" }
val xxx = formula { ["ABC", "DEF"] contains company }
// Rejected by OnlyInputTypes: formula { [1, 2, 3] contains company }


