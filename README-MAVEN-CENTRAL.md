# Publishing Gibbon to Maven Central

This guide walks you through the process of publishing the Gibbon library to Maven Central.

## Prerequisites

### 1. Sonatype OSSRH Account Setup

1. **Create a Sonatype JIRA account**: Go to https://issues.sonatype.org/secure/Signup!default.jspa
2. **Create a new project ticket**: 
   - Go to https://issues.sonatype.org/secure/CreateIssue.jspa?issuetype=21&pid=10134
   - Project: Community Support - Open Source Project Repository Hosting (OSSRH)
   - Issue Type: New Project
   - Summary: "Request for io.github.eyuaelberhe group ID"
   - Group Id: `io.github.eyuaelberhe`
   - Project URL: `https://github.com/eyuaelberhe/gibbon`
   - SCM URL: `https://github.com/eyuaelberhe/gibbon.git`
   - Username(s): Your Sonatype username
   - Already Synced to Central: No

3. **Wait for approval**: This usually takes 1-2 business days

### 2. GPG Key Setup

1. **Generate a GPG key** (if you don't have one):
   ```bash
   gpg --gen-key
   ```

2. **List your keys** to get the key ID:
   ```bash
   gpg --list-secret-keys --keyid-format LONG
   ```

3. **Export your public key** to a key server:
   ```bash
   gpg --keyserver keyserver.ubuntu.com --send-keys YOUR_KEY_ID
   gpg --keyserver keys.openpgp.org --send-keys YOUR_KEY_ID
   ```

4. **Update sonatype.sbt** with your key ID:
   Replace `YOUR_GPG_KEY_ID` in `sonatype.sbt` with your actual key ID.

### 3. Credentials Setup

Create `~/.sbt/1.0/sonatype.sbt` with your Sonatype credentials:

```scala
credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "s01.oss.sonatype.org",
  "your-sonatype-username",
  "your-sonatype-password"
)
```

## Publishing Process

### 1. Snapshot Publishing (Testing)

Test the publishing process with a snapshot version:

```bash
sbt publishSigned
```

### 2. Release Publishing

1. **Update version** in `build.sbt` (remove `-SNAPSHOT`):
   ```scala
   ThisBuild / version := "0.1.0"
   ```

2. **Publish and release**:
   ```bash
   sbt publishSigned
   sbt sonatypeBundleRelease
   ```

### 3. Using sbt-release (Recommended)

For automated releases:

```bash
sbt release
```

This will:
- Run tests
- Increment version
- Create git tag
- Publish to Sonatype
- Release to Maven Central

## Verification

After publishing, you can verify your artifacts at:
- Staging: https://s01.oss.sonatype.org/
- Maven Central: https://search.maven.org/

## Usage

Once published, users can add Gibbon to their projects:

### SBT
```scala
libraryDependencies += "io.github.eyuaelberhe" %% "gibbon-core" % "0.1.0"
libraryDependencies += "io.github.eyuaelberhe" %% "gibbon-akka" % "0.1.0"
libraryDependencies += "io.github.eyuaelberhe" %% "gibbon-pekko" % "0.1.0"
```

### Maven
```xml
<dependency>
    <groupId>io.github.eyuaelberhe</groupId>
    <artifactId>gibbon-core_2.13</artifactId>
    <version>0.1.0</version>
</dependency>
```

### Gradle
```gradle
implementation 'io.github.eyuaelberhe:gibbon-core_2.13:0.1.0'
```

## Troubleshooting

### Common Issues

1. **GPG signing fails**: Ensure your GPG key is properly configured and available
2. **Credentials error**: Check your `~/.sbt/1.0/sonatype.sbt` file
3. **Group ID not approved**: Wait for Sonatype JIRA ticket approval
4. **Artifacts not syncing**: It can take up to 2 hours for artifacts to appear on Maven Central

### Useful Commands

- Check staging repositories: `sbt sonatypeList`
- Drop staging repository: `sbt sonatypeDrop`
- Close and release: `sbt sonatypeClose` then `sbt sonatypeRelease`

## Security Notes

- Never commit credentials to version control
- Use environment variables or secure credential storage
- Regularly rotate your Sonatype password
- Keep your GPG key secure and backed up