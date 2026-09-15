---
name: Apex Athletic Performance
colors:
  surface: '#121318'
  surface-dim: '#121318'
  surface-bright: '#38393f'
  surface-container-lowest: '#0d0e13'
  surface-container-low: '#1a1b21'
  surface-container: '#1e1f25'
  surface-container-high: '#292a2f'
  surface-container-highest: '#34343a'
  on-surface: '#e3e1e9'
  on-surface-variant: '#bbcabf'
  inverse-surface: '#e3e1e9'
  inverse-on-surface: '#2f3036'
  outline: '#86948a'
  outline-variant: '#3c4a42'
  surface-tint: '#4edea3'
  primary: '#4edea3'
  on-primary: '#003824'
  primary-container: '#10b981'
  on-primary-container: '#00422b'
  inverse-primary: '#006c49'
  secondary: '#bcc7de'
  on-secondary: '#263143'
  secondary-container: '#3e495d'
  on-secondary-container: '#aeb9d0'
  tertiary: '#ffb3af'
  on-tertiary: '#650911'
  tertiary-container: '#fc7c78'
  on-tertiary-container: '#711419'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#6ffbbe'
  primary-fixed-dim: '#4edea3'
  on-primary-fixed: '#002113'
  on-primary-fixed-variant: '#005236'
  secondary-fixed: '#d8e3fb'
  secondary-fixed-dim: '#bcc7de'
  on-secondary-fixed: '#111c2d'
  on-secondary-fixed-variant: '#3c475a'
  tertiary-fixed: '#ffdad7'
  tertiary-fixed-dim: '#ffb3af'
  on-tertiary-fixed: '#410005'
  on-tertiary-fixed-variant: '#842225'
  background: '#121318'
  on-background: '#e3e1e9'
  surface-variant: '#34343a'
  surface-obsidian: '#090A0F'
  surface-card: '#12141A'
  surface-elevated: '#1A1D24'
  surface-overlay: '#222731'
  border-subtle: rgba(255, 255, 255, 0.08)
  border-active: rgba(16, 185, 129, 0.4)
  text-primary: '#FFFFFF'
  text-secondary: '#94A3B8'
  text-muted: '#64748B'
  status-danger: '#EF4444'
  status-warning: '#F59E0B'
  accent-emerald-glow: rgba(16, 185, 129, 0.15)
typography:
  display:
    fontFamily: Inter
    fontSize: 3.5rem
    fontWeight: '800'
    lineHeight: '1.1'
    letterSpacing: -0.03em
  display-mobile:
    fontFamily: Inter
    fontSize: 2.5rem
    fontWeight: '800'
    lineHeight: '1.15'
    letterSpacing: -0.025em
  headline-lg:
    fontFamily: Inter
    fontSize: 2.25rem
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.02em
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 1.75rem
    fontWeight: '700'
    lineHeight: '1.25'
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 1.5rem
    fontWeight: '600'
    lineHeight: '1.3'
    letterSpacing: -0.015em
  headline-sm:
    fontFamily: Inter
    fontSize: 1.25rem
    fontWeight: '600'
    lineHeight: '1.35'
    letterSpacing: -0.01em
  body-lg:
    fontFamily: Inter
    fontSize: 1.125rem
    fontWeight: '400'
    lineHeight: '1.6'
    letterSpacing: '0'
  body-md:
    fontFamily: Inter
    fontSize: 1rem
    fontWeight: '400'
    lineHeight: '1.5'
    letterSpacing: '0'
  body-sm:
    fontFamily: Inter
    fontSize: 0.875rem
    fontWeight: '400'
    lineHeight: '1.45'
    letterSpacing: '0'
  label-md:
    fontFamily: Inter
    fontSize: 0.875rem
    fontWeight: '600'
    lineHeight: '1'
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 0.75rem
    fontWeight: '700'
    lineHeight: '1'
    letterSpacing: 0.05em
  metric-num:
    fontFamily: Inter
    fontSize: 2rem
    fontWeight: '700'
    lineHeight: '1'
    letterSpacing: -0.02em
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
  margin: 2rem
  margin-sm: 1rem
  space-2xs: 0.25rem
  space-xs: 0.5rem
  space-sm: 0.75rem
  space-md: 1rem
  space-lg: 1.5rem
  space-xl: 2rem
  space-2xl: 3rem
  space-3xl: 4.5rem
