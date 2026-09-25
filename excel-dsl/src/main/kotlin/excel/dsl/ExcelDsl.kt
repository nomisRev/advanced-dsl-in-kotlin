package excel.dsl

/**
 * Marks the receivers of the DSL, so an inner block cannot implicitly call
 * the outer one: no `column { }` inside `formula { }`.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ExcelDsl
