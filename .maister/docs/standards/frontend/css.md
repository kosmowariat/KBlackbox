## CSS

### Consistent Methodology
Stick to the project's chosen approach (Tailwind, BEM, CSS modules, etc.) across the entire codebase.

### Work With the Framework
Use framework patterns as intended rather than fighting them with excessive overrides.

### Design Tokens
Establish and document consistent values for colors, spacing, and typography.

### Minimize Custom CSS
Prefer framework utilities to reduce custom styling maintenance.

### Production Optimization
Use CSS purging or tree-shaking to remove unused styles.

## Android interpretation (this project)

### Styling methodology
"CSS" means themes, styles, `TextAppearance` styles, and color/dimen resources. Design tokens live in `values/colors.xml`, `values/dimens.xml` and `themes.xml`; no inline styling (literal colors, sizes, text attributes) when a token exists. See `android-resources.md` for the target theme.
