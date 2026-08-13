import sbt.Def
import scoverage.ScoverageKeys

object CodeCoverageSettings {
  lazy val scoverageSettings: Seq[Def.Setting[?]] = Seq(
    ScoverageKeys.coverageExcludedFiles :=
      """.*RoutesPrefix;
        |.*Routes;
        |.*ReverseRoutes;
        |.*JavaScriptReverseRoutes;
        |.*routes_routing;
        |.*routes_reverseRouting
        |""".stripMargin.replace("\n", ""),
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}
