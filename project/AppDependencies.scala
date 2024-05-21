import sbt.*

object AppDependencies {
  import play.core.PlayVersion

  val bootStrapVersion = "8.5.0"
  val playVersion = "play-30"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% s"play-frontend-hmrc-$playVersion"       % "8.5.0",
    "uk.gov.hmrc" %% s"play-conditional-form-mapping-$playVersion" % "2.0.0",
    "uk.gov.hmrc"       %% s"bootstrap-frontend-$playVersion"        % bootStrapVersion,
  )

  val test = Seq(
    "uk.gov.hmrc" %% s"bootstrap-test-$playVersion" % bootStrapVersion,
    "org.scalatest"           %% "scalatest"               % "3.2.18",
    "org.scalatestplus"       %% "scalacheck-1-15"         % "3.2.11.0",
    "org.scalatestplus"       %% "mockito-3-4"             % "3.2.10.0",
    "org.scalatestplus.play"  %% "scalatestplus-play"      % "7.0.1",
    "org.pegdown"             %  "pegdown"                 % "1.6.0",
    "org.jsoup"               %  "jsoup"                   % "1.17.2",
    "org.playframework"       %% "play-test"               % PlayVersion.current,
    "org.mockito"             %% "mockito-scala"           % "1.17.31",
    "org.scalacheck"          %% "scalacheck"              % "1.18.0",
    "com.vladsch.flexmark"    %  "flexmark-all"            % "0.64.8"
  ).map(_ % "test")

  def apply(): Seq[ModuleID] = compile ++ test
}
