import { expect, Page, test } from "@playwright/test";

// ── Credentials ────────────────────────────────────────────────────
const KC_USER = "sebastian";
const KC_PASS = "sebastian";

// ── Bilingual label matchers (DE/EN) ───────────────────────────────
const NEW_ORDER_BTN = /\+?\s*(Neuer Auftrag|New Order)/i;
const ORDER_NUMBER_LABEL = /^(Auftragsnummer|Order Number)$/i;
const ORDER_NAME_LABEL = /^(Auftragsname|Order Name)$/i;
const SAVE_BTN = /^(Speichern|Save)$/i;

// Positions
const ADD_SERVICE_BTN = /\+\s*(Leistung|Service)/i;
const POSITION_NAME_LABEL = /^(Positionsname|Position Name)$/i;
const SERVICE_TYPE_LABEL = /^(Leistungsart|Service Type)$/i;
const START_TIME_LABEL = /^(Startzeit|Start Time)$/i;
const END_TIME_LABEL = /^(Endzeit|End Time)$/i;
const ALL_LABEL = /^(Alle|All)$/i;

// ── Helpers ─────────────────────────────────────────────────────────

async function login(page: Page) {
  await page.goto("/");
  await page.locator('a[href*="oauth2/authorization/keycloak"]').click();
  await page.locator('input[name="username"], #username').fill(KC_USER);
  await page.locator('input[name="password"], #password').fill(KC_PASS);
  await page
    .locator('#kc-login, input[type="submit"], button[type="submit"]')
    .click();
  await page.waitForURL(/localhost:8085/, { timeout: 30_000 });
  await expect(page.locator("vaadin-app-layout")).toBeVisible({
    timeout: 20_000,
  });
  await dismissDevBanner(page);
}

async function dismissDevBanner(page: Page) {
  await page
    .evaluate(() => {
      const dt = document.querySelector("vaadin-dev-tools");
      if (dt) (dt as HTMLElement).style.display = "none";
    })
    .catch(() => {});
}

async function clickVaadinButton(page: Page, textMatch: RegExp) {
  await dismissDevBanner(page);
  const pattern = textMatch.source;
  const flags = textMatch.flags;
  await page.evaluate(
    ({ p, f }) => {
      const re = new RegExp(p, f);
      const buttons = document.querySelectorAll("vaadin-button");
      for (const btn of buttons) {
        if (re.test(btn.textContent || "")) {
          (btn as HTMLElement).click();
          return;
        }
      }
      throw new Error(`No vaadin-button matching /${p}/${f}`);
    },
    { p: pattern, f: flags },
  );
}

async function screenshot(page: Page, name: string) {
  await page.screenshot({
    path: `test-results/lifecycle-${name}.png`,
    fullPage: true,
  });
}

// ── Test Data ───────────────────────────────────────────────────────
const TS = Date.now();
const ORDER_NR = `E2E-${TS}`;
const ORDER_NAME = `Lifecycle Test ${TS}`;
const SERVICE_POS_NAME = `Rangierung Basel ${TS}`;
const MODIFIED_SUFFIX = " (mod)";

// ── Service Position (Leistung) Tests ───────────────────────────────
// These tests cover LEISTUNG positions which are NOT tested by
// sbahn-integration.spec.ts (that test only covers FAHRPLAN positions).

