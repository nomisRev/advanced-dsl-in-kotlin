package excel.dsl

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.junit.jupiter.api.io.TempDir

/** Compile against the public API without friend paths or test-module access. */
class CompileSafetyTest {
  @TempDir lateinit var dir: Path

  private fun compile(name: String, body: String): Pair<ExitCode, String> {
    val source = dir.resolve("$name.kt")
    source.writeText("""
      import excel.dsl.*
      fun example() {
        $body
      }
    """.trimIndent())
    val diagnostics = ByteArrayOutputStream()
    val exit = PrintStream(diagnostics).use { output ->
      K2JVMCompiler().exec(
        output, "-no-stdlib", "-no-reflect", "-jvm-target", "21",
        "-language-version", "2.5", "-Xcollection-literals", "-Xcompanion-blocks-and-extensions",
        "-Xcontext-sensitive-resolution", "-Xreturn-value-checker=check",
        "-classpath", System.getProperty("dsl.compile.classpath"),
        "-d", dir.resolve(name).createDirectories().toString(), source.toString(),
      )
    }
    return exit to diagnostics.toString()
  }

  @Test
  fun `typed numeric boolean text and conditional formulas compile`() {
    val (exit, message) = compile("Valid", """
      sheet<Int> {
        val n by column { it }
        val flag: Column<Boolean> by formula { n gt 0 }
        val label: Column<String> by formula { flag then "yes" otherwise "no" }
        val value: Column<Number> by formula { flag then 2 * n otherwise -n }
        val converted by formula { flag.asNumber() + 1 }
        val named = formula("Alias") { label.ref() }
      }
    """)
    assertEquals(ExitCode.OK, exit, message)
  }

  @Test
  fun `invalid operands result types and branches are rejected`() {
    val cases = listOf(
      "val invalid: Column<Number> by formula { n gt 0 }",
      "val invalid by formula { flag + 1 }",
      "val invalid by formula { text * n }",
      "val invalid by formula { flag then 1 otherwise \"no\" }",
      "val invalid by formula { flag then (n * 2) otherwise text }",
      "val invalid by formula { flag then \"yes\" otherwise flag.ref() }",
      "val invalid by formula { n then 1 otherwise 0 }",
      "totals { flag.sum() }",
      "val invalid by formula { n eq text }",
      "val invalid by formula { Expr.Compare(n.ref(), Comparison.Equal, text.ref()) }",
    )
    cases.forEachIndexed { index, statement ->
      val (exit, message) = compile("Invalid$index", """
        sheet<Int> {
          val n by column { it }
          val flag by column { it > 0 }
          val text by column { it.toString() }
          $statement
        }
      """)
      assertEquals(ExitCode.COMPILATION_ERROR, exit, "$statement\n$message")
      assertTrue(message.contains("error:"), message)
    }
  }

  @Test
  fun `an incomplete conditional cannot become a formula`() {
    val (exit, message) = compile("IncompleteBranch", """
      sheet<Int> {
        val n by column { it }
        val due by formula<Number> { n gt 0 then n * 2 }
      }
    """)
    assertEquals(ExitCode.COMPILATION_ERROR, exit, message)
    assertTrue(message.contains("return type mismatch: expected 'Expr<Number>', actual 'Branch<Number>'."), message)
  }

  @Test
  fun `formula and totals scopes reject outer receivers and row operators`() {
    for ((index, statement) in listOf(
      "val bad by formula { val hidden by column { it }; n * 2 }",
      "totals { n.total { n * 2 } }",
      "totals { n.total { n.ref() } }",
    ).withIndex()) {
      val (exit, message) = compile("Scope$index", "sheet<Int> { val n by column { it }; $statement }")
      assertEquals(ExitCode.COMPILATION_ERROR, exit, message)
    }
  }
  @Test
  fun `full mode exports warnings to check mode consumers`() {
    val (exit, message) = compile("IgnoredDelegates", """
      val schema = sheet<Int> {
        column { it }
        formula { literal(1) }
        column("Named") { it }
        formula("Named formula") { literal(2) }
      }
      println(schema.headers)
    """)
    assertEquals(ExitCode.OK, exit, message)
    assertTrue(message.contains("unused return value of 'column'"), message)
    assertTrue(message.contains("unused return value of 'formula'"), message)
    assertEquals(2, Regex("unused return value").findAll(message).count(), message)
  }

  @Test
  fun `unsafe expression entry points are internal`() {
    for ((index, expression) in listOf(
      "n.ref().evaluate(mapOf(n to \"wrong\"))",
      "Expr.Range(n)",
      "Expr.Constant(1.0)",
      "Expr.Aggregate(AggregateFunction.Sum, listOf(n.ref()))",
    ).withIndex()) {
      val (exit, message) = compile("Internal$index", """
        val schema = sheet<Int> {
          val n by column { it }
          formula("Bad") { $expression }
        }
      """)
      assertEquals(ExitCode.COMPILATION_ERROR, exit, message)
      assertTrue(message.contains("internal"), message)
    }
  }

  @Test
  fun `total overloads reject mismatched types and numeric count rejects text`() {
    for ((index, statement) in listOf(
      "n.total { literal(true) }",
      "text.total { sum(n) }",
      "flag.total { literal(1) }",
      "text.count()",
      "n.total { count(text) }",
    ).withIndex()) {
      val (exit, message) = compile("TotalType$index", """
        val schema = sheet<Int> {
          val n by column { it }
          val text by column { it.toString() }
          val flag by column { it > 0 }
          totals { $statement }
        }
      """)
      assertEquals(ExitCode.COMPILATION_ERROR, exit, message)
    }
  }

  @Test
  fun `named overloads preserve decimal and integer types`() {
    val (exit, message) = compile("NamedTypes", """
      val schema = sheet<Int> {
        val text: Column<String> = column("Text") { it.toString() }
        val n: Column<Number> = column("Number") { it }
        val decimal: Column<java.math.BigDecimal> by column { it.toBigDecimal() }
        val integer: Column<java.math.BigInteger> = column("Integer") { it.toBigInteger() }
        val flag: Column<Boolean> = column("Flag") { it > 0 }
        val date = column("Date") { java.time.LocalDate.of(2026, 9, 1) }
        val at = column("At") { java.time.LocalDateTime.of(2026, 9, 1, 12, 0) }
        val instant = column("Instant") { java.time.Instant.EPOCH }
        val enum = column("Enum") { java.time.DayOfWeek.MONDAY }
        val amount = formula("Amount") { decimal + integer }
        totals {
          text.total { sum(n) gt 0 then "positive" otherwise "other" }
          n.count()
          flag.total { sum(n) gt 0 }
          amount.sum()
        }
      }
      println(schema.headers)
    """)
    assertEquals(ExitCode.OK, exit, message)
  }

}
