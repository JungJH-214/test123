import java.net.URLClassLoader
import java.sql.Driver
import java.sql.SQLException
import java.util.Properties

plugins {
    java
    id("org.springframework.boot") version "2.7.18"
    id("io.spring.dependency-management") version "1.0.15.RELEASE"
    id("com.diffplug.spotless") version "6.25.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
    implementation("org.flywaydb:flyway-core")
    implementation("org.springdoc:springdoc-openapi-ui:1.8.0")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("com.h2database:h2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

// pgtest 테스트 DB 격리: git worktree마다 다른 DB를 써서 동시에 테스트해도 서로 데이터를 지우지 않게 한다.
// DB 이름 우선순위: 환경 변수 POSTGRES_TEST_DB > worktree 이름(eduhub_test_<worktree>) > .env > eduhub_test
val repoRoot: File = projectDir.parentFile
val dotEnv = Properties().apply {
    val file = repoRoot.resolve(".env")
    if (file.isFile) file.inputStream().use { load(it) }
}
fun envOrDotEnv(key: String): String? = System.getenv(key) ?: dotEnv.getProperty(key)?.trim()

val worktreeTestDb: String? = repoRoot.takeIf {
    it.parentFile?.name == "worktrees" && it.parentFile.parentFile?.name == ".claude"
}?.let { "eduhub_test_" + it.name.lowercase().replace(Regex("[^a-z0-9_]"), "_") }?.take(63)

val pgTestDb: String = System.getenv("POSTGRES_TEST_DB")
    ?: worktreeTestDb
    ?: dotEnv.getProperty("POSTGRES_TEST_DB")?.trim()
    ?: "eduhub_test"
val pgPort: String = envOrDotEnv("POSTGRES_PORT") ?: "5432"
val pgUser: String? = envOrDotEnv("POSTGRES_USER")
val pgPassword: String? = envOrDotEnv("POSTGRES_PASSWORD")

tasks.withType<Test> {
    useJUnitPlatform()
    environment("POSTGRES_TEST_DB", pgTestDb)

    val pgDriverJars = configurations.testRuntimeClasspath.get().filter { it.name.startsWith("postgresql") }
    doFirst {
        logger.lifecycle("pgtest DB: $pgTestDb")
        if (pgUser == null) {
            logger.warn("POSTGRES_USER가 없어 pgtest DB 자동 생성을 건너뛴다 (.env를 worktree 루트에 두었는지 확인)")
            return@doFirst
        }
        try {
            val loader = URLClassLoader(
                pgDriverJars.files.map { it.toURI().toURL() }.toTypedArray(),
                ClassLoader.getPlatformClassLoader(),
            )
            val driver = loader.loadClass("org.postgresql.Driver")
                .getDeclaredConstructor().newInstance() as Driver
            val props = Properties().apply {
                setProperty("user", pgUser)
                if (pgPassword != null) setProperty("password", pgPassword)
            }
            driver.connect("jdbc:postgresql://localhost:$pgPort/postgres", props).use { conn ->
                val exists = conn.prepareStatement("SELECT 1 FROM pg_database WHERE datname = ?").use { ps ->
                    ps.setString(1, pgTestDb)
                    ps.executeQuery().use { it.next() }
                }
                if (!exists) {
                    try {
                        conn.createStatement().use {
                            it.execute("CREATE DATABASE \"" + pgTestDb.replace("\"", "\"\"") + "\"")
                        }
                        logger.lifecycle("pgtest DB 생성: $pgTestDb")
                    } catch (e: SQLException) {
                        if (e.sqlState != "42P04") throw e // 42P04: 동시에 다른 실행이 먼저 만든 경우
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("pgtest DB 자동 생성 실패 ($pgTestDb): ${e.message}. pgtest 테스트가 실패할 수 있다")
        }
    }
}

spotless {
    encoding("UTF-8")
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}