---

## Brand & Style

This design system delivers a high-precision, digital-first performance training atmosphere. It merges the surgical clarity of elite software tools with the aspirational energy of luxury athletic conditioning. The tone is authoritative, discipline-driven, and meticulously engineered, avoiding cheap motivational clichés in favor of performance telemetry, clarity, and undeniable competence.

The aesthetic philosophy draws upon Modern Minimalist Tech with subtle tactile glassmorphic refinement:
- **Obsidian Foundations:** Deep, non-distracting dark surfaces create an immersive training cockpit where biometric data and progression milestones command attention.
- **Surgical Precision:** High contrast, legible typography, razor-clean dividers, and controlled kinetic indicators replicate the instrumentation of medical-grade athletic telemetry.
- **Electric Focus:** Pure, high-intensity emerald green highlights highlight critical progress metrics, active workout states, and high-conversion transaction touchpoints without visual fatigue.

## Colors

The palette operates under a strict dark-mode architecture, prioritizing functional contrast and biometric readability:

- **Primary (`#10B981`):** The kinetic emerald engine. Reserved strictly for primary calls-to-action, personal records (PRs), live active workout states, and critical completion states. Never applied across expansive non-interactive surfaces.
- **Secondary (`#1E293B`):** Deep slate foundation. Used for secondary interface controls, inactive indicators, badge containers, and segmented controller tracks.
- **Neutral / Background Surfaces:**
  - `surface-obsidian` (`#090A0F`): Deep base canvas background.
  - `surface-card` (`#12141A`): Primary content cards, telemetry panels, and list groups.
  - `surface-elevated` (`#1A1D24`): Popovers, sticky action bars, and modal sheets.
- **Typography & Details:** Pure white (`#FFFFFF`) commands top hierarchy, supported by `#94A3B8` for secondary captions and metrics, framed by an ultra-thin hairline border (`rgba(255, 255, 255, 0.08)`).

## Typography

Typography relies entirely on **Inter** configured with tabular numeric features (`tnum`) for workout metrics, countdown timers, weight progression tracks, and micro-analytics.

- **Headlines & Display:** Tightly tracked headings (`-0.02em` to `-0.03em`) generate high visual tension and athletic impact.
- **Labels & Micro-Data:** Set in semi-bold and bold, with uppercase transformations applied specifically to `label-sm` to identify categorical tags, sets, reps, and macro targets.
- **Tabular Numerics:** All dynamic telemetry (heart rate, load in kilograms/pounds, RPE scores, caloric intakes) must enforce fixed-width figures to prevent jitter during real-time tracking loops.

## Layout & Spacing

The spatial architecture is calibrated to an 8px base rhythm, prioritizing data scanning speed and confident margins:

- **Grid Systems:**
  - **Desktop (1024px+):** 12-column grid, max container width of 1280px, 24px gutters (`1.5rem`), and 32px canvas margins (`2rem`).
  - **Tablet (768px - 1023px):** 8-column layout, 16px gutters, and 24px margins.
  - **Mobile (< 767px):** 4-column layout, 16px gutters, and 16px screen edge padding to maximize interactive area on athletic coaching dashboards and workout execution views.
- **Vertical Hierarchy:** Section separation utilizes `space-2xl` and `space-3xl` to preserve distinct mental models between training blocks, nutrition tracking, and messaging feeds.

## Elevation & Depth

Depth is conveyed through tonal surface layering, low-contrast hairline borders, and targeted ambient backdrops rather than muddy drop shadows:

