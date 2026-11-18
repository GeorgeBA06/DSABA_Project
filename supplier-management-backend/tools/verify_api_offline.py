from pathlib import Path
import subprocess, tempfile
src=Path(__file__).resolve().parents[1]/'src/main/java'
with tempfile.TemporaryDirectory() as tmp:
    t=Path(tmp)
    def put(pkg,cls,body):
        f=t/'stubs'/pkg.replace('.','/')/(cls+'.java');f.parent.mkdir(parents=True,exist_ok=True)
        f.write_text('package '+pkg+';\n'+body+'\n')
    for x in ('Entity','Id','EmbeddedId','Embeddable','Version','PrePersist','PreUpdate'):
        put('jakarta.persistence',x,'public @interface '+x+' {}')
    put('jakarta.persistence','Table','public @interface Table { String name(); }')
    put('jakarta.persistence','Column','public @interface Column { String name(); boolean nullable() default true; boolean updatable() default true; int length() default 255; int precision() default 0; int scale() default 0; String columnDefinition() default ""; }')
    put('jakarta.persistence','JoinColumn','public @interface JoinColumn { String name(); boolean nullable() default true; }')
    put('jakarta.persistence','ManyToOne','public @interface ManyToOne { FetchType fetch() default FetchType.EAGER; boolean optional() default true; }')
    put('jakarta.persistence','FetchType','public enum FetchType {EAGER,LAZY}')
    put('jakarta.persistence','GeneratedValue','public @interface GeneratedValue { GenerationType strategy(); }')
    put('jakarta.persistence','GenerationType','public enum GenerationType {UUID}')
    put('jakarta.persistence','Enumerated','public @interface Enumerated { EnumType value(); }')
    put('jakarta.persistence','EnumType','public enum EnumType {STRING}')
    put('jakarta.persistence','MapsId','public @interface MapsId { String value(); }')
    put('jakarta.persistence','LockModeType','public enum LockModeType {PESSIMISTIC_WRITE}')
    put('org.hibernate.annotations','JdbcTypeCode','public @interface JdbcTypeCode { int value(); }')
    put('org.hibernate.type','SqlTypes','public class SqlTypes { public static final int JSON = 2016; }')
    put('org.springframework.data.jpa.repository','JpaRepository','public interface JpaRepository<T,I> { T saveAndFlush(T value); T save(T value); java.util.Optional<T> findById(I id); boolean existsById(I id); void flush(); void deleteAll(Iterable<? extends T> e); org.springframework.data.domain.Page<T> findAll(org.springframework.data.domain.Pageable p); long count(); }')
    put('org.springframework.data.jpa.repository','Lock','public @interface Lock { jakarta.persistence.LockModeType value(); }')
    put('org.springframework.data.jpa.repository','Query','public @interface Query { String value(); }')
    put('org.springframework.data.repository.query','Param','public @interface Param { String value(); }')
    put('org.springframework.data.domain','Page','public interface Page<T> { <R> Page<R> map(java.util.function.Function<? super T,? extends R> f); java.util.List<T> getContent(); long getTotalElements(); int getTotalPages(); int getNumber(); int getSize(); boolean isLast(); }')
    put('org.springframework.data.domain','Pageable','public interface Pageable { }')
    put('org.springframework.data.domain','Sort','public class Sort { public static Sort by(String s){return null;} }')
    put('org.springframework.data.domain','PageRequest','public class PageRequest implements Pageable { public static PageRequest of(int p,int s,Sort so){return null;} }')
    put('org.springframework.boot.autoconfigure','SpringBootApplication','public @interface SpringBootApplication { }')
    put('org.springframework.boot','SpringApplication','public class SpringApplication { public static void run(Class<?> c, String[] args) { } }')
    put('jakarta.persistence','EntityManager','public interface EntityManager { <T> T getReference(Class<T> type, Object id); }')
    put('jakarta.persistence','PersistenceContext','public @interface PersistenceContext { }')
    put('org.springframework.stereotype','Service','public @interface Service { }')
    put('org.springframework.transaction.annotation','Isolation','public enum Isolation {DEFAULT,REPEATABLE_READ}')
    put('org.springframework.transaction.annotation','Transactional','public @interface Transactional { boolean readOnly() default false; Isolation isolation() default Isolation.DEFAULT; Class<?>[] noRollbackFor() default {}; }')
    put('org.junit.jupiter.api','Test','public @interface Test { }')
    put('org.junit.jupiter.api.function','Executable','@FunctionalInterface public interface Executable { void execute() throws Throwable; }')
    put('org.junit.jupiter.api','Assertions','public class Assertions { public static void assertEquals(Object a,Object b) {} public static void assertTrue(boolean b) {} public static void assertNotNull(Object x) {} public static <T extends Throwable> T assertThrows(Class<T> type, org.junit.jupiter.api.function.Executable run){return null;} }')
    put('org.springframework.beans.factory.annotation','Autowired','public @interface Autowired { }')
    put('org.springframework.boot.test.context','SpringBootTest','public @interface SpringBootTest { String[] properties() default {}; WebEnvironment webEnvironment() default WebEnvironment.MOCK; enum WebEnvironment { MOCK,RANDOM_PORT } }')
    put('org.springframework.jdbc.core','JdbcTemplate','public class JdbcTemplate { public <T> java.util.List<T> queryForList(String sql,Class<T> type){return null;} public int update(String sql,Object... args){return 0;} }')
    put('org.springframework.dao','DataAccessException','public class DataAccessException extends RuntimeException { }')
    put('org.springframework.test.context','DynamicPropertyRegistry','public interface DynamicPropertyRegistry { void add(String key,java.util.function.Supplier<?> value); }')
    put('org.springframework.test.context','DynamicPropertySource','public @interface DynamicPropertySource { }')
    put('org.testcontainers.containers','PostgreSQLContainer','public class PostgreSQLContainer<T> { public PostgreSQLContainer(String img) {} public PostgreSQLContainer<T> withDatabaseName(String name){return this;} public PostgreSQLContainer<T> withUsername(String name){return this;} public PostgreSQLContainer<T> withPassword(String name){return this;} public String getJdbcUrl(){return "";} public String getUsername(){return "";} public String getPassword(){return "";} }')
    put('org.testcontainers.junit.jupiter','Container','public @interface Container { }')
    put('org.testcontainers.junit.jupiter','Testcontainers','public @interface Testcontainers { }')

    # HTTP, validation, and Spring configuration API surface (compilation-only stubs).
    for annotation in ('Valid',):
        put('jakarta.validation',annotation,'@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE_USE,java.lang.annotation.ElementType.PARAMETER,java.lang.annotation.ElementType.FIELD}) public @interface '+annotation+' {}')
    for annotation in ('NotBlank','NotNull','NotEmpty','Email'):
        put('jakarta.validation.constraints',annotation,'@java.lang.annotation.Target({java.lang.annotation.ElementType.TYPE_USE,java.lang.annotation.ElementType.PARAMETER,java.lang.annotation.ElementType.FIELD}) public @interface '+annotation+' {}')
    for annotation in ('Min','Max'):
        put('jakarta.validation.constraints',annotation,'public @interface '+annotation+' { long value(); }')
    put('jakarta.validation.constraints','DecimalMin','public @interface DecimalMin { String value(); }')
    put('org.springframework.web.bind.annotation','RestController','public @interface RestController {}')
    put('org.springframework.web.bind.annotation','RestControllerAdvice','public @interface RestControllerAdvice {}')
    for annotation in ('RequestMapping','GetMapping','PostMapping','PutMapping','PatchMapping','DeleteMapping'):
        put('org.springframework.web.bind.annotation',annotation,'public @interface '+annotation+' { String[] value() default {}; String[] produces() default {}; }')
    put('org.springframework.web.bind.annotation','RequestBody','public @interface RequestBody { boolean required() default true; }')
    put('org.springframework.web.bind.annotation','PathVariable','public @interface PathVariable { String value() default ""; }')
    put('org.springframework.web.bind.annotation','RequestParam','public @interface RequestParam { String value() default ""; String defaultValue() default ""; }')
    put('org.springframework.web.bind.annotation','ResponseStatus','public @interface ResponseStatus { org.springframework.http.HttpStatus value(); }')
    put('org.springframework.web.bind.annotation','ExceptionHandler','public @interface ExceptionHandler { Class<?>[] value(); }')
    put('org.springframework.web.bind','MethodArgumentNotValidException','public class MethodArgumentNotValidException extends Exception {}')
    put('org.springframework.http.converter','HttpMessageNotReadableException','public class HttpMessageNotReadableException extends Exception {}')
    put('org.springframework.dao','OptimisticLockingFailureException','public class OptimisticLockingFailureException extends DataAccessException {}')
    put('org.springframework.web.method.annotation','MethodArgumentTypeMismatchException','public class MethodArgumentTypeMismatchException extends RuntimeException {}')
    put('jakarta.validation','ConstraintViolationException','public class ConstraintViolationException extends RuntimeException {}')
    put('org.springframework.dao','DataIntegrityViolationException','public class DataIntegrityViolationException extends DataAccessException {}')
    put('org.springframework.http','HttpStatus','public enum HttpStatus {OK,CREATED,NO_CONTENT,BAD_REQUEST,CONFLICT,UNAUTHORIZED,FORBIDDEN,UNPROCESSABLE_ENTITY,INTERNAL_SERVER_ERROR}')
    put('org.springframework.http','HttpMethod','public enum HttpMethod { POST,GET,OPTIONS }')
    put('org.springframework.http','HttpHeaders','public class HttpHeaders { public static final String CONTENT_DISPOSITION="Content-Disposition"; }')
    put('org.springframework.http','MediaType','public class MediaType { public static MediaType parseMediaType(String v){return null;} }')
    put('org.springframework.http','ResponseEntity','public class ResponseEntity<T> {public static <T> BodyBuilder<T> status(HttpStatus h){return new BodyBuilder<>();} public static BodyBuilder<byte[]> ok(){return new BodyBuilder<>();} public static class BodyBuilder<T> { public BodyBuilder<T> header(String a,String b){return this;} public BodyBuilder<T> contentType(MediaType m){return this;} public <X> ResponseEntity<X> body(X data){return new ResponseEntity<>();} } }')
    put('org.springframework.context.annotation','Configuration','public @interface Configuration {}')
    put('org.springframework.context.annotation','Bean','public @interface Bean {}')
    put('org.springframework.beans.factory.annotation','Value','public @interface Value {String value();}')
    put('org.springframework.security.config.annotation.method.configuration','EnableMethodSecurity','public @interface EnableMethodSecurity {}')
    put('org.springframework.security.access.prepost','PreAuthorize','public @interface PreAuthorize {String value();}')
    put('org.springframework.security.access','AccessDeniedException','public class AccessDeniedException extends RuntimeException { public AccessDeniedException(String m){super(m);} }')
    put('org.springframework.security.core','Authentication','public interface Authentication { String getName(); boolean isAuthenticated(); }')
    put('org.springframework.security.core.authority','SimpleGrantedAuthority','public class SimpleGrantedAuthority {public SimpleGrantedAuthority(String v){} }')
    put('org.springframework.security.authentication','BadCredentialsException','public class BadCredentialsException extends RuntimeException { public BadCredentialsException(String m){super(m);} }')
    put('org.springframework.security.authentication','AbstractAuthenticationToken','public abstract class AbstractAuthenticationToken implements org.springframework.security.core.Authentication {}')
    put('org.springframework.security.crypto.password','PasswordEncoder','public interface PasswordEncoder { String encode(CharSequence a); boolean matches(CharSequence a, String b); }')
    put('org.springframework.security.crypto.bcrypt','BCryptPasswordEncoder','public class BCryptPasswordEncoder implements org.springframework.security.crypto.password.PasswordEncoder {public BCryptPasswordEncoder(int strength){} public String encode(CharSequence a){return "";} public boolean matches(CharSequence a,String b){return false;} }')
    put('org.springframework.security.oauth2.jose.jws','SignatureAlgorithm','public enum SignatureAlgorithm {RS256}')
    put('org.springframework.security.oauth2.jwt','Jwt','public class Jwt { public String getSubject(){return "";} public String getClaimAsString(String s){return "";} public java.util.List<String> getAudience(){return java.util.List.of();} public String getTokenValue(){return "";} public java.time.Instant getExpiresAt(){return java.time.Instant.now().plusSeconds(60);} }')
    put('org.springframework.security.oauth2.jwt','JwtEncoder','public interface JwtEncoder {Jwt encode(JwtEncoderParameters p);}')
    put('org.springframework.security.oauth2.jwt','JwtDecoder','public interface JwtDecoder { Jwt decode(String token); }')
    put('org.springframework.security.oauth2.jwt','JwtEncoderParameters','public class JwtEncoderParameters {public static JwtEncoderParameters from(JwsHeader h,JwtClaimsSet c){return null;} }')
    put('org.springframework.security.oauth2.jwt','JwsHeader','public class JwsHeader {public static Builder with(org.springframework.security.oauth2.jose.jws.SignatureAlgorithm alg){return null;} public static class Builder{public JwsHeader build(){return null;}} }')
    put('org.springframework.security.oauth2.jwt','JwtClaimsSet','public class JwtClaimsSet {public static Builder builder(){return null;} public static class Builder{ public Builder issuer(String s){return this;} public Builder subject(String s){return this;} public Builder audience(java.util.List<String> s){return this;} public Builder issuedAt(java.time.Instant t){return this;} public Builder notBefore(java.time.Instant t){return this;} public Builder expiresAt(java.time.Instant t){return this;} public Builder id(String s){return this;} public Builder claim(String k,Object v){return this;} public JwtClaimsSet build(){return null;} }}')
    put('org.springframework.security.oauth2.jwt','JwtValidators','public class JwtValidators {public static org.springframework.security.oauth2.core.OAuth2TokenValidator<Jwt> createDefaultWithIssuer(String issuer){return null;} }')
    put('org.springframework.security.oauth2.jwt','NimbusJwtDecoder','public class NimbusJwtDecoder implements JwtDecoder {public static Builder withPublicKey(java.security.interfaces.RSAPublicKey key){return null;} public void setJwtValidator(org.springframework.security.oauth2.core.OAuth2TokenValidator<Jwt> validator){} public Jwt decode(String value){return null;} public static class Builder{public NimbusJwtDecoder build(){return null;}} }')
    put('org.springframework.security.oauth2.jwt','NimbusJwtEncoder','public class NimbusJwtEncoder implements JwtEncoder {public NimbusJwtEncoder(com.nimbusds.jose.jwk.source.ImmutableJWKSet<?> set){} public Jwt encode(JwtEncoderParameters p){return null;} }')
    put('org.springframework.security.oauth2.core','OAuth2Error','public class OAuth2Error {public OAuth2Error(String s,String d,String uri){} }')
    put('org.springframework.security.oauth2.core','OAuth2TokenValidator','public interface OAuth2TokenValidator<T> { OAuth2TokenValidatorResult validate(T token); }')
    put('org.springframework.security.oauth2.core','OAuth2TokenValidatorResult','public class OAuth2TokenValidatorResult {public static OAuth2TokenValidatorResult success(){return null;} public static OAuth2TokenValidatorResult failure(OAuth2Error e){return null;} }')
    put('org.springframework.security.oauth2.core','DelegatingOAuth2TokenValidator','public class DelegatingOAuth2TokenValidator<T> implements OAuth2TokenValidator<T> { @SafeVarargs public DelegatingOAuth2TokenValidator(OAuth2TokenValidator<T>... validators){} public OAuth2TokenValidatorResult validate(T token){return null;} }')
    put('org.springframework.security.oauth2.server.resource.authentication','JwtAuthenticationToken','public class JwtAuthenticationToken extends org.springframework.security.authentication.AbstractAuthenticationToken {public JwtAuthenticationToken(org.springframework.security.oauth2.jwt.Jwt jwt,java.util.List<org.springframework.security.core.authority.SimpleGrantedAuthority> roles,String name){} public org.springframework.security.oauth2.jwt.Jwt getToken(){return null;} public String getName(){return "";} public boolean isAuthenticated(){return true;} }')
    put('org.springframework.core.convert.converter','Converter','public interface Converter<S,T> {T convert(S s);}')
    put('com.nimbusds.jose.jwk','JWKSet','public class JWKSet { public JWKSet(RSAKey key){} }')
    put('com.nimbusds.jose.jwk','RSAKey','public class RSAKey {public static class Builder{ public Builder(java.security.interfaces.RSAPublicKey k){} public Builder privateKey(java.security.interfaces.RSAPrivateKey k){return this;} public Builder keyID(String s){return this;} public RSAKey build(){return null;} }}')
    put('com.nimbusds.jose.jwk.source','ImmutableJWKSet','public class ImmutableJWKSet<T> {public ImmutableJWKSet(com.nimbusds.jose.jwk.JWKSet j){} }')
    put('org.springframework.boot','ApplicationRunner','public interface ApplicationRunner {void run(ApplicationArguments arguments) throws Exception;}')
    put('org.springframework.boot','ApplicationArguments','public interface ApplicationArguments {}')
    put('org.springframework.stereotype','Component','public @interface Component {}')
    put('org.springframework.security.config.http','SessionCreationPolicy','public enum SessionCreationPolicy {STATELESS}')
    put('org.springframework.web.cors','CorsConfigurationSource','public interface CorsConfigurationSource {}')
    put('org.springframework.web.cors','CorsConfiguration','public class CorsConfiguration {public void setAllowedOrigins(java.util.List<String> a){} public void setAllowedMethods(java.util.List<String> a){} public void setAllowedHeaders(java.util.List<String> a){} public void setExposedHeaders(java.util.List<String> a){} public void setAllowCredentials(boolean v){} public void setMaxAge(long v){} }')
    put('org.springframework.web.cors','UrlBasedCorsConfigurationSource','public class UrlBasedCorsConfigurationSource implements CorsConfigurationSource { public void registerCorsConfiguration(String s,CorsConfiguration c){} }')
    put('org.springframework.security.web','SecurityFilterChain','public interface SecurityFilterChain {}')
    put('org.springframework.security.config.annotation.web.builders','HttpSecurity',"""public class HttpSecurity {
      public HttpSecurity csrf(java.util.function.Consumer<Dummy> f){return this;} public HttpSecurity cors(java.util.function.Consumer<Dummy> f){return this;}
      public HttpSecurity httpBasic(java.util.function.Consumer<Dummy> f){return this;}
      public HttpSecurity formLogin(java.util.function.Consumer<Dummy> f){return this;}
      public HttpSecurity sessionManagement(java.util.function.Consumer<Dummy> f){return this;}
      public HttpSecurity authorizeHttpRequests(java.util.function.Consumer<Dummy> f){return this;}
      public HttpSecurity oauth2ResourceServer(java.util.function.Consumer<Dummy> f){return this;}
      public HttpSecurity exceptionHandling(java.util.function.Consumer<Dummy> f){return this;}
      public org.springframework.security.web.SecurityFilterChain build(){return null;}
      public static class Dummy {
        public Dummy disable(){return this;} public Dummy sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy v){return this;}
        public Dummy requestMatchers(Object... v){return this;} public Dummy permitAll(){return this;} public Dummy authenticated(){return this;}
        public Dummy anyRequest(){return this;} public Dummy jwt(java.util.function.Consumer<Dummy> c){return this;}
        public Dummy jwtAuthenticationConverter(org.springframework.core.convert.converter.Converter<?,?> c){return this;}
        public Dummy authenticationEntryPoint(EntryPoint p){return this;} public Dummy accessDeniedHandler(Denied p){return this;}
      }
      public interface EntryPoint {void commence(Req req,Res res,Object error) throws Exception;}
      public interface Denied {void handle(Req req,Res res,Object error) throws Exception;}
      public interface Req {} public interface Res {void sendError(int status,String message) throws Exception;}
    }""")

    put('org.springframework.boot.test.web.server','LocalServerPort','public @interface LocalServerPort {}')
    put('com.fasterxml.jackson.databind','JsonNode','public class JsonNode {public JsonNode get(String key){return null;} public JsonNode get(int i){return null;} public String asText(){return null;} }')
    put('com.fasterxml.jackson.databind','ObjectMapper','public class ObjectMapper {public byte[] writeValueAsBytes(Object value){return new byte[0];} public JsonNode readTree(byte[] bytes){return null;} }')
    tests=src.parents[1]/'test/java'
    files=[str(f) for f in src.rglob('*.java')]+[str(f) for f in tests.rglob('*.java')]+[str(f) for f in (t/'stubs').rglob('*.java')]+[str(src.parents[2]/'tools'/'PurePolicyChecks.java')]
    result=subprocess.run(['javac','--release','21','-d',str(t/'classes'),*files],capture_output=True,text=True)
    print('JAVAC source count:',len(list(src.rglob('*.java'))))
    print('JAVAC exit:',result.returncode)
    if result.returncode: print(result.stderr[:10000])
    if result.returncode: raise SystemExit(result.returncode)
    run=subprocess.run(['java','-cp',str(t/'classes'),'PurePolicyChecks'],capture_output=True,text=True)
    print(run.stdout.strip() or run.stderr.strip())
    raise SystemExit(run.returncode)
