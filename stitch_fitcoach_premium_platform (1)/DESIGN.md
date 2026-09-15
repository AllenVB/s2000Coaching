---
name: Obsidian Kinetic
colors:
  surface: '#111319'
  surface-dim: '#111319'
  surface-bright: '#37393f'
  surface-container-lowest: '#0c0e13'
  surface-container-low: '#191b21'
  surface-container: '#1e1f25'
  surface-container-high: '#282a30'
  surface-container-highest: '#33353a'
  on-surface: '#e2e2e9'
  on-surface-variant: '#bbcabf'
  inverse-surface: '#e2e2e9'
  inverse-on-surface: '#2e3036'
  outline: '#86948a'
  outline-variant: '#3c4a42'
  surface-tint: '#4edea3'
  primary: '#4edea3'
  on-primary: '#003824'
  primary-container: '#10b981'
  on-primary-container: '#00422b'
  inverse-primary: '#006c49'
  secondary: '#94de2d'
  on-secondary: '#1f3700'
  secondary-container: '#7ac100'
  on-secondary-container: '#2c4900'
  tertiary: '#95d3ba'
  on-tertiary: '#003829'
  tertiary-container: '#71af97'
  on-tertiary-container: '#004231'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#6ffbbe'
  primary-fixed-dim: '#4edea3'
  on-primary-fixed: '#002113'
  on-primary-fixed-variant: '#005236'
  secondary-fixed: '#acf847'
  secondary-fixed-dim: '#91db2a'
  on-secondary-fixed: '#102000'
  on-secondary-fixed-variant: '#304f00'
  tertiary-fixed: '#b0f0d6'
  tertiary-fixed-dim: '#95d3ba'
  on-tertiary-fixed: '#002117'
  on-tertiary-fixed-variant: '#0b513d'
  background: '#111319'
  on-background: '#e2e2e9'
  surface-variant: '#33353a'
typography:
  display-xl:
    fontFamily: Montserrat
    fontSize: 56px
    fontWeight: '900'
    lineHeight: 64px
    letterSpacing: -0.03em
  display-xl-mobile:
    fontFamily: Montserrat
    fontSize: 38px
    fontWeight: '900'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Montserrat
    fontSize: 36px
    fontWeight: '800'
    lineHeight: 44px
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Montserrat
    fontSize: 28px
    fontWeight: '800'
    lineHeight: 34px
    letterSpacing: -0.01em
  headline-md:
    fontFamily: Montserrat
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Montserrat
    fontSize: 20px
    fontWeight: '700'
    lineHeight: 28px
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Inter
    fontSize: 15px
    fontWeight: '400'
    lineHeight: 24px
  body-sm:
    fontFamily: Inter
    fontSize: 13px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Montserrat
    fontSize: 14px
    fontWeight: '700'
    lineHeight: 18px
    letterSpacing: 0.06em
  label-md:
    fontFamily: Montserrat
    fontSize: 12px
    fontWeight: '700'
    lineHeight: 16px
    letterSpacing: 0.08em
  label-sm:
    fontFamily: Montserrat
    fontSize: 10px
    fontWeight: '700'
    lineHeight: 14px
    letterSpacing: 0.1em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  gutter: 1.5rem
  gutter-sm: 1rem
  margin: 3rem
  margin-sm: 1.25rem
  space-xs: 0.25rem
  space-sm: 0.5rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2.5rem
  space-2xl: 4rem
---

## Brand & Style

This design system targets high-performance athletes, elite trainers, and premium fitness enthusiasts who demand precision, velocity, and uncompromising quality. The visual style merges High-Contrast Athletic Boldness with Tactical Glassmorphism.

The core visual signature relies on a pitch-black, light-absorbing obsidian environment pierced by high-luminance emerald neon and vivid lime highlights. The interface evokes the atmosphere of an elite human performance lab at night: technical, hyper-focused, and aggressive yet meticulously controlled. Glass surfaces with subtle inner borders provide tactile depth, while uppercase geometric headers command attention and project authority.

## Colors

The palette is engineered around pure visual punch against an ultra-deep canvas:

- **Canvas & Obsidian Bases**:
  - `canvas-default`: `#080a0f` (deepest void, grounds full-bleed viewports)
  - `surface-elevated`: `#0f131a` (opaque base for interactive panels)
  - `surface-glass`: `rgba(15, 19, 26, 0.75)` (standard backdrop-filtered panels)
  - `border-glass`: `rgba(255, 255, 255, 0.08)` (subtle structural separation)
  - `border-active`: `rgba(16, 185, 129, 0.35)` (emerald-charged container edges)

- **Kinetic Accents**:
  - **Primary (`#10b981`)**: Electric Emerald. Primary interactive focal points, metric completion gauges, and vital triggers.
  - **Secondary (`#84cc16`)**: Hyper Lime. Velocity indicators, dynamic badges, peak strain scores, and conversion highlights.
  - **Tertiary (`#064e3b`)**: Deep Pine Shadow. Subdued fills, active state backgrounds, and container depth anchors.

- **Data & Functional Status**:
  - `critical`: `#ef4444` (zone failures, overtraining alerts)
  - `warning`: `#f59e0b` (stamina thresholds)
  - `info`: `#06b6d4` (recovery & biometrics)

Never wash out surfaces with generic gray overlays; tint all neutral layers with a faint navy-obsidian undertone.

## Typography

Typography delivers a high-tension balance between raw momentum and scientific readability. 

