package dsl

import excel.dsl.test.SheetBuilder

class CellType

context(builder: SheetBuilder<T>)
fun <T> column(name: String, block: (T) -> CellType) {}


