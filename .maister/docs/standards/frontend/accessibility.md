## Accessibility

### Semantic HTML
Use appropriate elements (nav, main, button) that convey meaning to assistive technologies.

### Keyboard Navigation
Make all interactive elements accessible via keyboard with visible focus indicators.

### Color Contrast
Maintain 4.5:1 contrast for normal text; don't rely solely on color to convey information.

### Alt Text and Labels
Provide descriptive alt text for images and labels for form inputs.

### Screen Reader Testing
Verify all views work with screen readers.

### ARIA When Needed
Use ARIA attributes to enhance complex components when semantic HTML isn't enough.

### Heading Structure
Use heading levels (h1-h6) in proper order for clear document outline.

### Focus Management
Manage focus appropriately in dynamic content, modals, and SPAs.

## Android interpretation (this project)

### Content descriptions
Every meaningful `ImageView`/`ImageButton`/FAB gets a `contentDescription` from string resources; decorative images use `@null` or `importantForAccessibility="no"`. No "TODO" descriptions (3 exist today — fix them when touching).

### Touch targets and contrast
Minimum 48dp touch targets. Check the contrast of the theme colors (including dark mode) against the 4.5:1 guideline.

### TalkBack verification
Verify new and changed screens with TalkBack (the Android screen reader) — focus order, labels, and dialog focus.
