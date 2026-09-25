package presentation.support

/** Marks every receiver of the Excel DSL, including Apache POI types we don't own. */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class ExcelDsl
