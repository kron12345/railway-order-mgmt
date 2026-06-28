/**
 * Order overview filter + full Order create→read→delete, all via real clicks/typing.
 * Console errors fail the test (fixtures); self-cleaning (the created order is deleted).
 */
import {
  test,
  expect,
  login,
  navTo,
  clickButton,
  fillField,
  fillDate,
  expectNoServerError,
  shot,
} from "./fixtures";

test.describe("Orders (pure click)", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("Overview search filter narrows the list", async ({ page }) => {
    await navTo(page, /Orders/, "/orders");
    const cards = page.locator("#order-list .md-card-wrapper");
    await expect(cards.first()).toBeVisible({ timeout: 10_000 });

    const search = page.locator("#order-filter input").first();
    await search.click();
    await search.fill("4711"); // a seeded order number (S-Bahn Zürich – Olten)
    await page.waitForTimeout(1200);
    await expect(page.locator("#order-list")).toContainText("4711");
    await shot(page, "orders-filter-match");

    // A term that matches nothing empties the list (server-side filtered).
    await search.fill("ZZ-nichts-" + Date.now());
    await page.waitForTimeout(1200);
    await expect(cards).toHaveCount(0);
    await expectNoServerError(page);
  });

  test("Lifecycle: create → read → delete (all via clicks)", async ({ page }) => {
    const number = "ZZ-KT-" + Date.now();
    const name = "Klicktest-Auftrag " + number;

    await navTo(page, /Orders/, "/orders");
    await clickButton(page, /New Order|Neuer Auftrag/);
    await expect(page).toHaveURL(/\/orders\/new/);

    await fillField(page, /order number|auftragsnummer/i, number);
    await fillField(page, /order name|auftragsname/i, name);
    // English UI locale → date pickers parse M/d/yyyy.
    await fillDate(page, /valid from|gültig ab/i, "6/1/2026");
    await fillDate(page, /valid to|gültig bis/i, "8/31/2026");
    await shot(page, "order-new-filled");
    await clickButton(page, /^Save$|Speichern/);

    // READ — landed on the order detail with our data shown.
    await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}/, { timeout: 10_000 });
    const url = page.url();
    await expect(page.locator("body")).toContainText(number);
    await expectNoServerError(page);
    await shot(page, "order-created");

    // DELETE — trash (icon) → confirm dialog → confirm.
    await page
      .locator("vaadin-button")
      .filter({ has: page.locator('vaadin-icon[icon="vaadin:trash"]') })
      .first()
      .click();
    const confirm = page.locator("vaadin-confirm-dialog-overlay");
    await expect(confirm).toBeVisible();
    await confirm.getByRole("button", { name: /delete|löschen/i }).first().click();
    await expect(page).toHaveURL(/\/orders$/, { timeout: 10_000 });

    // Verify gone: revisiting the deleted order's URL redirects to the overview.
    await page.goto(url);
    await expect(page).toHaveURL(/\/orders$/, { timeout: 10_000 });
  });
});
