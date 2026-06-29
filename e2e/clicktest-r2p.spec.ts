/**
 * R2P inbox accept dialog via real clicks: the confirm button must stay disabled until a target
 * order is chosen (no-match path). Console errors fail the test (fixtures). The simulated entry is
 * cancelled (not consumed); the r2p inbox is cleaned up in DB teardown by the runner.
 */
import { test, expect, login, navTo, clickButton, expectNoServerError, shot } from "./fixtures";

test.describe("R2P inbox accept (pure click)", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("Accept dialog: confirm is gated until a target order is selected", async ({ page }) => {
    await navTo(page, /R2P/, "/r2p-inbox");
    await expectNoServerError(page);

    // Ensure there's an entry to accept.
    await clickButton(page, /Simulate incoming|Simulier/i);
    await page.waitForTimeout(1500);

    // Open the accept dialog for the first inbox entry.
    await page.getByRole("button", { name: /^Accept$/ }).first().click();
    const dialog = page.locator("vaadin-dialog-overlay");
    await expect(dialog).toBeVisible({ timeout: 6_000 });

    const confirm = dialog.getByRole("button", { name: /^Accept$/ }).first();
    const targetSelect = dialog.locator("vaadin-select");

    if (await targetSelect.count()) {
      // No-match path: confirm disabled → pick a target order → confirm enabled.
      await expect(confirm).toBeDisabled();
      await targetSelect.click();
      // scope to the just-opened overlay (the header language switcher is also a vaadin-select)
      await page.locator("vaadin-select-overlay[opened] vaadin-select-item").first().click();
      await expect(confirm).toBeEnabled();
      await shot(page, "r2p-accept-gated");
    } else {
      // Auto-match path: a note is shown and confirm is enabled.
      await expect(confirm).toBeEnabled();
      await shot(page, "r2p-accept-matched");
    }

    // Cancel without consuming the entry.
    await dialog.getByRole("button", { name: /^Cancel$|Abbrechen/ }).first().click();
    await expect(dialog).toBeHidden({ timeout: 6_000 });
    await expectNoServerError(page);
  });
});
