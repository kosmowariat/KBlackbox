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
Target: expose read-only state (`StateFlow`/`LiveData`) backed by a private mutable field; repositories are `suspend` functions that return results instead of receiving `MutableLiveData`; one-off events (toasts, navigation) use a single-event mechanism, not nulling LiveData from the view. Run blocking work with `withContext(Dispatchers.IO)`.
Legacy (migrate when touching): public `MutableLiveData` passed into repository methods which `postValue()`; `BaseViewModel.launchOnUI` (actually runs on IO and swallows Throwables); views writing to ViewModel LiveData.

### Observing state
Observe with the lifecycle-owner lambda: `observe(this) { }` in Activities, `observe(viewLifecycleOwner) { }` in Fragments (or `repeatOnLifecycle` for flows). List screens update the adapter, then switch StateView to content/empty.

### Error handling in UI (K1)
No blanket `try/catch` around whole lifecycle methods or every statement. Catch specific, expected failures at the repository/ViewModel boundary, map them to a UI state or a message from `strings.xml`, and show it to the user (toast/dialog/empty state).
Never silently return an empty view on failure. Log with `Log.e(TAG, "Error <doing X>", e)`.

Source: code (app module analysis), user decisions K1/K3.
