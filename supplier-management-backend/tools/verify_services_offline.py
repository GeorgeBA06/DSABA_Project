from pathlib import Path
import subprocess, tempfile

src = Path(__file__).resolve().parents[1] / "src/main/java"
with tempfile.TemporaryDirectory() as tmp:
    t = Path(tmp)

    def put(pkg, cls, body):
        f = t / "stubs" / pkg.replace(".", "/") / (cls + ".java")
        f.parent.mkdir(parents=True, exist_ok=True)
        f.write_text("package " + pkg + ";\n" + body + "\n")

    for x in (
        "Entity",
        "Id",
        "EmbeddedId",
        "Embeddable",
        "Version",
        "PrePersist",
        "PreUpdate",
    ):
        put("jakarta.persistence", x, "public @interface " + x + " {}")
    put("jakarta.persistence", "Table", "public @interface Table { String name(); }")
    put(
        "jakarta.persistence",
        "Column",
        'public @interface Column { String name(); boolean nullable() default true; boolean updatable() default true; int length() default 255; int precision() default 0; int scale() default 0; String columnDefinition() default ""; }',
    )
    put(
        "jakarta.persistence",
        "JoinColumn",
        "public @interface JoinColumn { String name(); boolean nullable() default true; }",
    )
    put(
        "jakarta.persistence",
        "ManyToOne",
        "public @interface ManyToOne { FetchType fetch() default FetchType.EAGER; boolean optional() default true; }",
    )
    put("jakarta.persistence", "FetchType", "public enum FetchType {EAGER,LAZY}")
    put(
        "jakarta.persistence",
        "GeneratedValue",
        "public @interface GeneratedValue { GenerationType strategy(); }",
    )
    put("jakarta.persistence", "GenerationType", "public enum GenerationType {UUID}")
    put(
        "jakarta.persistence",
        "Enumerated",
        "public @interface Enumerated { EnumType value(); }",
    )
    put("jakarta.persistence", "EnumType", "public enum EnumType {STRING}")
    put("jakarta.persistence", "MapsId", "public @interface MapsId { String value(); }")
    put(
        "jakarta.persistence",
        "LockModeType",
        "public enum LockModeType {PESSIMISTIC_WRITE}",
    )
    put(
        "org.hibernate.annotations",
        "JdbcTypeCode",
        "public @interface JdbcTypeCode { int value(); }",
    )
    put(
        "org.hibernate.type",
        "SqlTypes",
        "public class SqlTypes { public static final int JSON = 2016; }",
    )
    put(
        "org.springframework.data.jpa.repository",
        "JpaRepository",
        "public interface JpaRepository<T,I> { T saveAndFlush(T value); T save(T value); java.util.Optional<T> findById(I id); boolean existsById(I id); void flush(); void deleteAll(Iterable<? extends T> e); }",
    )
    put(
        "org.springframework.data.jpa.repository",
        "Lock",
        "public @interface Lock { jakarta.persistence.LockModeType value(); }",
    )
    put(
        "org.springframework.data.jpa.repository",
        "Query",
        "public @interface Query { String value(); }",
    )
    put(
        "org.springframework.data.repository.query",
        "Param",
        "public @interface Param { String value(); }",
    )
    put("org.springframework.data.domain", "Page", "public interface Page<T> { }")
    put("org.springframework.data.domain", "Pageable", "public interface Pageable { }")
    put(
        "org.springframework.boot.autoconfigure",
        "SpringBootApplication",
        "public @interface SpringBootApplication { }",
    )
    put(
        "org.springframework.boot",
        "SpringApplication",
        "public class SpringApplication { public static void run(Class<?> c, String[] args) { } }",
    )
    put(
        "jakarta.persistence",
        "EntityManager",
        "public interface EntityManager { <T> T getReference(Class<T> type, Object id); }",
    )
    put(
        "jakarta.persistence",
        "PersistenceContext",
        "public @interface PersistenceContext { }",
    )
    put("org.springframework.stereotype", "Service", "public @interface Service { }")
    put(
        "org.springframework.transaction.annotation",
        "Isolation",
        "public enum Isolation {DEFAULT,REPEATABLE_READ}",
    )
    put(
        "org.springframework.transaction.annotation",
        "Transactional",
        "public @interface Transactional { boolean readOnly() default false; Isolation isolation() default Isolation.DEFAULT; }",
    )
    put("org.junit.jupiter.api", "Test", "public @interface Test { }")
    put(
        "org.junit.jupiter.api.function",
        "Executable",
        "@FunctionalInterface public interface Executable { void execute() throws Throwable; }",
    )
    put(
        "org.junit.jupiter.api",
        "Assertions",
        "public class Assertions { public static void assertEquals(Object a,Object b) {} public static void assertTrue(boolean b) {} public static void assertNotNull(Object x) {} public static <T extends Throwable> T assertThrows(Class<T> type, org.junit.jupiter.api.function.Executable run){return null;} }",
    )
    put(
        "org.springframework.beans.factory.annotation",
        "Autowired",
        "public @interface Autowired { }",
    )
    put(
        "org.springframework.boot.test.context",
        "SpringBootTest",
        "public @interface SpringBootTest { }",
    )
    put(
        "org.springframework.jdbc.core",
        "JdbcTemplate",
        "public class JdbcTemplate { public <T> java.util.List<T> queryForList(String sql,Class<T> type){return null;} public int update(String sql,Object... args){return 0;} }",
    )
    put(
        "org.springframework.dao",
        "DataAccessException",
        "public class DataAccessException extends RuntimeException { }",
    )
    put(
        "org.springframework.test.context",
        "DynamicPropertyRegistry",
        "public interface DynamicPropertyRegistry { void add(String key,java.util.function.Supplier<?> value); }",
    )
    put(
        "org.springframework.test.context",
        "DynamicPropertySource",
        "public @interface DynamicPropertySource { }",
    )
    put(
        "org.testcontainers.containers",
        "PostgreSQLContainer",
        'public class PostgreSQLContainer<T> { public PostgreSQLContainer(String img) {} public PostgreSQLContainer<T> withDatabaseName(String name){return this;} public PostgreSQLContainer<T> withUsername(String name){return this;} public PostgreSQLContainer<T> withPassword(String name){return this;} public String getJdbcUrl(){return "";} public String getUsername(){return "";} public String getPassword(){return "";} }',
    )
    put(
        "org.testcontainers.junit.jupiter",
        "Container",
        "public @interface Container { }",
    )
    put(
        "org.testcontainers.junit.jupiter",
        "Testcontainers",
        "public @interface Testcontainers { }",
    )
    tests = src.parents[1] / "test/java"
    files = (
        [str(f) for f in src.rglob("*.java")]
        + [str(f) for f in tests.rglob("*.java")]
        + [str(f) for f in (t / "stubs").rglob("*.java")]
        + [str(src.parents[2] / "tools" / "PurePolicyChecks.java")]
    )
    result = subprocess.run(
        ["javac", "--release", "21", "-d", str(t / "classes"), *files],
        capture_output=True,
        text=True,
    )
    print("JAVAC source count:", len(list(src.rglob("*.java"))))
    print("JAVAC exit:", result.returncode)
    if result.returncode:
        print(result.stderr[:10000])
    if result.returncode:
        raise SystemExit(result.returncode)
    run = subprocess.run(
        ["java", "-cp", str(t / "classes"), "PurePolicyChecks"],
        capture_output=True,
        text=True,
    )
    print(run.stdout.strip() or run.stderr.strip())
    raise SystemExit(run.returncode)
