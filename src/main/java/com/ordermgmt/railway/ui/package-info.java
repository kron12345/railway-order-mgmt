/**
 * <strong>UI layer</strong> — Vaadin Flow views and components.
 *
 * <p>Sub-packages:
 *
 * <ul>
 *   <li>{@code layout} — {@code MainLayout} (the global app shell, drawer navigation, breadcrumbs,
 *       theme application, global keyboard shortcuts).
 *   <li>{@code view} — top-level {@code @Route} entry points (Dashboard, OrderOverview,
 *       BusinessOverview, Settings, Timetable Builder, Path Manager, Rolling Stock, My Profile).
 *   <li>{@code component} — reusable building blocks. Notable infrastructure: {@code
 *       masterdetail.MasterDetailLayout} (URL-driven master/detail shell), {@code a11y.*}
 *       (SkipLinks, BreadcrumbBar, AriaLive, CommandPalette, KeyboardHelpOverlay), {@code grid.*}
 *       (per-user column-preference binder + settings popover).
 * </ul>
 *
 * <p>Layer rule (enforced by ArchUnit): UI may depend on {@code domain} and {@code infrastructure},
 * but neither may depend on {@code ui}.
 *
 * <p>Theming is centralised in {@code frontend/themes/order-mgmt/styles.css} with CSS variables for
 * the three themes (dark-amber, dark-teal, light). Status indicators always combine icon + text —
 * never colour alone.
 */
package com.ordermgmt.railway.ui;