- **Layer 0 (Base Canvas):** `#090A0F` flat background.
- **Layer 1 (Card & Module Surfaces):** `#12141A` with a crisp outline of `1px solid rgba(255, 255, 255, 0.08)`. No traditional shadow; boundary definition is structural.
- **Layer 2 (Overlays, Floating Action Bars, Drawer Navigation):** `#1A1D24` combined with a subtle frosted glass backdrop filter (`backdrop-filter: blur(12px)`) and `rgba(0, 0, 0, 0.4)` ambient drop shadow (`0 8px 32px`).
- **Kinetic Accent Glow:** Active and focused states utilize a diffused emerald aura (`0 0 24px rgba(16, 185, 129, 0.2)`), signaling interactive readiness and completion achievements.

## Shapes

The design system employs **Level 2 Roundedness**, delivering disciplined, architectural geometry:

- **Base Controls & Inputs:** 8px radius (`0.5rem`). Applied to buttons, inputs, segmented controls, and badges.
- **Cards & Modules:** 16px radius (`1rem` / `rounded-lg`). Gives panels an engineered, cohesive silhouette.
- **Modals & Sheet Bottoms:** 24px radius (`1.5rem` / `rounded-xl`). Provides softer ergonomics on modal bottom sheets in mobile viewport contexts.
- **Circular Indicators:** Avatars, floating mini-action icons, and biometric progress rings use full circle geometry (`rounded-full`).

## Components

### Buttons
- **Primary:** Solid `#10B981` background, `#090A0F` text with `font-weight: 600`. Hover state triggers brightness increase with an ambient glow (`box-shadow: 0 0 16px rgba(16, 185, 129, 0.35)`). Active state scales slightly to `0.98`.
- **Secondary:** Surface `#1A1D24` with `1px solid rgba(255, 255, 255, 0.12)`, text `#FFFFFF`. Hover changes border to `rgba(255, 255, 255, 0.25)`.
- **Ghost:** Transparent background, `#94A3B8` text, transitioning to `#FFFFFF` on hover with a subtle `rgba(255, 255, 255, 0.05)` fill.

### Cards
- **Telemetry & Workout Cards:** Background `#12141A`, `border: 1px solid rgba(255, 255, 255, 0.08)`, 16px corner radius.
- **Interactive Coaching Cards:** Hover state increases border luminosity to `rgba(255, 255, 255, 0.16)` and translates the card -2px along the Y-axis.

### Chips & Badges
- **Status Badges:** Compact height (24px), uppercase `label-sm` font, 4px corner radius. Completed states use `rgba(16, 185, 129, 0.15)` fill with `#10B981` text. Pending or intensity tags use deep slate backgrounds with `#94A3B8` text.

### Input Fields
- **Text & Metric Inputs:** Height 44px (mobile: 48px), background `#12141A`, border `1px solid rgba(255, 255, 255, 0.1)`. Placeholder text `#64748B`.
- **Focus State:** Border shifts cleanly to `#10B981` with a localized inset ring (`0 0 0 1px #10B981`).

### Checkboxes & Segmented Controls
- **Workout Set Checkboxes:** 22px square with 6px corner radius. Default state has a `1px solid rgba(255, 255, 255, 0.2)` border. When marked complete, fills immediately with `#10B981` and reveals a bold `#090A0F` checkmark.
- **View Switches (Macros / Training / Analytics):** Embedded within a `#090A0F` track, active segment floats on `#1E293B` with high-contrast `#FFFFFF` text.

### Bespoke Domain Components
- **Biometric Metric Tile:** Houses large numerical readouts (`metric-num`) with unit designations in `label-sm`, paired with trend arrows (+2.5 kg, -1.2% body fat) colored conditionally in emerald or slate.
- **Workout Execution Header:** Fixed top strip during active training sessions showing total workout time, heart rate zone, and progress bar rendered in `#10B981`.