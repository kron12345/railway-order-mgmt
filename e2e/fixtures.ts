/**
 * Shared E2E foundation: a Playwright `test` that automatically captures browser console
 * errors + uncaught page errors and FAILS the test if any "real" one occurred, plus pure
 * click-test helpers (real typing/clicking, no component-internal/JS state poking) and a
 * screenshot helper.
 *
 * Import { test, expect } from "./fixtures" in a spec and every test transparently watches
 * the browser console. The helpers operate the Vaadin UI the way a user does — locate the
 * field/button by its visible label/text and fill()/click() it — never by reading or writing
 * Vaadin component internals (grid.items, .value = ...).
 */
import { test as base, expect, Page, Locator } from "@playwright/test";

const KC_USER = "sebastian";
const KC_PASS = "sebastian";

/** Browser console/page errors that are environmental noise, not app defects. */
const IGNORED_CONSOLE = [
  /favicon/i,
  /Failed to load resource.*\b404\b/i, // missing static asset, not an app error
  /\/VAADIN\/push/i, // push websocket reconnect chatter
  /ResizeObserver loop/i, // benign browser warning some libs trigger
  /Atmosphere/i, // Vaadin push transport reconnects
];

type Fixtures = {
  /** Auto fixture: collects console.error + pageerror and asserts none (minus IGNORED). */
  consoleErrors: string[];
};

export const test = base.extend<Fixtures>({
  consoleErrors: [
    async ({ page }, use, testInfo) => {
      const errors: string[] = [];
      page.on("console", (msg) => {
        if (msg.type() === "error") {
          errors.push("[console.error] " + msg.text());
        }
      });
      page.on("pageerror", (err) => {
        errors.push("[pageerror] " + (err.stack || err.message));
      });

      await use(errors);

      const real = errors.filter((e) => !IGNORED_CONSOLE.some((rx) => rx.test(e)));
      if (real.length > 0) {
        // Attach for the HTML report and fail clearly.
        await testInfo.attach("browser-errors.txt", { body: real.join("\n\n") });
      }
      expect(real, `Browser reported ${real.length} error(s):\n${real.join("\n")}`).toEqual([]);
    },
    { auto: true },
  ],
});

export { expect };

// ── Auth ─────────────────────────────────────────────────────────────────

/** Real SSO click-through against Keycloak; lands on the app shell. */
export async function login(page: Page): Promise<void> {
  await page.goto("/");
  await page.locator('a[href*="oauth2/authorization/keycloak"]').click();
  await page.locator('input[name="username"], #username').fill(KC_USER);
  await page.locator('input[name="password"], #password').fill(KC_PASS);
  await page.locator('#kc-login, input[type="submit"], button[type="submit"]').click();
  await page.waitForURL(/localhost:8085/, { timeout: 30_000 });
  await expect(page.locator("vaadin-app-layout")).toBeVisible({ timeout: 20_000 });
  await dismissDevOverlays(page);
}

/** Hide the Vaadin dev-tools/copilot gizmos if present (no-op in production mode). */
export async function dismissDevOverlays(page: Page): Promise<void> {
  await page
    .evaluate(() => {
      document.querySelectorAll("vaadin-dev-tools, copilot-main").forEach((el) => {
        (el as HTMLElement).style.display = "none";
      });
    })
    .catch(() => {});
}

// ── Navigation ───────────────────────────────────────────────────────────

/**
 * Go to a view. Prefers the given URL (plain navigation — a user can type a URL too; this is
 * not "DB magic"), since some sidenav items are collapsed children. Pass only a label to force
 * a real sidenav click (used by the dedicated navigation test).
 */
export async function navTo(page: Page, label: RegExp, urlFallback?: string): Promise<void> {
  if (urlFallback) {
    await page.goto(urlFallback);
  } else {
    await page.locator("vaadin-side-nav-item").filter({ hasText: label }).first().click();
  }
  await dismissDevOverlays(page);
}

// ── Pure-click form helpers (locate by visible label, then fill/click) ─────

/** The Vaadin field component (text-field/text-area/etc.) whose label text matches. */
function fieldByLabel(page: Page, label: string | RegExp): Locator {
  return page
    .locator(
      "vaadin-text-field, vaadin-text-area, vaadin-email-field, vaadin-integer-field, vaadin-number-field, vaadin-password-field",
    )
    .filter({ hasText: label })
    .first();
}

/** Type a value into the field with the given label (real keyboard input). */
export async function fillField(page: Page, label: string | RegExp, value: string): Promise<void> {
  const field = fieldByLabel(page, label);
  const input = field.locator("input, textarea").first();
  await input.click();
  await input.fill(value);
}

/**
 * Click a button by its accessible name. getByRole matches the a11y name (robust against the
 * icon/whitespace that breaks an anchored hasText regex) and excludes hidden dialog templates.
 */
export async function clickButton(page: Page, text: string | RegExp): Promise<void> {
  await dismissDevOverlays(page);
  await page.getByRole("button", { name: text }).first().click();
}

/** Open a vaadin-combo-box by label, filter by `search`, click the matching item. */
export async function selectCombo(
  page: Page,
  label: string | RegExp,
  search: string,
  optionText: string | RegExp,
): Promise<void> {
  const combo = page.locator("vaadin-combo-box").filter({ hasText: label }).first();
  await combo.locator("input").click();
  await combo.locator("input").fill(search);
  const option = page.locator("vaadin-combo-box-item").filter({ hasText: optionText }).first();
  await expect(option).toBeVisible({ timeout: 10_000 });
  await option.click();
}

/** Pick a value from a vaadin-select by label. */
export async function selectOption(
  page: Page,
  label: string | RegExp,
  optionText: string | RegExp,
): Promise<void> {
  const select = page.locator("vaadin-select").filter({ hasText: label }).first();
  await select.click();
  const item = page.locator("vaadin-select-item, vaadin-item").filter({ hasText: optionText }).first();
  await expect(item).toBeVisible({ timeout: 10_000 });
  await item.click();
}

// ── Assertions on visible result ───────────────────────────────────────────

/** Assert a Vaadin notification with matching text is shown (validation error / success). */
export async function expectNotification(page: Page, text: string | RegExp): Promise<void> {
  const card = page.locator("vaadin-notification-card").filter({ hasText: text }).first();
  await expect(card).toBeVisible({ timeout: 6_000 });
}

/** Assert NO Vaadin internal-server-error overlay is present. */
export async function expectNoServerError(page: Page): Promise<void> {
  const overlay = page.locator(
    ".v-system-error, vaadin-connection-indicator[error], div.v-app-loading",
  );
  await expect(overlay).toHaveCount(0);
  const body = (await page.locator("body").innerText().catch(() => "")) || "";
  expect(
    /Internal error|Interner Fehler|There was an exception|HTTP Status 5\d\d/i.test(body),
    "page shows a server-error message",
  ).toBeFalsy();
}

/**
 * Screenshot into a STABLE dir (Playwright wipes test-results/ each run). Named by the checkpoint
 * so reruns overwrite deterministically and the latest set is always browsable for visual review.
 */
export async function shot(page: Page, name: string): Promise<string> {
  const file = `e2e-screenshots/${name}.png`;
  await dismissDevOverlays(page);
  await page.screenshot({ path: file, fullPage: false });
  return file;
}
