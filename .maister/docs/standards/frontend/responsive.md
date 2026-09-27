## Responsive Design

### Mobile-First
Start with mobile layout and progressively enhance for larger screens.

### Standard Breakpoints
Use consistent breakpoints (mobile, tablet, desktop) across the application.

### Fluid Layouts
Use percentage-based widths and flexible containers that adapt to screen size.

### Relative Units
Prefer rem/em over fixed pixels for better scalability.

### Cross-Device Testing
Test across multiple screen sizes to ensure a balanced experience.

### Touch-Friendly
Size tap targets appropriately (minimum 44x44px) for mobile users.

### Mobile Performance
Optimize images and assets for mobile network conditions.

### Readable Typography
Maintain readable font sizes across all breakpoints.

### Content Priority
Show the most important content first on smaller screens.

## Android interpretation (this project)

### Units and layouts
Use dp for sizes and sp for text (not px); build screens with `ConstraintLayout` so they adapt to screen size.

### Resource qualifiers
Use resource qualifiers (`-land`, `-sw600dp`) for tablet and landscape variants instead of runtime size checks.

### RTL and device testing
Use RTL-safe `start`/`end` attributes instead of `left`/`right`. Test on small phones and on large screens/tablets.
