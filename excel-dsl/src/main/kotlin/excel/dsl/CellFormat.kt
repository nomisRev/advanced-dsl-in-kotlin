package excel.dsl

/**
 * An Excel number format, how a cell's value is shown, not what it holds.
 * `CellFormat("0.0")` takes any pattern Excel's "Format Cells" dialog does.
 */
@Suppress("PROPERTY_WITH_BACKING_FIELD_INSIDE_VALUE_CLASS")
@JvmInline
value class CellFormat(val pattern: String) {
  companion {
    /** `1,080` */
    val Integer: CellFormat = CellFormat("#,##0")

    /** `1,080.00` */
    val Decimal: CellFormat = CellFormat("#,##0.00")

    /** `21.00%` for `0.21` */
    val Percent: CellFormat = CellFormat("0.00%")

    /** `2026-01-05`, the default for `LocalDate`. */
    val Date: CellFormat = CellFormat("yyyy-mm-dd")

    /** `2026-01-05 09:30:00`, the default for `LocalDateTime` and `Instant`. */
    val DateTime: CellFormat = CellFormat("yyyy-mm-dd hh:mm:ss")

    /** `€1,080.00`: [symbol] before the amount, [decimals] after the point. */
    fun currency(symbol: String, decimals: Int = 2): CellFormat {
      require(decimals >= 0) { "decimals must not be negative, got $decimals" }
      val fraction = if (decimals == 0) "" else "." + "0".repeat(decimals)
      return CellFormat("\"${symbol.replace("\"", "\"\"")}\"#,##0$fraction")
    }
  }
}