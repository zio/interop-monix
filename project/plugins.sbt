addSbtPlugin("ch.epfl.scala"                     % "sbt-bloop"                 % "1.4.12")
addSbtPlugin("com.eed3si9n"                      % "sbt-buildinfo"             % "0.10.0")
addSbtPlugin("com.github.cb372"                  % "sbt-explicit-dependencies" % "0.2.16")
addSbtPlugin("com.github.sbt"                    % "sbt-ci-release"            % "1.5.10")
addSbtPlugin("com.github.sbt"                    % "sbt-unidoc"                % "0.5.0")
addSbtPlugin("com.thoughtworks.sbt-api-mappings" % "sbt-api-mappings"          % "3.0.0")
addSbtPlugin("de.heikoseeberger"                 % "sbt-header"                % "5.6.5")
addSbtPlugin("org.portable-scala"                % "sbt-scalajs-crossproject"  % "1.1.0")
addSbtPlugin("org.scala-js"                      % "sbt-scalajs"               % "1.9.0")
addSbtPlugin("org.scalameta"                     % "sbt-mdoc"                  % "2.3.0")
addSbtPlugin("org.scalameta"                     % "sbt-scalafmt"              % "2.4.6")
addSbtPlugin("pl.project13.scala"                % "sbt-jmh"                   % "0.4.3")

libraryDependencies += "org.snakeyaml" % "snakeyaml-engine" % "2.3"
