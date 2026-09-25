package snippets.`lesson-2`

import presentation.support.CustomType
import presentation.support.typed.column as baseColumn
import presentation.support.Invoice
import presentation.support.typed.SheetBuilder
import kotlin.experimental.ExperimentalTypeInference

@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
fun SheetBuilder<Invoice>.column(name: String, block: (Invoice) -> CustomType) {
  baseColumn(name) { block(it).toColumnText() }
}