## Hidden-API Reflection (black-reflection)

### Mirror interfaces
Declare each hidden framework class once as a `public interface` in `black.<framework package>` with the framework's simple name, annotated `@BClassName("<fqcn>")`.
Fields: no-arg methods named like the field with `@BField`/`@BStaticField`; methods `@BMethod`/`@BStaticMethod`; constructors `@BConstructor`; hidden param types `@BParamClassName("fqcn") Object`. Inner classes: nested interfaces with `@BClassName("Outer$Inner")`.
SDK/OEM differences get a separate interface with a version suffix (prefer API letters: `Q`, `N`, `NMR1`; legacy names like `Oreo`, `Kitkat` exist). Use meaningful parameter names, not decompiler-style `IBinder0`.

### Use generated BR* accessors
Access hidden APIs via the generated `BR<Class>`: `BRX.get()` (static view), `BRX.get(instance)` (instance view), `_set_<field>(v)`, `_check_<member>()`, `getWithException()`.
Raw `java.lang.reflect`/`Reflector` only as a fallback when no mirror exists.

### Annotation processor
`compiler` is an `annotationProcessor` of Bcore (AutoService + JavaPoet) and generates `XContext`/`XStatic`/`BRX`. Annotations are declared with `@Retention(RUNTIME) @Target({...})` using static imports.

### ProGuard keep rules
Classes/members annotated with black-reflection annotations and the `top.niunaijun.blackbox.**`, `mirror.**`, `android.**` trees are kept in `proguard-rules.pro` and `Bcore/consumer-rules.pro`. New reflection-driven classes outside these trees need matching keep rules.

Source: code (220 mirror interfaces), config (build.gradle, proguard).
