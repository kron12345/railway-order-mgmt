import { expect, Page, test } from "@playwright/test";

// ── Credentials ────────────────────────────────────────────────────
const KC_USER = "sebastian";
const KC_PASS = "sebastian";

// ── Bilingual label matchers (DE/EN) ───────────────────────────────
const NEW_ORDER_BTN = /\+?\s*(Neuer Auftrag|New Order)/i;
const ORDER_NUMBER_LABEL = /^(Auftragsnummer|Order Number)$/i;
const ORDER_NAME_LABEL = /^(Auftragsname|Order Name)$/i;
const VALID_FROM_LABEL = /^(Gültig ab|Valid from)$/i;
const VALID_TO_LABEL = /^(Gültig bis|Valid to)$/i;
const SAVE_BTN = /^(Speichern|Save)$/i;
const CANCEL_BTN = /^(Abbrechen|Cancel)$/i;
const DELETE_BTN = /^(Löschen|Delete)$/i;
const EDIT_BTN = /^(Bearbeiten|Edit)$/i;

// Positions
const ADD_SERVICE_BTN = /\+\s*(Leistung|Service)/i;
const ADD_TIMETABLE_BTN = /\+\s*(Fahrplan|Timetable)/i;
const POSITION_NAME_LABEL = /^(Positionsname|Position Name)$/i;
const SERVICE_TYPE_LABEL = /^(Leistungsart|Service Type)$/i;
const FROM_LABEL = /^(Von|From)$/i;
const TO_LABEL = /^(Nach|To)$/i;
const START_TIME_LABEL = /^(Startzeit|Start Time)$/i;
const END_TIME_LABEL = /^(Endzeit|End Time)$/i;
const DEPARTURE_ANCHOR_LABEL = /^(Abfahrt am Start|Departure at origin)$/i;
const CALCULATE_ROUTE_BTN = /^(Route berechnen|Calculate route)$/i;
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
  // Remove Vaadin dev-mode overlay
  await dismissDevBanner(page);
}

async function selectComboBoxOption(
  page: Page,
  label: RegExp,
  search: string,
  optionText: string | RegExp,
) {
  const combo = page.getByLabel(label).locator("..");
  await expect(combo).toBeVisible();
  await combo.click();
  await combo.locator("input").fill(search);
  const option = page
    .locator("vaadin-combo-box-item")
    .filter({ hasText: optionText })
    .first();
  await expect(option).toBeVisible({ timeout: 10_000 });
  await option.click();
}

async function dismissDevBanner(page: Page) {
  // Vaadin dev-tools uses Shadow DOM — hide it so it doesn't block clicks
  await page
    .evaluate(() => {
      const dt = document.querySelector("vaadin-dev-tools");
      if (dt) (dt as HTMLElement).style.display = "none";
    })
    .catch(() => {});
}

