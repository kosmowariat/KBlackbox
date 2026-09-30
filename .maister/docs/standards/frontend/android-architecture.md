## Android UI Architecture (app module)

### Package-by-feature
UI code lives in `view/<feature>/` (apps, fake, gms, list, main, setting); shared bases in `view/base/`. A list feature holds `<Feature>Activity|Fragment`, `<Feature>Adapter`, `<Feature>Factory`, `<Feature>ViewModel`.
Non-UI layers: `bean/` (UI models), `data/` (repositories), `util/` (helpers/extensions), `widget/` (custom views), `app/` (Application, managers).

### Class suffixes
Use role suffixes: `*Activity`, `*Fragment`, `*ViewModel`, `*Factory`, `*Adapter`, `*Repository`, `*Bean` (data classes in `bean/`), nested `*VH` view holders, `*Util`/`*Ex` for helpers.

### Views talk to the engine only through repositories
Views and ViewModels must not call `BlackBoxCore`/`B*Manager` directly; go through a repository in `data/`.
Exception: the lifecycle hooks Bcore requires (`BlackBoxCore.get().onBefore/AfterMainActivityOnCreate`, Application attach hooks).
About 18 legacy direct calls exist (AppsFragment, MainActivity, SettingFragment, FakeManagerActivity, ShortcutActivity) — move them into repositories when touching those files.

### ViewModel creation
Current: `ViewModelProvider(this, InjectionUtil.get<Feature>Factory())[XViewModel::class.java]`, with `InjectionUtil` holding singleton repositories and building `*Factory` classes.
New screens follow this until a DI decision is made; prefer the `[ ]` indexer over `.get(...)`.

### ViewModel state exposure (target)
State is exposed as read-only `StateFlow` backed by a private `MutableStateFlow` (a nullable value means "not loaded yet"). Repositories are `suspend` functions that return results and switch to `Dispatchers.IO` themselves. One-off events (messages, navigation, dialogs to open) are a `sealed interface` sent through a `Channel(BUFFERED)` and exposed as `receiveAsFlow()`, so they are delivered once and never replayed. ViewModels start work with `BaseViewModel.launch { }`, which logs uncaught errors. Screens collect with `LifecycleOwner.collectStarted(flow) { }` (`util/FlowEx.kt`, `repeatOnLifecycle(STARTED)`); a Fragment uses `viewLifecycleOwner`. Views never write to ViewModel state.

### Observing state
Observe with the lifecycle-owner lambda: `observe(this) { }` in Activities, `observe(viewLifecycleOwner) { }` in Fragments (or `repeatOnLifecycle` for flows). List screens update the adapter, then switch StateView to content/empty.

### Error handling in UI (K1)
No blanket `try/catch` around whole lifecycle methods or every statement. Catch specific, expected failures at the repository/ViewModel boundary, map them to a UI state or a message from `strings.xml`, and show it to the user (toast/dialog/empty state).
Never silently return an empty view on failure. Log with `Log.e(TAG, "Error <doing X>", e)`.

Source: code (app module analysis), user decisions K1/K3.