test.describe("Service Position (Leistung): Create and Modify", () => {
  test.describe.configure({ mode: "serial" });

  let page: Page;
  let orderUrl: string;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);

    // Create a new order for this test suite
    await page.goto("/orders");
    await page.waitForTimeout(1_000);
    await page.getByRole("button", { name: NEW_ORDER_BTN }).click();
    await page.waitForURL(/\/orders\/new/, { timeout: 10_000 });

    await page.getByLabel(ORDER_NUMBER_LABEL).fill(ORDER_NR);
    await page.getByLabel(ORDER_NAME_LABEL).fill(ORDER_NAME);

    const today = new Date();
    const inThreeMonths = new Date(today);
    inThreeMonths.setMonth(inThreeMonths.getMonth() + 3);
    const isoDate = (d: Date) =>
      `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, "0")}-${d.getDate().toString().padStart(2, "0")}`;

    await page.evaluate(
      ({ fromVal, toVal }) => {
        const pickers = document.querySelectorAll("vaadin-date-picker");
        const fromPicker = pickers[0] as any;
        const toPicker = pickers[1] as any;
        if (fromPicker) fromPicker.value = fromVal;
        if (toPicker) toPicker.value = toVal;
      },
      { fromVal: isoDate(today), toVal: isoDate(inThreeMonths) },
    );

    await dismissDevBanner(page);
    const vaadinSave = page
      .locator("vaadin-button")
      .filter({ hasText: /Save|Speichern/ })
      .last();
    await vaadinSave.scrollIntoViewIfNeeded();
    await page.waitForTimeout(500);
    await vaadinSave.click({ force: true, position: { x: 30, y: 12 } });

    const currentUrl = page.url();
    await page.waitForTimeout(3_000);
    if (page.url() === currentUrl) {
      await clickVaadinButton(page, /Save|Speichern/);
    }
    await page.waitForURL(/\/orders\/[0-9a-f-]+$/, { timeout: 20_000 });
    orderUrl = page.url();
    await expect(page.getByText(ORDER_NR)).toBeVisible({ timeout: 10_000 });
  });

  test.afterAll(async () => {
    // Cleanup: delete the test order
    await page.goto(orderUrl);
    await expect(page.getByText(ORDER_NR)).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(1_000);
    await dismissDevBanner(page);

    const deleteBtn = page.locator("vaadin-button").filter({
      has: page.locator('vaadin-icon[icon*="trash"]'),
    });
    await deleteBtn.first().click();

    const confirmOverlay = page.locator("vaadin-confirm-dialog-overlay");
    await expect(confirmOverlay).toBeVisible({ timeout: 5_000 });
    await dismissDevBanner(page);
    await confirmOverlay
      .locator("vaadin-button")
      .filter({ hasText: /Delete|Löschen/ })
      .click({ force: true });

    await page.waitForURL(/\/orders$/, { timeout: 20_000 });
    await page.close();
  });

  // ── Test 1: Create a Service Position (Leistung) ────────────────

  test("1. create service position (Leistung)", async () => {
    await page.goto(orderUrl);
    await expect(page.getByText(ORDER_NR)).toBeVisible({ timeout: 10_000 });

    // Click "+ Leistung" button
    await page.getByRole("button", { name: ADD_SERVICE_BTN }).click();

    // Wait for service position dialog to open
    const dialog = page.getByRole("dialog", { name: /New Position|Neue Position/i });
    await expect(dialog).toBeVisible({ timeout: 10_000 });
    await screenshot(page, "01-service-dialog-empty");

    // Fill service position form
    await dialog.getByLabel(POSITION_NAME_LABEL).fill(SERVICE_POS_NAME);
    await dialog.getByLabel(SERVICE_TYPE_LABEL).fill("Rangierung");

    // Select operational points (combo-box items render at page level)
    const fromCombo = dialog.locator("vaadin-combo-box").first();
    await fromCombo.click();
    await fromCombo.locator("input").fill("Basel SBB");
    const fromOpt = page.locator("vaadin-combo-box-item").filter({ hasText: /Basel SBB/ }).first();
    await expect(fromOpt).toBeVisible({ timeout: 10_000 });
    await fromOpt.click();

    const toCombo = dialog.locator("vaadin-combo-box").last();
    await toCombo.click();
    await toCombo.locator("input").fill("Basel SBB");
    const toOpt = page.locator("vaadin-combo-box-item").filter({ hasText: /Basel SBB/ }).first();
    await expect(toOpt).toBeVisible({ timeout: 10_000 });
    await toOpt.click();

    // Set times
    await dialog.getByLabel(START_TIME_LABEL).fill("06:00");
    await dialog.getByLabel(END_TIME_LABEL).fill("07:30");

    // Select validity dates — click "Alle" (All) button in calendar
    await dialog.getByText(ALL_LABEL, { exact: true }).click();

    await screenshot(page, "02-service-dialog-filled");

    // Save
    await dismissDevBanner(page);
    await dialog.locator("vaadin-button").filter({ hasText: /Save|Speichern/ }).click({ force: true });
    await expect(dialog).not.toBeVisible({ timeout: 10_000 });

    // Verify position appears in the list
    await page.waitForTimeout(2_000);
    await expect(page.getByText(SERVICE_POS_NAME).first()).toBeVisible({
      timeout: 15_000,
    });
    await screenshot(page, "03-service-position-created");
  });

  // ── Test 2: Modify the Service Position ─────────────────────────

  test("2. modify service position", async () => {
    await page.goto(orderUrl);
    await expect(page.getByText(SERVICE_POS_NAME)).toBeVisible({
      timeout: 10_000,
    });

    // Click the edit icon for the service position
    await dismissDevBanner(page);
    const allEditIcons = page.locator('vaadin-icon[icon*="edit"], vaadin-icon[icon*="pencil"]');
    await allEditIcons.nth(1).click({ force: true });

    // Wait for edit dialog
    const dialog = page.getByRole("dialog", { name: /Position|Edit/i });
    await expect(dialog).toBeVisible({ timeout: 10_000 });

    // Modify the name
    const nameField = dialog.getByLabel(POSITION_NAME_LABEL);
    await nameField.clear();
    await nameField.fill(SERVICE_POS_NAME + MODIFIED_SUFFIX);

    // Modify the service type
    const serviceTypeField = dialog.getByLabel(SERVICE_TYPE_LABEL);
    await serviceTypeField.clear();
    await serviceTypeField.fill("Reinigung");

    await screenshot(page, "04-service-edit-dialog");

    // Save
    await dismissDevBanner(page);
    await dialog.locator("vaadin-button").filter({ hasText: /Save|Speichern/ }).click({ force: true });
    await expect(dialog).not.toBeVisible({ timeout: 10_000 });

    // Verify modified name
    await page.waitForTimeout(1_000);
    await expect(
      page.getByText(SERVICE_POS_NAME + MODIFIED_SUFFIX).first(),
    ).toBeVisible({ timeout: 10_000 });
    await screenshot(page, "05-service-position-modified");
  });
});
