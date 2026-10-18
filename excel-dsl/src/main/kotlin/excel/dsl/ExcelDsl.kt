package excel.dsl

/**
 * Marks the builders and formula contexts, so an inner block cannot implicitly
 * access an outer context: no `column { }` inside `formula { }`.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ExcelDsl
