import dsl.CellType
import excel.dsl.test.SheetBuilder
import excel.dsl.test.workbook
import excel.dsl.test.sheet
import excel.dsl.test.column

data class User(val name: String, val age: Int, val cellType: CellType = CellType())

val users = sequenceOf(User("n", 2), User("n", 3))

val w = workbook {
  sheet(users) {
    column("name") { it.name }
    column("cell") { it.cellType }
  }
}

context(builder: SheetBuilder<T>)
fun <T> column(name: String, block: (T) -> CellType) {}