- **Display & Headlines (`Montserrat`)**: Rendered with heavier weights (`700` through `900`) and tight letter-spacing to build imposing, modern structural blocks. Key metrics, KPI banners, and section titles utilize uppercase styling via label tokens to reinforce an athletic, competitive tone.
- **Body & Numerical Data (`Inter`)**: Neutral, highly legible, and optimized for high-density tracking tables, workout logs, and training regimes. High-contrast white (`#f8fafc`) and muted slate (`#94a3b8`) maintain strict accessibility across dark canvas backgrounds.

## Layout & Spacing

The layout is built on a responsive 12-column grid system calibrated to maximize dashboard real-estate while maintaining airy breathing space for high-impact promotional blocks:

- **Breakpoints**:
  - Mobile (`< 768px`): 4 columns, `margin-sm` (1.25rem / 20px), `gutter-sm` (1rem / 16px).
  - Tablet (`768px - 1024px`): 8 columns, `margin` (2rem / 32px), `gutter` (1.5rem / 24px).
  - Desktop (`> 1024px`): 12 columns, `margin` (3rem / 48px), `gutter` (1.5rem / 24px). Maximum container width bounded to `1440px`.

Vertical rhythms prioritize modular micro-rhythms (`space-sm` and `space-md` for form fields and control clusters) offset by cinematic voids (`space-xl` and `space-2xl`) separating distinct workout blocks or conversion stages.

## Elevation & Depth

Visual depth combines frosted obsidian glassmorphism with vivid chromatic glows. Traditional diffuse drop shadows are replaced by localized, luminous halos that make active elements appear energised from within:

- **Level 0 (Canvas Ground)**: Deep `#080a0f`. No blur, zero shadow.
- **Level 1 (Card & Module Shells)**: Surface `#0f131a` at 80% opacity, `backdrop-filter: blur(16px)`. Border: 1px solid `rgba(255, 255, 255, 0.07)`. Subtle top-edge inset highlight: `inset 0 1px 0 0 rgba(255, 255, 255, 0.1)`.
- **Level 2 (Active / Hover States)**: Border switches to `rgba(16, 185, 129, 0.4)`. Box shadow: `0 12px 32px -8px rgba(0, 0, 0, 0.7), 0 0 20px -4px rgba(16, 185, 129, 0.25)`.
- **Level 3 (Modals, Overlays, Floating Trays)**: Surface `rgba(15, 19, 26, 0.95)`, `backdrop-filter: blur(24px)`. Border: 1px solid `rgba(255, 255, 255, 0.12)`. Shadow: `0 24px 60px -12px rgba(0, 0, 0, 0.85), 0 0 40px -8px rgba(16, 185, 129, 0.2)`.

## Shapes

With `roundedness: 2`, structural surfaces utilize a controlled 0.5rem (8px) corner radius, delivering an engineered, machine-calibrated silhouette rather than an overly bubbly aesthetic. 

- **Containers, Panels & Cards**: `rounded-md` (0.5rem / 8px) creates clean structural framing.
- **Hero Containers & Major Groupings**: `rounded-lg` (1rem / 16px).
- **Interactive Badges, Metric Chips & Pills**: Full pill profiles (`rounded-full`) reserved exclusively for quick-scan labels and dynamic biometric status indicators.

## Components

- **Buttons**:
  - *Primary Kinetic*: Filled with neon emerald (`#10b981`), text in pure obsidian (`#080a0f`, Montserrat bold uppercase). Subtle outer glow: `box-shadow: 0 0 24px rgba(16, 185, 129, 0.35)`. On hover, scale 1.02 and shift background to vivid lime (`#84cc16`).
  - *Secondary Glass*: Transparent dark fill (`rgba(255, 255, 255, 0.04)`), 1px solid `rgba(255, 255, 255, 0.15)`, text in `#f8fafc`. Hover triggers emerald border and text glow.
- **Glassmorphic Cards**:
  - Translucent slate-obsidian shell (`rgba(15, 19, 26, 0.75)`), 16px blur, 1px perimeter border (`rgba(255, 255, 255, 0.08)`). Header zones feature crisp divider rules.
- **Data & Metric Chips**:
  - Compact padding (`0.25rem 0.75rem`), rounded full pill, font `label-sm`. Background: `rgba(16, 185, 129, 0.1)`, text: `#10b981`, border: `1px solid rgba(16, 185, 129, 0.25)`.
- **Inputs & Form Controls**:
  - Height 48px, background `rgba(8, 10, 15, 0.8)`, border `1px solid rgba(255, 255, 255, 0.1)`. Focus state: border turns `#10b981` with ring `0 0 0 3px rgba(16, 185, 129, 0.2)`. Placeholder text in muted slate (`#64748b`).
- **Checkboxes & Radios**:
  - 20px geometric boxes with `rounded-sm` (4px). Unchecked: `border: 1px solid rgba(255, 255, 255, 0.2)`. Checked: filled with `#10b981`, bold dark checkmark icon, vibrant emerald back-glow.
- **Lists & Metric Feeds**:
  - Borderless list rows with subtle bottom borders (`1px solid rgba(255, 255, 255, 0.04)`). Row hover triggers left-edge 2px neon bar in Hyper Lime (`#84cc16`).
- **Performance Gauges & Progress Bars**:
  - Height 6px to 10px track in deep slate (`#1e293b`). Filled with linear gradient from Emerald (`#10b981`) to Lime (`#84cc16`), terminating in a high-intensity glow head.