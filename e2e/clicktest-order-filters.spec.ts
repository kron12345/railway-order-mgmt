/**
 * Complete Order-overview filter panel coverage — every filter applied via real clicks, asserted
 * against the seeded orders' rendered cards (no DB/grid internals). Console errors fail the test.
 * Fields are located with getByLabel (exact) — Vaadin sets `label` as a property, not a reflected
 * attribute, so a [label=...] CSS selector would not match.
 *
 * Seed orders used as fixtures (read once from the running app):
 *   3234         AUFTRAG / SINGLE / tag Gueterverkehr / 06-01→08-31 / has open purchase
 *   4711         PLANUNG / SINGLE / tag Personenverkehr / 06-01→08-31 / has open purchase
 *   ABW-2026-001 PRODUKT_LEISTUNG / ANNUAL / 09-01→12-12
 *   JB-2026-001  AUFTRAG / ANNUAL / 06-01→12-12
 *   ZUG-2026-001 AUFTRAG / SINGLE / 06-01→12-12
 */
import { test, expect, login, clickButton, dismissDevOverlays, expectNoServerError, shot } from "./fixtures";

type P = import("@playwright/test").Page;

async function openOrdersWithFilters(page: P) {
  await page.goto("/orders");
  await dismissDevOverlays(page);
  await expect(page.locator("#order-list .md-card-wrapper").first()).toBeVisible({ timeout: 10_000 });
  await clickButton(page, /^Filter$/);
  await expect(page.getByLabel("Status", { exact: true })).toBeVisible({ timeout: 6_000 });
}

async function pickSelect(page: P, label: string, option: RegExp) {
  await page.getByLabel(label, { exact: true }).click();
  await page
    .locator("vaadin-select-overlay[opened] vaadin-select-item")
    .filter({ hasText: option })
    .first()
    .click();
  await page.waitForTimeout(1200); // server-side reload
}

const list = (page: P) => page.locator("#order-list");
const cards = (page: P) => page.locator("#order-list .md-card-wrapper");

test.describe("Order filter panel (pure click)", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await openOrdersWithFilters(page);
  });

  test("Process status = Planning shows only planning orders", async ({ page }) => {
    await pickSelect(page, "Status", /Planning/);
    await expect(list(page)).toContainText("4711"); // PLANUNG
    await expect(list(page)).not.toContainText("3234"); // AUFTRAG
    await expectNoServerError(page);
    await shot(page, "filter-process-status");
  });

  test("Order type = Annual vs Single discriminates the list", async ({ page }) => {
    await pickSelect(page, "Order type", /Annual order/);
    await expect(list(page)).toContainText("JB-2026-001"); // ANNUAL
    await expect(list(page)).not.toContainText("4711"); // SINGLE
    await shot(page, "filter-ordertype-annual");

    await pickSelect(page, "Order type", /Single order/);
    await expect(list(page)).toContainText("4711");
    await expect(list(page)).not.toContainText("JB-2026-001");
  });

  test("Processing status filter (none set on seed orders) empties the list", async ({ page }) => {
    // Every seed order has no internal status, so selecting any value matches nothing.
    await page.getByLabel("Processing status", { exact: true }).click();
    // skip the leading "—" (clear) entry, pick a real status
    await page.locator("vaadin-select-overlay[opened] vaadin-select-item").nth(1).click();
    await expect(cards(page)).toHaveCount(0, { timeout: 6_000 });
    await expectNoServerError(page);
  });

  // Regression guard for the date-range filter bug: a bare `:validFromMin is null` check on a
  // LocalDate param made PostgreSQL fail to infer the bind type (42P18), the lazy reload threw and
  // was swallowed, so the list stayed unfiltered. Fixed by casting the param in the null-guard
  // (cast(:validFromMin as LocalDate) is null) in OrderRepository.searchOrders.
  test("Validity 'From' filter excludes orders ending before it", async ({ page }) => {
    const from = page.getByLabel("From", { exact: true });
    await from.click();
    await from.pressSequentially("9/15/2026", { delay: 30 }); // EN locale M/d/yyyy
    await from.press("Enter");
    await expect(page.locator(".md-filter-chips")).toContainText(/From/i, { timeout: 6_000 });
    await expect(list(page)).toContainText("JB-2026-001"); // valid_to 12-12 ≥ 9/15
    await expect(list(page)).not.toContainText("4711"); // valid_to 08-31 < 9/15
    await shot(page, "filter-daterange");
  });

  test("Tags filter matches the order carrying the tag", async ({ page }) => {
    const tags = page.getByLabel("Tags", { exact: true });
    await tags.click();
    await tags.fill("Personenverkehr");
    await page.waitForTimeout(1200);
    await expect(list(page)).toContainText("4711"); // tag Personenverkehr
    await expect(list(page)).not.toContainText("3234"); // tag Gueterverkehr
    await shot(page, "filter-tags");
  });

  test("'Assigned to me' empties the list (all seed orders unassigned)", async ({ page }) => {
    await page.getByLabel("Assigned to me", { exact: true }).check();
    await page.waitForTimeout(1200);
    await expect(cards(page)).toHaveCount(0, { timeout: 6_000 });
    await expectNoServerError(page);
  });

  test("'Order incomplete' keeps only orders with an open purchase", async ({ page }) => {
    await page.getByLabel("Order incomplete", { exact: true }).check();
    await page.waitForTimeout(1200);
    await expect(list(page)).toContainText("3234"); // has open purchase
    await expect(list(page)).toContainText("4711"); // has open purchase
    await expect(list(page)).not.toContainText("ABW-2026-001"); // none open
    await expect(list(page)).not.toContainText("ZUG-2026-001");
    await shot(page, "filter-incomplete");
  });
});
