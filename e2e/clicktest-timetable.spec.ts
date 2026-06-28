/**
 * Timetable builder route-step validation via real clicks (P0 error path that was untested).
 * Opens the builder for a seeded order and tries to calculate a route with no origin/destination.
 * Console errors fail the test (fixtures).
 */
import { test, expect, login, navTo, clickButton, expectNoServerError, shot } from "./fixtures";

test.describe("Timetable builder (pure click)", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("Calculating a route without origin/destination shows an error", async ({ page }) => {
    // Reach the builder via a seeded order (URL nav is fine; the validation is the click test).
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

    // Calculate with both origin and destination empty.
    await clickButton(page, /Calculate route|Route berechnen/);

    // The route error message is shown (origin/destination required).
    await expect(page.locator("body")).toContainText(
      /Please select origin and destination|Start und Ziel/i,
      { timeout: 6_000 },
    );
    await expectNoServerError(page);
    await shot(page, "timetable-route-no-points");
  });
});
