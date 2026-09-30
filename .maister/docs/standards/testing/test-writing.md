## Test Writing

### Test Behavior
Focus on what code does, not how it does it, to allow safe refactoring.

### Clear Names
Use descriptive names explaining what's tested and expected (`shouldReturnErrorWhenUserNotFound`).

### Mock External Dependencies
Isolate tests by mocking databases, APIs, and external services.

### Fast Execution
Keep unit tests fast (milliseconds) so developers run them frequently.

### Risk-Based Testing
Prioritize testing based on business criticality and likelihood of bugs.

### Balance Coverage and Velocity
Adjust test coverage based on project needs and team workflow.

### Critical Path Focus
Ensure core user workflows and critical business logic are well-tested.

### Appropriate Depth
Match edge case testing to the risk profile of the code.

### Current state (this project)
A small JVM unit-test suite lives in `app/src/test` (`DataDirCopierTest`, `MathUtilTest`) and runs in CI with `:app:testDebugUnitTest`; there are no instrumented tests yet. Tests are encouraged, not required (K5). Pull pure logic out of Android classes (as `DataDirCopier` was) so it can be tested on the JVM.
Put unit tests in `app/src/test/java/top/niunaijun/blackboxa/...` (JUnit 4, already declared) and instrumented/Espresso tests in `app/src/androidTest/...`. Best first targets: ViewModels and repositories.
The engine is verified manually: run the app, filter `adb logcat` by component TAGs, and record results per Android version. `Bcore/.../core/system/JarManagerTest.java` sits in main sources and is not a real test.
