import sbt.Keys.parallelExecution
import sbt.{Def, Setting, Test}
import scoverage.ScoverageKeys

object CodeCoverageSettings {
  lazy val scoverageSettings: Seq[Def.Setting[? >: String & Double & Boolean]] = Seq(
    // Semicolon-separated list of regexes matching classes to exclude
    ScoverageKeys.coverageExcludedFiles :=
      """.*/target/scala-.*/routes/main/router/RoutesPrefix\.scala;
        |.*/target/scala-.*/routes/main/router/Routes\.scala;
        |.*/target/scala-.*/routes/main/router/ReverseRoutes\.scala;
        |.*/target/scala-.*/routes/main/router/javascript/JavaScriptReverseRoutes\.scala;
        |.*/target/scala-.*/routes/main/(app|prod)/routes_routing\.scala;
        |.*/target/scala-.*/routes/main/(app|prod)/routes_reverseRouting\.scala
        |""".stripMargin.replace("\n", ""),
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
