/**
 * Order-form business-rule validations, exercised by real clicks/typing (no DB/grid magic).
 * Console errors fail the test (fixtures). The cost-center test is self-cleaning (the order it
 * creates is deleted via the UI).
 *
 * Covered:
 *  1. New Order: "Valid To" before "Valid From" is rejected on Save — the field is flagged invalid
 *     with the message "Valid To < Valid From" and the app does NOT navigate away
 *     (OrderFormPanel.validate, lines 168-174).
 *  2. SOB §5.7: moving an order's Processing status to "Approved" (FREIGEGEBEN) without a cost
 *     center is blocked with an error notification (OrderService.requireCostCenter →
 *     OrderInternalStatusBar shows order.costCenter.required).
 */
import {
  test,
  expect,
  login,
  navTo,
  clickButton,
  fillField,
  fillDate,
  expectNotification,
  expectNoServerError,
  shot,
} from "./fixtures";

type P = import("@playwright/test").Page;

async function openNewOrderForm(page: P) {
  await navTo(page, /Orders/, "/orders");
  await clickButton(page, /New Order|Neuer Auftrag/);
  await expect(page).toHaveURL(/\/orders\/new/);
}

test.describe("Order form validation (pure click)", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("New Order: 'Valid To' before 'Valid From' is rejected on Save", async ({ page }) => {
    await openNewOrderForm(page);

    await fillField(page, /order number|auftragsnummer/i, "ZZ-VTF-" + Date.now());
    await fillField(page, /order name|auftragsname/i, "E2E valid-range reject");
    // English UI locale → date pickers parse M/d/yyyy. Valid To is BEFORE Valid From.
    await fillDate(page, /valid from|gültig ab/i, "8/31/2026");
    await fillDate(page, /valid to|gültig bis/i, "6/1/2026");

    await clickButton(page, /^Save$|Speichern/);

    // Save is blocked: the app stays on the new-order form (no navigation to /orders/<uuid>).
    await expect(page).toHaveURL(/\/orders\/new/);
    // The "Valid To" picker is flagged invalid and shows the comparison error message.
    const validTo = page.locator("vaadin-date-picker").filter({ hasText: /Valid To/ }).first();
    await expect(validTo).toHaveAttribute("invalid", "");
    await expect(page.getByText(/Valid To < Valid From/)).toBeVisible();
    await expectNoServerError(page);
    await shot(page, "order-form-bad-daterange"); // captures the rejected/invalid state
  });

  test("Releasing an order without a cost center is blocked (SOB §5.7)", async ({ page }) => {
    const number = "ZZ-CC-" + Date.now();

    // Create a fresh order WITHOUT a cost center (allowed while not yet released).
    await openNewOrderForm(page);
    await fillField(page, /order number|auftragsnummer/i, number);
    await fillField(page, /order name|auftragsname/i, "E2E cost-center gate");
    await fillDate(page, /valid from|gültig ab/i, "6/1/2026");
    await fillDate(page, /valid to|gültig bis/i, "12/31/2026");
    // cost center left blank on purpose
    await clickButton(page, /^Save$|Speichern/);

    await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}/, { timeout: 10_000 });
    const url = page.url();
    await expect(page.locator("body")).toContainText(number);

    // The Processing-status select has no label property (its caption is a sibling Span), and the
    // header language switcher is also a vaadin-select → scope to the internal-status bar.
    const statusSelect = page.locator(".order-detail__internal-status vaadin-select").first();
    await expect(statusSelect).toBeVisible({ timeout: 6_000 });
    await statusSelect.click();
    await page
      .locator("vaadin-select-overlay[opened] vaadin-select-item")
      .filter({ hasText: /Approved/ })
      .first()
      .click();

    // Blocked: error notification, status not persisted.
    await expectNotification(page, /A cost center .* is required before the order can be released/i);
    await shot(page, "order-form-costcenter-required");
    await expectNoServerError(page);

    // Self-clean: delete the order via the trash icon → confirm.
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
