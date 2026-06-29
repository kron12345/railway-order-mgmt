/**
 * CAPACITY purchase → TttOrderDialog validation, via real clicks (no DB/grid magic). Reaches the
 * dialog by adding a purchase on seed order 3234's FAHRPLAN/CAPACITY position, then asserts the
 * client-side guards in TttOrderDialog.validateRequired:
 *   - submitting with everything empty flags all six required fields invalid (no notification —
 *     the only signal is the [invalid] attribute and the dialog staying open);
 *   - a malformed e-mail is rejected on blur, a valid one clears the error.
 *
 * Also guards the i18n fix: the Train type / Traffic type labels used to render as
 * "!ttt.order.trainType!" because the keys were missing in EN/IT/FR.
 *
 * NOTE: PurchaseDialog.Save creates the purchase position BEFORE TttOrderDialog opens and there is
 * no UI to delete a purchase, so this test leaves one OFFEN CAPACITY purchase on 3234. It is removed
 * in DB teardown (purchase on 3234's CAPACITY need with pm_path_request_id IS NULL) — seed data is
 * preserved.
 */
import { test, expect, login, navTo, expectNoServerError, shot } from "./fixtures";

type P = import("@playwright/test").Page;

test.describe("CAPACITY purchase via TTT (pure click)", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("Required fields and a malformed e-mail are rejected", async ({ page }) => {
    // Open seed order 3234 (has one FAHRPLAN/CAPACITY position with no purchase yet).
    await navTo(page, /Orders/, "/orders");
    await expect(page.locator("#order-list .md-card-wrapper").first()).toBeVisible({
      timeout: 10_000,
    });
    await page.locator("#order-list .md-card-wrapper").filter({ hasText: "3234" }).first().click();
    await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}/, { timeout: 10_000 });

    // The CAPACITY need row carries the "Capacity" badge AND its own "+ Add Purchase Position"
    // button (the VEHICLE/PERSONNEL needs have their own buttons → scope to the row with Capacity).
    const capRow = page
      .locator("div")
      .filter({ has: page.getByText("Capacity", { exact: true }) })
      .filter({ has: page.getByRole("button", { name: /Add Purchase Position/ }) })
      .last();
    await expect(capRow).toBeVisible({ timeout: 10_000 });
    await capRow.getByRole("button", { name: /Add Purchase Position/ }).click();

    // PurchaseDialog: "Order via TTT" is pre-checked for CAPACITY → Save opens the TttOrderDialog.
    const purchaseDialog = page
      .locator("vaadin-dialog-overlay")
      .filter({ hasText: /Add Purchase Position/ });
    await expect(purchaseDialog).toBeVisible({ timeout: 6_000 });
    await purchaseDialog.getByRole("button", { name: /^Save$/ }).click();

    // Scope to the TttOrderDialog. The just-closed PurchaseDialog overlay lingers in the DOM and
    // also contains the "Order via TTT" checkbox text, so additionally require "Debit code" (unique
    // to this dialog) to avoid a strict-mode match on two overlays.
    const ttt = page
      .locator("vaadin-dialog-overlay")
      .filter({ hasText: /Order via TTT/ })
      .filter({ hasText: /Debit code/ });
    await expect(ttt).toBeVisible({ timeout: 6_000 });

    // i18n fix guard: the labels resolve (no missing-key markers).
    await expect(ttt.getByText("!ttt.order.trainType!")).toHaveCount(0);
    await expect(ttt.getByText("!ttt.order.trafficType!")).toHaveCount(0);
    await expect(ttt.getByLabel("Train type", { exact: true })).toBeVisible();
    await expect(ttt.getByLabel("Traffic type", { exact: true })).toBeVisible();

    // Submit everything empty → all six required fields flagged invalid; dialog stays open.
    await ttt.getByRole("button", { name: /^Order/ }).click();
    await expect(ttt).toBeVisible();
    await expect(ttt.locator("vaadin-text-field[invalid]")).toHaveCount(2); // debit code, contact name
    await expect(ttt.locator("vaadin-combo-box[invalid]")).toHaveCount(3); // train type, traffic type, brake
    await expect(ttt.locator("vaadin-email-field[invalid]")).toHaveCount(1); // contact email
    await shot(page, "ttt-order-required");

    // E-mail format: a valid address clears the error, a malformed one re-flags it.
    const emailHost = ttt.locator("vaadin-email-field");
    const emailInput = emailHost.locator("input");
    await emailInput.click();
    await emailInput.fill("dispatch@example.com");
    await emailInput.press("Tab");
    await expect(emailHost).not.toHaveAttribute("invalid", "");

    await emailInput.click();
    await emailInput.fill("not-an-email");
    await emailInput.press("Tab");
    await expect(emailHost).toHaveAttribute("invalid", "");
    await shot(page, "ttt-order-bad-email");

    // Cancel — no TTT order is placed (the created purchase is cleaned up in DB teardown).
    await ttt.getByRole("button", { name: /^Cancel$/ }).click();
    await expect(ttt).toBeHidden({ timeout: 6_000 });
    await expectNoServerError(page);
  });
});