/** Clicks a vaadin-button by its visible text, bypassing overlays. */
async function clickVaadinButton(page: Page, textMatch: RegExp) {
  await dismissDevBanner(page);
  // Use JS click() on the actual DOM element — reliable even with overlays
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
const TIMETABLE_POS_NAME = `Fahrplan Olten-Aarau ${TS}`;
const MODIFIED_SUFFIX = " (mod)";

// ── Full Lifecycle Test ─────────────────────────────────────────────

test.describe("Order Lifecycle: Create -> Positions -> Modify -> Delete", () => {
  test.describe.configure({ mode: "serial" });

  let page: Page;
  let orderUrl: string;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);
  });

  test.afterAll(async () => {
    await page.close();
  });

  // ── Step 1: Create a new Order ──────────────────────────────────

  test("1. create new order", async () => {
    await page.goto("/orders");
    await page.waitForTimeout(1_000);

    await page.getByRole("button", { name: NEW_ORDER_BTN }).click();
    await page.waitForURL(/\/orders\/new/, { timeout: 10_000 });
    await screenshot(page, "01-new-order-form");

    // Fill order form
    await page.getByLabel(ORDER_NUMBER_LABEL).fill(ORDER_NR);
    await page.getByLabel(ORDER_NAME_LABEL).fill(ORDER_NAME);

    // Set validity dates: today + 3 months via Vaadin DatePicker internal API
    const today = new Date();
    const inThreeMonths = new Date(today);
    inThreeMonths.setMonth(inThreeMonths.getMonth() + 3);
    const isoDate = (d: Date) =>
      `${d.getFullYear()}-${(d.getMonth() + 1).toString().padStart(2, "0")}-${d.getDate().toString().padStart(2, "0")}`;

    // Set values via JS on the Vaadin component to ensure they're committed
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

    await screenshot(page, "02-order-form-filled");

    // Click save — try multiple strategies
    await dismissDevBanner(page);
    // Strategy: direct Playwright click with force on the visible button
    const vaadinSave = page.locator("vaadin-button").filter({ hasText: /Save|Speichern/ }).last();
    await vaadinSave.scrollIntoViewIfNeeded();
    await page.waitForTimeout(500);
    await vaadinSave.click({ force: true, position: { x: 30, y: 12 } });

    // If still on same page after 3s, try JS click as fallback
    const currentUrl = page.url();
    await page.waitForTimeout(3_000);
    if (page.url() === currentUrl) {
      console.log("First click didn't navigate, trying JS click...");
      await screenshot(page, "02c-debug-after-first-click");
      await clickVaadinButton(page, /Save|Speichern/);
    }
    await page.waitForURL(/\/orders\/[0-9a-f-]+$/, { timeout: 20_000 });
    orderUrl = page.url();

    // Verify order detail loaded
    await expect(page.getByText(ORDER_NR)).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText(ORDER_NAME)).toBeVisible();
    await screenshot(page, "03-order-created");
  });

  // ── Step 2: Create a Service Position (Leistung) ────────────────

  test("2. create service position (Leistung)", async () => {
    await page.goto(orderUrl);
    await expect(page.getByText(ORDER_NR)).toBeVisible({ timeout: 10_000 });

    // Click "+ Leistung" button
    await page.getByRole("button", { name: ADD_SERVICE_BTN }).click();

    // Wait for service position dialog to open
    const dialog = page.getByRole("dialog", { name: /New Position|Neue Position/i });
    await expect(dialog).toBeVisible({ timeout: 10_000 });
    await screenshot(page, "04-service-dialog-empty");

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

    await screenshot(page, "05-service-dialog-filled");

    // Save
    await dismissDevBanner(page);
    await dialog.locator("vaadin-button").filter({ hasText: /Save|Speichern/ }).click({ force: true });
    await expect(dialog).not.toBeVisible({ timeout: 10_000 });

    // Verify position appears in the list
    await page.waitForTimeout(2_000);
    await expect(page.getByText(SERVICE_POS_NAME).first()).toBeVisible({
      timeout: 15_000,
    });
    await screenshot(page, "06-service-position-created");
  });

  // ── Step 3: Create a Timetable Position (Fahrplan) ──────────────

  test("3. create timetable position (Fahrplan) with route on map", async () => {
    await page.goto(orderUrl);
    await expect(page.getByText(ORDER_NR)).toBeVisible({ timeout: 10_000 });

    // Click "+ Fahrplan" button
    await page.getByRole("button", { name: ADD_TIMETABLE_BTN }).click();
    await page.waitForURL(/\/timetable-builder/, { timeout: 20_000 });
    await screenshot(page, "07-timetable-builder-empty");

    // Fill position name (in the Details accordion)
    await page.getByLabel(POSITION_NAME_LABEL).fill(TIMETABLE_POS_NAME);

    // Select route: Olten -> Aarau (known working route from existing test)
    await selectComboBoxOption(page, FROM_LABEL, "Olten", "Olten (CH00218)");
    await selectComboBoxOption(page, TO_LABEL, "Aarau", "Aarau GB (CH02136)");

    // Set departure time
    await page.getByLabel(DEPARTURE_ANCHOR_LABEL).fill("09:00");

    await screenshot(page, "08-timetable-route-form");

    // Calculate route
    await dismissDevBanner(page);
    await page.getByRole("button", { name: CALCULATE_ROUTE_BTN }).click({ force: true });

    // Wait for table step to appear (grid with timetable rows)
    await expect(page.locator("vaadin-grid")).toBeVisible({ timeout: 30_000 });
    await screenshot(page, "09-timetable-table-step");

    // Go back to route step to verify map
    await page
      .getByRole("button", { name: /Route|Schritt 1|Step 1/i })
      .click();
    await page.waitForTimeout(1_500);

    // Verify route is displayed on the map (check for Leaflet elements)
    const mapContainer = page.locator("rom-timetable-map");
    await expect(mapContainer).toBeVisible({ timeout: 10_000 });

    // Check that route polyline exists (Leaflet renders SVG paths)
    const routePath = page.locator(
      "rom-timetable-map path.leaflet-interactive",
    );
    await expect(routePath.first()).toBeVisible({ timeout: 10_000 });

    // Check that station markers exist
    const markers = page.locator("rom-timetable-map .rtm-marker");
    const markerCount = await markers.count();
    expect(markerCount).toBeGreaterThan(1);

    // Check route info overlay exists
    const overlay = page.locator("rom-timetable-map .rtm-overlay");
    await expect(overlay).toBeVisible();
    await expect(overlay).toContainText("km");

    await screenshot(page, "10-timetable-map-with-route");

    // Go to table step, select all dates, save
    await page
      .getByRole("button", { name: /^(Weiter|Next)$/i })
      .click();
    await expect(page.locator("vaadin-grid")).toBeVisible({ timeout: 20_000 });

    await page.getByText(ALL_LABEL, { exact: true }).click();
    await page.getByRole("button", { name: SAVE_BTN }).click();

    // Should navigate back to order detail
    await page.waitForURL(/\/orders\/[0-9a-f-]+$/, { timeout: 20_000 });
    await expect(page.getByText(TIMETABLE_POS_NAME)).toBeVisible({
      timeout: 10_000,
    });
    await screenshot(page, "11-timetable-position-created");
  });

  // ── Step 3b: View Timetable Archive ──────────────────────────────

  test("3b. view timetable archive", async () => {
    await page.goto(orderUrl);
    await expect(page.getByText(ORDER_NR)).toBeVisible({ timeout: 10_000 });

    // Find the FAHRPLAN position row and click the eye icon (view button)
    await dismissDevBanner(page);
    await page.evaluate(() => {
      // Find the FAHRPLAN/TIMETABLE badge, then locate the eye icon in the same row
      const spans = document.querySelectorAll("span");
      for (const span of spans) {
        if (/^(TIMETABLE|FAHRPLAN)$/i.test(span.textContent?.trim() || "")) {
          let container: HTMLElement | null = span.closest("div");
          for (let i = 0; i < 8 && container; i++) {
            const eyeIcon = container.querySelector(
              'vaadin-icon[icon*="eye"]',
            );
            if (eyeIcon) {
              const btn = eyeIcon.closest("vaadin-button") as HTMLElement;
              if (btn) {
                btn.click();
                return;
              }
              (eyeIcon as HTMLElement).click();
              return;
            }
            container = container.parentElement?.closest("div") as HTMLElement | null;
          }
        }
      }
      throw new Error("Could not find eye icon button for FAHRPLAN position");
    });

    // Wait for the archive view URL
    await page.waitForURL(/\/orders\/[0-9a-f-]+\/timetable\/[0-9a-f-]+/, {
      timeout: 20_000,
    });

    // Verify the archive view loaded with position name
    await expect(page.getByText(TIMETABLE_POS_NAME)).toBeVisible({
      timeout: 10_000,
    });

    // Verify the timetable table is present (Div-based grid rows)
    const archiveTable = page.locator(
      'div[style*="grid-template-columns"]',
    );
    await expect(archiveTable.first()).toBeVisible({ timeout: 10_000 });

    // Verify the map is present
    const map = page.locator("rom-timetable-map");
    await expect(map).toBeVisible({ timeout: 10_000 });

    // Verify an Edit/Bearbeiten button exists
    const editBtn = page
      .locator("vaadin-button")
      .filter({ hasText: /Edit|Bearbeiten/i });
    await expect(editBtn.first()).toBeVisible({ timeout: 5_000 });

    await screenshot(page, "11b-timetable-archive-view");

    // Click the Edit button to navigate to the builder
    await dismissDevBanner(page);
    await editBtn.first().click({ force: true });

    // Verify navigation to timetable-builder
    await page.waitForURL(/\/timetable-builder/, { timeout: 20_000 });
    await screenshot(page, "11c-archive-to-builder");

    // Navigate back to order detail
    await page.goto(orderUrl);
    await expect(page.getByText(ORDER_NR)).toBeVisible({ timeout: 10_000 });
  });

  // ── Step 4: Modify the Service Position ─────────────────────────

  test("4. modify service position", async () => {
    await page.goto(orderUrl);
    await expect(page.getByText(SERVICE_POS_NAME)).toBeVisible({
      timeout: 10_000,
    });

    // Edit icons on the page: [0]=Order header Edit, [1]=Service pos edit, [2]=Timetable pos edit
    // Click the edit icon for the service position (index 1)
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

    await screenshot(page, "12-service-edit-dialog");

    // Save
    await dismissDevBanner(page);
    await dialog.locator("vaadin-button").filter({ hasText: /Save|Speichern/ }).click({ force: true });
    await expect(dialog).not.toBeVisible({ timeout: 10_000 });

    // Verify modified name
    await page.waitForTimeout(1_000);
    await expect(
      page.getByText(SERVICE_POS_NAME + MODIFIED_SUFFIX).first(),
    ).toBeVisible({ timeout: 10_000 });
    await screenshot(page, "13-service-position-modified");
  });

  // ── Step 5: Modify the Timetable Position ───────────────────────

  test("5. modify timetable position", async () => {
    await page.goto(orderUrl);
    await expect(page.getByText(TIMETABLE_POS_NAME)).toBeVisible({
      timeout: 10_000,
    });

    // Click the timetable position's edit icon via JS — find the row with TIMETABLE badge
    await dismissDevBanner(page);
    await page.evaluate(() => {
      // Find all spans containing "TIMETABLE" or "Fahrplan" badge text
      const spans = document.querySelectorAll("span");
      for (const span of spans) {
        if (/^TIMETABLE$/i.test(span.textContent?.trim() || "")) {
          // Navigate up to the position row container and find the edit icon
          let container = span.closest("div");
          for (let i = 0; i < 5 && container; i++) {
            const editIcon = container.querySelector(
              'vaadin-icon[icon*="edit"], vaadin-icon[icon*="pencil"]',
            );
            if (editIcon) {
              (editIcon as HTMLElement).click();
              return;
            }
            container = container.parentElement?.closest("div") || null;
          }
        }
      }
    });

    // Should navigate to timetable builder
    await page.waitForURL(/\/timetable-builder/, { timeout: 20_000 });

    // Modify position name (open metadata accordion if collapsed)
    const nameField = page.getByLabel(POSITION_NAME_LABEL);
    if (!(await nameField.isVisible())) {
      // Click Details summary to open it
      await page
        .locator("vaadin-details")
        .first()
        .locator("[slot='summary'], summary")
        .click();
    }
    await nameField.clear();
    await nameField.fill(TIMETABLE_POS_NAME + MODIFIED_SUFFIX);

    await screenshot(page, "14-timetable-edit");

    // Save
    await page.getByRole("button", { name: SAVE_BTN }).click();
    await page.waitForURL(/\/orders\/[0-9a-f-]+$/, { timeout: 20_000 });

    await expect(
      page.getByText(TIMETABLE_POS_NAME + MODIFIED_SUFFIX),
    ).toBeVisible({ timeout: 10_000 });
    await screenshot(page, "15-timetable-position-modified");
  });

  // ── Step 6: Delete the Order ────────────────────────────────────

  test("6. delete order with all positions", async () => {
    await page.goto(orderUrl);
    await expect(page.getByText(ORDER_NR)).toBeVisible({ timeout: 10_000 });
    await screenshot(page, "16-order-before-delete");

    // Click delete button (trash icon in the header)
    const deleteBtn = page.locator("vaadin-button").filter({
      has: page.locator('vaadin-icon[icon*="trash"]'),
    });
    await deleteBtn.first().click();

    // Confirm deletion in the dialog
    // Wait for confirm dialog overlay
    const confirmOverlay = page.locator("vaadin-confirm-dialog-overlay");
    await expect(confirmOverlay).toBeVisible({ timeout: 5_000 });
    await screenshot(page, "17-delete-confirmation");

    // Click the red "Delete" confirm button
    await dismissDevBanner(page);
    await confirmOverlay.locator("vaadin-button").filter({ hasText: /Delete|Löschen/ }).click({ force: true });

    // Should navigate back to orders list
    await page.waitForURL(/\/orders$/, { timeout: 20_000 });

    // Verify order is gone
    await page.waitForTimeout(1_000);
    await expect(page.getByText(ORDER_NR)).not.toBeVisible({ timeout: 5_000 });
    await screenshot(page, "18-order-deleted");
  });
});
