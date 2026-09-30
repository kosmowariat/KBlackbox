## Android Views & Screens

### ViewBinding via `by inflate()`
Activities/Fragments: `private val viewBinding: XxxBinding by inflate()` (`util/ViewBindingEx.kt`), then `setContentView(viewBinding.root)`. RecyclerView holders: `XxxBinding.bind(itemView)`.
Do not use `findViewById`. The field name is `viewBinding`.

### Toolbar
Screens with a toolbar use `<include layout="@layout/view_toolbar" android:id="@+id/toolbar_layout"/>` and call `initToolbar(viewBinding.toolbarLayout.toolbar, R.string.<title>, showBack)` from `BaseActivity` right after `setContentView`.

### Base activities
Toolbar screens extend `BaseActivity`; screens with blocking operations extend `LoadingActivity` (`showLoading()/hideLoading()`). Only trampolines (Welcome, Shortcut) extend `AppCompatActivity` directly.

### Screen structure
Split `onCreate` into private `init*()` helpers (initToolbar, initViewModel, initRecyclerView, initData). Adapter and ViewModel fields: `private lateinit var mAdapter`, `private lateinit var viewModel`.

### Lists (target)
Use `util/BindingAdapter` (ViewBinding holders, DiffUtil updates, in-place `replaceAt` and drag `moveItem`); each screen supplies its adapter as a small factory function next to the screen (`appsAdapter()`, `installedAppsAdapter()`). Read-only lists that never mutate items may use a plain `ListAdapter` + `DiffUtil.ItemCallback` instead. The cbfg `RVAdapter` library has been removed.
Search in a toolbar is a standard `androidx.appcompat.widget.SearchView` declared as `app:actionViewClass` on a menu item.
Set the LayoutManager explicitly (`GridLayoutManager(4)` for the app grid, `LinearLayoutManager` for lists).

### Navigation helpers
Activities opened from elsewhere expose `companion object { fun start(context: Context) }` at the end of the class; fragments with args expose `newInstance(...)` using `bundleOf`.
Intent extra keys must be shared constants (legacy code mixes `"userID"` and `"userId"` literals — unify on one constant when touching).

### Activity results
Use `registerForActivityResult(...)` stored in private vals; return results through a `finishWithResult()` helper (`setResult(RESULT_OK, intent)` + `finish()`). No `onActivityResult` overrides.
Replace deprecated `onBackPressed` overrides with `OnBackPressedDispatcher`.

Source: code (app module analysis), user decision K3.
