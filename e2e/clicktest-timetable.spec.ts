/**
 * Timetable builder route-step validations via real clicks (P0 error paths). Opens the builder for
 * a seeded order and drives the "Define Route" step the way a user does — picking operational points
 * in the From/To combos, adding a via, setting anchor times — then asserts the inline routeError Span
 * (NOT a notification; the messages render in light DOM, so body.toContainText is correct).
 * Console errors fail the test (fixtures).
 *
 * Validators under test: TimetableRouteStep.validateRouteInputs (pointsRequired / samePoint /
 * anchorExclusive / viaRequired). All fire before any routing-service/DB call.
 */
import { test, expect, login, navTo, clickButton, expectNoServerError, shot } from "./fixtures";

type P = import("@playwright/test").Page;

/** Open the timetable builder (Define Route step) for the first seeded order. */
async function openBuilder(page: P) {
  await navTo(page, /Orders/, "/orders");
  const firstCard = page.locator("#order-list .md-card-wrapper").first();
  await expect(firstCard).toBeVisible({ timeout: 10_000 });
  await firstCard.click();
  await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}/, { timeout: 10_000 });
  const orderId = page.url().match(/orders\/([0-9a-f-]{36})/)![1];

  await page.goto(`/orders/${orderId}/timetable-builder`);
  await expectNoServerError(page);
  await expect(page.locator("body")).toContainText(/Define Route|Route definieren|Schritt 1/i, {
    timeout: 15_000,
  });
}

/**
 * Pick an operational point in a From/To combo (the proven getByLabel pattern: the combo's label
 * lives in shadow DOM, so a hasText filter would miss it; getByLabel targets the input by a11y name).
 * The OP search is lazy/server-side, so we wait for the option before clicking.
 */
async function selectOp(page: P, label: string, search: string, option: string | RegExp) {
  const combo = page.getByLabel(label, { exact: true }).locator("..");
  await combo.click();
  await combo.locator("input").fill(search);
  const item = page.locator("vaadin-combo-box-item").filter({ hasText: option }).first();
  await expect(item).toBeVisible({ timeout: 10_000 });
  await item.click();
}

// Seeded operational points (RINF import) — the exact strings the passing sbahn flow uses.
const OLTEN = { search: "Olten", option: "Olten (CH00218)" } as const;
const AARAU = { search: "Aarau", option: /Aarau GB \(CH02136\)/ } as const;

test.describe("Timetable builder route validation (pure click)", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
    await openBuilder(page);
  });

  test("Calculating a route without origin/destination shows an error", async ({ page }) => {
    await clickButton(page, /Calculate route|Route berechnen/);
    await expect(page.locator("body")).toContainText(
      /Please select origin and destination|Start und Ziel/i,
      { timeout: 6_000 },
    );
    await expectNoServerError(page);
    await shot(page, "timetable-route-no-points");
  });

  test("Same origin and destination is rejected", async ({ page }) => {
    await selectOp(page, "From", OLTEN.search, OLTEN.option);
    await selectOp(page, "To", OLTEN.search, OLTEN.option);
    await clickButton(page, /Calculate route|Route berechnen/);
    await expect(page.locator("body")).toContainText(/Origin and destination must be different/i, {
      timeout: 6_000,
    });
    await expectNoServerError(page);
    await shot(page, "timetable-route-same-point");
  });

  test("Setting both anchor times is rejected (only one allowed)", async ({ page }) => {
    await selectOp(page, "From", OLTEN.search, OLTEN.option);
    await selectOp(page, "To", AARAU.search, AARAU.option);
    // Both a departure AND an arrival anchor → the route is over-constrained.
    await page.getByLabel("Departure at origin", { exact: true }).fill("05:00");
    await page.getByLabel("Arrival at destination", { exact: true }).fill("06:00");
    await clickButton(page, /Calculate route|Route berechnen/);
    await expect(page.locator("body")).toContainText(
      /Please set only one anchor time, not both/i,
      { timeout: 6_000 },
    );
    await expectNoServerError(page);
    await shot(page, "timetable-route-anchor-exclusive");
  });

  test("An incomplete via point is rejected", async ({ page }) => {
    await selectOp(page, "From", OLTEN.search, OLTEN.option);
    await selectOp(page, "To", AARAU.search, AARAU.option);
    // Add a via row but leave its operational point empty.
    await clickButton(page, /Add via|Zwischenhalt/);
    await clickButton(page, /Calculate route|Route berechnen/);
    await expect(page.locator("body")).toContainText(/Please complete every via point/i, {
      timeout: 6_000,
    });
    await expectNoServerError(page);
    await shot(page, "timetable-route-via-required");
  });

  test("A via marked as 'Stop' without an activity is rejected", async ({ page }) => {
    await selectOp(page, "From", OLTEN.search, OLTEN.option);
    await selectOp(page, "To", AARAU.search, AARAU.option);
    await clickButton(page, /Add via|Zwischenhalt/);

    // The via editor is the innermost <div> holding the "Stop" checkbox + the label-less point combo.
    const viaCard = page
      .locator("div")
      .filter({ has: page.getByLabel("Stop", { exact: true }) })
      .last();
    await expect(viaCard).toBeVisible({ timeout: 6_000 });

    // Give the via a real operational point — otherwise "via required" fires before the activity rule.
    const viaPoint = viaCard.locator("vaadin-combo-box").first();
    await viaPoint.click();
    await viaPoint.locator("input").fill(OLTEN.search);
    const item = page.locator("vaadin-combo-box-item").filter({ hasText: OLTEN.option }).first();
    await expect(item).toBeVisible({ timeout: 10_000 });
    await item.click();

    // Mark it as a Stop (reveals the activity field) but leave the activity empty.
    await viaCard.locator("vaadin-checkbox").first().click();

    await clickButton(page, /Calculate route|Route berechnen/);
    await expect(page.locator("body")).toContainText(/Each intermediate stop requires an activity/i, {
      timeout: 6_000,
    });
    await expectNoServerError(page);
    await shot(page, "timetable-route-via-activity");
  });
});
