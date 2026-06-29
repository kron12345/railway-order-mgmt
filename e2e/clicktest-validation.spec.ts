/**
 * Error / validation click-tests — the path that was entirely untested before.
 * Pure UI: every step is a real click/type; assertions read the rendered DOM
 * (invalid fields, error notifications). Browser console errors fail the test
 * (see fixtures). Each case screenshots the error state for visual review.
 */
import {
  test,
  expect,
  login,
  navTo,
  clickButton,
  expectNotification,
  shot,
} from "./fixtures";

test.describe("Validation & error handling (pure click)", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("New Order: saving an empty form flags the required fields", async ({ page }) => {
    await navTo(page, /^Orders$|Auftr/, "/orders");
    await clickButton(page, /New Order|Neuer Auftrag/);
    await expect(page).toHaveURL(/\/orders\/new/);

    // Save with everything empty.
    await clickButton(page, /^Save$|Speichern/);

    // Required fields (order number, name, valid-from, valid-to) become invalid,
    // and we stay on the new-order form (nothing was persisted).
    const invalid = page.locator(
      "vaadin-text-field[invalid], vaadin-date-picker[invalid]",
    );
    await expect(invalid.first()).toBeVisible({ timeout: 6_000 });
    expect(await invalid.count()).toBeGreaterThanOrEqual(1);
    await expect(page).toHaveURL(/\/orders\/new/);
    await shot(page, "order-empty-required");
  });

  test("New Business: saving without a title shows a required-field error", async ({ page }) => {
    await navTo(page, /Business|Gesch/, "/businesses");
    await clickButton(page, /New Business|Neues Gesch/);
    await expect(page).toHaveURL(/\/businesses\/new/);

    await clickButton(page, /^Save$|Speichern/);

    // saveNew() shows a "<title> ist Pflichtfeld" notification and does not navigate.
    await expectNotification(page, /Pflichtfeld|required/i);
    await expect(page).toHaveURL(/\/businesses\/new/);
    await shot(page, "business-empty-title");
  });

  test("New deadline rule: saving without a name is rejected", async ({ page }) => {
    await navTo(page, /Deadline rules|Frist-Regeln|Fristregeln/, "/fristregeln");
    await clickButton(page, /New rule|Neue Regel/);

    // Dialog is open; save it with an empty name.
    const dialog = page.locator("vaadin-dialog-overlay");
    await expect(dialog).toBeVisible();
    await dialog.getByRole("button", { name: /save|speichern/i }).first().click();

    // Name field is flagged and the dialog stays open (rule not created).
    const nameInvalid = dialog.locator("vaadin-text-field[invalid]");
    await expect(nameInvalid.first()).toBeVisible({ timeout: 6_000 });
    await expect(dialog).toBeVisible();
    await shot(page, "deadlinerule-empty-name");
  });

  test("Add Vehicle: saving without a designation is rejected", async ({ page }) => {
    await navTo(page, /Rolling Stock|Rollmaterial/, "/rollingstock");
    await clickButton(page, /Add Vehicle|Fahrzeug hinzu/);

    const dialog = page.locator("vaadin-dialog-overlay");
    await expect(dialog).toBeVisible();
    await dialog.getByRole("button", { name: /save|speichern/i }).first().click();

    const designationInvalid = dialog.locator("vaadin-text-field[invalid]");
    await expect(designationInvalid.first()).toBeVisible({ timeout: 6_000 });
    await expect(dialog).toBeVisible();
    await shot(page, "vehicle-empty-designation");
  });
});
