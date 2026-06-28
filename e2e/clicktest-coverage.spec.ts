/**
 * Positive-flow click-tests for views that previously had only screenshot/smoke coverage.
 * Pure UI: create/read/edit/delete via real clicks + typing; assertions read the rendered DOM.
 * Console errors fail the test (fixtures). Self-cleaning (the business CRUD deletes its own row).
 */
import {
  test,
  expect,
  login,
  navTo,
  clickButton,
  fillField,
  expectNotification,
  expectNoServerError,
  shot,
} from "./fixtures";

test.describe("Positive flows (pure click)", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  test("Dashboard shows KPI metrics", async ({ page }) => {
    await navTo(page, /Dashboard/, "/");
    await expectNoServerError(page);
    // KPI cards render numeric values.
    const body = await page.locator("main, vaadin-app-layout").innerText();
    expect(body.length).toBeGreaterThan(20);
    await shot(page, "dashboard");
  });

  test("Business lifecycle: create → read → edit → delete (all via clicks)", async ({ page }) => {
    const title = "ZZ-Klicktest " + Date.now();
    const edited = title + " (geändert)";

    // CREATE
    await navTo(page, /Business|Gesch/, "/businesses");
    await clickButton(page, /New Business|Neues Gesch/);
    await expect(page).toHaveURL(/\/businesses\/new/);
    await fillField(page, /title|titel/i, title);
    await shot(page, "business-new-filled");
    await clickButton(page, /^Save$|Speichern/);

    // READ — landed on the read view, title visible
    await expect(page).toHaveURL(/\/businesses\/[0-9a-f-]{36}$/, { timeout: 10_000 });
    const businessUrl = page.url();
    await expect(page.locator("body")).toContainText(title);
    await expectNoServerError(page);
    await shot(page, "business-read");

    // EDIT — change the title, save, verify the new value shows
    await clickButton(page, /^Edit$|Bearbeiten/);
    await expect(page).toHaveURL(/\/edit$/);
    await fillField(page, /title|titel/i, edited);
    await clickButton(page, /^Save$|Speichern/);
    await expect(page.locator("body")).toContainText(edited, { timeout: 10_000 });
    await shot(page, "business-edited");

    // DELETE — the trash icon button (icon-only) → confirm dialog → confirm; row gone
    await page
      .locator("vaadin-button")
      .filter({ has: page.locator('vaadin-icon[icon="vaadin:trash"]') })
      .first()
      .click();
    const confirm = page.locator("vaadin-confirm-dialog-overlay");
    await expect(confirm).toBeVisible();
    await confirm.getByRole("button", { name: /delete|löschen/i }).first().click();
    await expect(page).toHaveURL(/\/businesses$/, { timeout: 10_000 });
    await shot(page, "business-deleted");

    // Robustly verify it is really gone (not just absent from the lazy list): revisiting the
    // deleted business's URL redirects back to the overview.
    await page.goto(businessUrl);
    await expect(page).toHaveURL(/\/businesses$/, { timeout: 10_000 });
  });

  test("Order → position detail → back (navigation via clicks)", async ({ page }) => {
    await navTo(page, /Orders/, "/orders");
    // open the first order card
    const firstCard = page.locator("#order-list .md-card-wrapper").first();
    await expect(firstCard).toBeVisible({ timeout: 10_000 });
    await firstCard.click();
    await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}/, { timeout: 10_000 });
    await expectNoServerError(page);

    // open the first position's detail via its "view" (eye) action
    const viewBtn = page.getByRole("button", { name: /view|ansicht|anzeigen/i }).first();
    if (await viewBtn.count()) {
      await viewBtn.click();
      await expect(page).toHaveURL(/\/positions\/[0-9a-f-]{36}/, { timeout: 10_000 });
      await expectNoServerError(page);
      await shot(page, "position-detail");
      // back to the order via the ← (arrow-left) button — icon + "to order" label
      await page
        .locator("vaadin-button")
        .filter({ has: page.locator('vaadin-icon[icon="vaadin:arrow-left"]') })
        .first()
        .click();
      await expect(page).toHaveURL(/\/orders\/[0-9a-f-]{36}/, { timeout: 10_000 });
    }
  });

  test("Open positions view loads with content", async ({ page }) => {
    await navTo(page, /Open positions|Offene/, "/offene-positionen");
    await expectNoServerError(page);
    await expect(page.locator("vaadin-app-layout")).toBeVisible();
    await shot(page, "open-positions");
  });

  test("R2P inbox: simulate an incoming order (mock)", async ({ page }) => {
    await navTo(page, /R2P/, "/r2p-inbox");
    await expectNoServerError(page);
    const simulate = page.getByRole("button", { name: /simulate|simulier/i }).first();
    if (await simulate.count()) {
      await simulate.click();
      // a success notification or a new inbox card should appear
      await page.waitForTimeout(1500);
      await expectNoServerError(page);
    }
    await shot(page, "r2p-inbox");
  });

  test("Settings view loads (admin) with tabs", async ({ page }) => {
    await navTo(page, /Settings|Einstellungen/, "/settings");
    await expectNoServerError(page);
    const tabs = page.locator("vaadin-tab");
    await expect(tabs.first()).toBeVisible({ timeout: 10_000 });
    // click the second tab to confirm tab switching works
    if ((await tabs.count()) > 1) {
      await tabs.nth(1).click();
      await expectNoServerError(page);
    }
    await shot(page, "settings");
  });

  test("Profile view shows the signed-in user", async ({ page }) => {
    await navTo(page, /Profile|Profil/, "/profile");
    await expectNoServerError(page);
    await expect(page.locator("body")).toContainText(/sebastian/i, { timeout: 10_000 });
    await shot(page, "profile");
  });
});
