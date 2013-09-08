
resolvers += "Sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

addSbtPlugin("com.github.mpeltonen" % "sbt-idea" % "1.6.0-SNAPSHOT")

resolvers += "Jetbrains releases" at "http://repository.jetbrains.com/list/repo/"

addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.0.0")
