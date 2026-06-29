/**
 * Dialog flows via real clicks: audit-history open/close, and service-position validation.
 * Console errors fail the test (fixtures). No data is persisted (validation is rejected; the
 * audit dialog is read-only).
 */
import { test, expect, login, navTo, expectNoServerError, shot } from "./fixtures";

async function openFirstOrder(page: import("@playwright/test").Page) {
  await navTo(page, /Orders/, "/orders");
  const firstCard = page.locator("#order-list .md-card-wrapper").first();
  await expect(firstCard).toBeVisible({ timeout: 10_000 });
  await firstCard.click();
  await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}/, { timeout: 10_000 });
  await expectNoServerError(page);
}

test.describe("Dialogs (pure click)", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("Audit history dialog opens and closes", async ({ page }) => {
    await openFirstOrder(page);
    await page.getByRole("button", { name: "History", exact: true }).first().click();

    const dialog = page.locator("vaadin-dialog-overlay");
    await expect(dialog).toBeVisible({ timeout: 6_000 });
    await expect(dialog.locator("vaadin-grid")).toBeVisible();
    await shot(page, "audit-history-dialog");

    await page.keyboard.press("Escape");
    await expect(dialog).toBeHidden({ timeout: 6_000 });
    await expectNoServerError(page);
  });

  test("Service position dialog rejects an empty name", async ({ page }) => {
    await openFirstOrder(page);
    await page.getByRole("button", { name: /\+\s*(Service|Leistung)/i }).first().click();

    const dialog = page.locator("vaadin-dialog-overlay");
    await expect(dialog).toBeVisible({ timeout: 6_000 });

    // Save with an empty name.
    await dialog.getByRole("button", { name: /^(Save|Speichern)$/i }).first().click();

    // Name is flagged invalid (or an error notification appears) and the dialog stays open.
    const nameInvalid = dialog.locator("vaadin-text-field[invalid], vaadin-text-field[required][invalid]");
    await expect(nameInvalid.first()).toBeVisible({ timeout: 6_000 });
    await expect(dialog).toBeVisible();
    await shot(page, "service-position-empty-name");

    // Close without saving (nothing persisted).
    await dialog.getByRole("button", { name: /^(Cancel|Abbrechen)$/i }).first().click();
    await expect(dialog).toBeHidden({ timeout: 6_000 });
  });
});
