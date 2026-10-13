import excel.dsl.CellFormat
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
fun <T> column(
  format: CellFormat? = null,
  width: Int? = null,
  value: (T) -> Custom
): ColumnDelegate<String> =
  column(format, width) { value(it).raw }

val  x =  excel.dsl.sheet<Invoice> {
  val name by column { it.name }
  val custom by column { it.custom }
}
