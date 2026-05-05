/**
 * Centralized keyboard shortcut wiring for the Railway Order Management app.
 *
 * Java sides (MainLayout, MasterDetailLayout) call into the helpers exposed on
 * window.romShortcuts. Using hotkeys-js keeps three things easy:
 *
 *  - matching by character (so "/" works on a German keyboard where the slash
 *    key is physically the dash key — Vaadin's Shortcuts API matches by
 *    physical code which is layout-dependent),
 *  - the global hotkeys.filter that suppresses bindings while typing in
 *    INPUT/TEXTAREA/contenteditable so dashes and ns no longer hijack form
 *    fields,
 *  - sequences such as `g o` for vim-style global navigation.
 */
import hotkeys from "hotkeys-js";

// hotkeys-js' default filter blocks INPUT/TEXTAREA. We extend it to also
// cover Vaadin's web-component fields, which present as their own tag names.
hotkeys.filter = (event: KeyboardEvent) => {
    const t = event.target as HTMLElement | null;
    if (!t) return true;
    const tag = t.tagName;
    if (tag === "INPUT" || tag === "TEXTAREA" || tag === "SELECT") return false;
    if ((t as HTMLElement).isContentEditable) return false;
    if (t.matches?.(
        "vaadin-text-field, vaadin-text-area, vaadin-combo-box, vaadin-date-picker," +
        "vaadin-checkbox, vaadin-select, vaadin-number-field, vaadin-password-field," +
        "vaadin-email-field, vaadin-time-picker, vaadin-date-time-picker," +
        "vaadin-integer-field, vaadin-big-decimal-field"
    )) return false;
    return true;
};

declare global {
    interface Window {
        romShortcuts?: {
            registerGlobal(): void;
            registerView(filterId: string, allowNew: boolean): void;
        };
    }
}

const dispatch = (name: string) =>
    document.body.dispatchEvent(new CustomEvent(name, { bubbles: true, composed: true }));

if (!window.romShortcuts) {
    let globalReady = false;
    let viewBound: { filterId: string | null; allowNew: boolean } = { filterId: null, allowNew: false };

    window.romShortcuts = {
        registerGlobal() {
            if (globalReady) return;
            globalReady = true;

            hotkeys("g+o", (e) => { e.preventDefault(); window.location.assign("/orders"); });
            hotkeys("g+b", (e) => { e.preventDefault(); window.location.assign("/businesses"); });
            hotkeys("g+h", (e) => { e.preventDefault(); window.location.assign("/"); });

            hotkeys("ctrl+k, command+k", (e) => { e.preventDefault(); dispatch("rom-palette"); });
            hotkeys("shift+/", (e) => { e.preventDefault(); dispatch("rom-help"); });
        },

        // Idempotent: re-binds with the latest filterId so navigating between master-
        // detail views (orders ↔ businesses) updates which input gets focused.
        registerView(filterId: string, allowNew: boolean) {
            viewBound = { filterId, allowNew };

            // Bindings install once; closures read the latest viewBound.
            if ((window.romShortcuts as any).__viewReady) return;
            (window.romShortcuts as any).__viewReady = true;

            hotkeys("/", (e) => {
                e.preventDefault();
                if (viewBound.filterId) {
                    document.getElementById(viewBound.filterId)?.focus();
                }
            });
            hotkeys("n", (e) => {
                if (!viewBound.allowNew) return;
                e.preventDefault();
                dispatch("md-new");
            });
        },
    };
}
