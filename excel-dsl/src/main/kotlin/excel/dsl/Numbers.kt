package excel.dsl

import kotlin.math.abs
import org.apache.poi.ss.util.NumberComparer

/** Excel does not support IEEE 754 subnormal numbers. Signed zero remains valid. */
internal fun Double.isExcelNumber(): Boolean = isFinite() && (this == 0.0 || abs(this) >= java.lang.Double.MIN_NORMAL)

/** Arithmetic underflow becomes zero in Excel instead of a subnormal number. */
internal fun Double.flushUnderflow(): Double =
  if (this != 0.0 && abs(this) < java.lang.Double.MIN_NORMAL) 0.0 else this

/** Excel compares at its decimal precision; nearby binary doubles can be equal. */
internal fun compareExcelNumbers(left: Double, right: Double): Int =
  if (left == right) 0 else NumberComparer.compare(left, right)
