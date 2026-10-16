import dsl.CellType
import excel.dsl.test.SheetBuilder
import excel.dsl.test.workbook
import excel.dsl.test.sheet
import excel.dsl.test.column
import dsl.column
import excel.dsl.test.formula
import excel.dsl.test.times

data class User(val name: String, val age: Int, val cellType: CellType = CellType())

val users =
  sequenceOf(User("n", 2), User("n", 3))

val w = workbook {
  sheet(users) {

    val x = contextOf<SheetBuilder<User>>()
    x.add()

    column("name") { it.name }
    column("cell") { it.cellType }
    val one = column("one") { 1 }
    val two = column("two") { 2 }
    formula("times") {  one * two }

    one * two
  }
}