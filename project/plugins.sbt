// Plugin for publishing to Sonatype/Maven Central
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "3.11.3")

// Plugin for GPG signing
addSbtPlugin("com.github.sbt" % "sbt-pgp" % "2.2.1")

// Plugin for release management (optional but recommended)
addSbtPlugin("com.github.sbt" % "sbt-release" % "1.1.0")