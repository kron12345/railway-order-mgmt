import { expect, Page, test } from "@playwright/test";

// ── Credentials ────────────────────────────────────────────────────
const KC_USER = "sebastian";
const KC_PASS = "sebastian";

// ── Bilingual label matchers (DE/EN) ───────────────────────────────
const NEW_ORDER_BTN = /\+?\s*(Neuer Auftrag|New Order)/i;
const ORDER_NUMBER_LABEL = /^(Auftragsnummer|Order Number)$/i;
const ORDER_NAME_LABEL = /^(Auftragsname|Order Name)$/i;
const TIMETABLE_BUTTON = /\+\s*(Fahrplan|Timetable)/i;
const POSITION_NAME_LABEL = /^(Positionsname|Position Name)$/i;
const FROM_LABEL = /^(Von|From)$/i;
const TO_LABEL = /^(Nach|To)$/i;
const DEPARTURE_ANCHOR_LABEL = /^(Abfahrt am Start|Departure at origin)$/i;
const CALCULATE_ROUTE_BTN = /^(Route berechnen|Calculate route)$/i;
const ALL_LABEL = /^(Alle|All)$/i;
const SAVE_BTN = /^(Speichern|Save)$/i;
const APPLY_BTN = /^(Anwenden|Apply|Übernehmen)$/i;

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
  await page
    .evaluate(() => {
      // Hide Vaadin dev-tools overlay
      const dt = document.querySelector("vaadin-dev-tools");
      if (dt) (dt as HTMLElement).style.display = "none";
      // Hide Vaadin Copilot overlay which can intercept pointer events
      const copilot = document.querySelector("copilot-main");
      if (copilot) (copilot as HTMLElement).style.display = "none";
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
    path: `test-results/editing-${name}.png`,
    fullPage: true,
  });
}

/** Returns the grid item count via the Vaadin Grid internal API. */
async function getGridSize(page: Page): Promise<number> {
  return page.evaluate(() => {
    const g = document.querySelector("vaadin-grid") as any;
    if (!g) return 0;
    if (typeof g.size === "number") return g.size;
    if (typeof g._effectiveSize === "number") return g._effectiveSize;
    if (Array.isArray(g.items)) return g.items.length;
    return 0;
  });
}

/**
 * Selects a grid row by index using the Vaadin Grid JS API.
 */
async function selectGridRow(page: Page, rowIndex: number) {
  await page.evaluate((idx) => {
    const grid = document.querySelector("vaadin-grid") as any;
    if (!grid) throw new Error("No vaadin-grid found");
    const items = grid.items || grid._cache?.items;
    if (!items || !items[idx]) {
      throw new Error(`Row ${idx} not found in grid (size=${items?.length})`);
    }
    grid.selectedItems = [items[idx]];
    grid.activeItem = items[idx];
  }, rowIndex);
}

/**
 * Clicks a grid action button by icon name (e.g. "vaadin:plus", "vaadin:trash").
 */
async function clickGridActionButton(page: Page, iconName: string) {
  await page.evaluate((icon) => {
    const allIcons = document.querySelectorAll("vaadin-icon");
    for (const el of allIcons) {
      if (el.getAttribute("icon") === icon) {
        const btn = el.closest("vaadin-button");
        if (btn && (btn as HTMLElement).offsetParent !== null) {
          (btn as HTMLElement).click();
          return;
        }
      }
    }
    throw new Error(`No button with icon "${icon}" found`);
  }, iconName);
}

/**
 * Counts visible grid action buttons with the given icon name.
 */
async function countGridActionButtons(
  page: Page,
  iconName: string,
): Promise<number> {
  return page.evaluate((icon) => {
    let count = 0;
    const allIcons = document.querySelectorAll("vaadin-icon");
    for (const el of allIcons) {
      if (el.getAttribute("icon") === icon) {
        const btn = el.closest("vaadin-button");
        if (btn && (btn as HTMLElement).offsetParent !== null) {
          count++;
        }
      }
    }
    return count;
  }, iconName);
}

/**
 * Finds a visible combo box by its label pattern and returns its index.
 */
async function findComboBoxByLabel(
  page: Page,
  labelPattern: RegExp,
): Promise<number> {
  const pattern = labelPattern.source;
  const flags = labelPattern.flags;
  return page.evaluate(
    ({ p, f }) => {
      const re = new RegExp(p, f);
      const combos = document.querySelectorAll("vaadin-combo-box");
      for (let i = 0; i < combos.length; i++) {
        const label = (combos[i] as any).label || "";
        if (
          re.test(label) &&
          (combos[i] as HTMLElement).offsetParent !== null
        ) {
          return i;
        }
      }
      return -1;
    },
    { p: pattern, f: flags },
  );
}

// ── Test Data ───────────────────────────────────────────────────────
const TS = Date.now();
const ORDER_NR = `TTE-${TS}`;
const ORDER_NAME = `Timetable Editing Test ${TS}`;
const POSITION_NAME = `E2E Editing ${TS}`;

// ── Timetable Editing Tests ─────────────────────────────────────────

test.describe("Timetable Editing: Grid operations on Step 2", () => {
  test.describe.configure({ mode: "serial" });

  let page: Page;
  let orderUrl: string;
  let initialRowCount: number;

  test.beforeAll(async ({ browser }) => {
    // Wide and tall viewport ensures all grid columns (including action column)
    // are rendered and the grid is visible below the validity calendar.
    page = await browser.newPage({ viewport: { width: 1920, height: 1200 } });
    await login(page);

    // ── Create a new order for this test suite ──────────────────────
    await page.goto("/orders");
    await page.waitForTimeout(1_000);
    await page.getByRole("button", { name: NEW_ORDER_BTN }).click();
    await page.waitForURL(/\/orders\/new/, { timeout: 10_000 });

    await page.getByLabel(ORDER_NUMBER_LABEL).fill(ORDER_NR);
    await page.getByLabel(ORDER_NAME_LABEL).fill(ORDER_NAME);

    // Set validity dates: today + 3 months
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

    // ── Navigate to timetable builder ───────────────────────────────
    await dismissDevBanner(page);
    await page.getByRole("button", { name: TIMETABLE_BUTTON }).click();
    await page.waitForURL(/\/timetable-builder/, { timeout: 20_000 });

    // Fill position name
    await page.getByLabel(POSITION_NAME_LABEL).fill(POSITION_NAME);

    // Select route: Olten -> Aarau
    await selectComboBoxOption(page, FROM_LABEL, "Olten", "Olten (CH00218)");
    await selectComboBoxOption(page, TO_LABEL, "Aarau", "Aarau GB (CH02136)");

    // Set departure time
    await page.getByLabel(DEPARTURE_ANCHOR_LABEL).fill("08:00");

    // Calculate route
    await dismissDevBanner(page);
    await page
      .getByRole("button", { name: CALCULATE_ROUTE_BTN })
      .click({ force: true });

    // Wait for grid (Step 2) to appear
    await expect(page.locator("vaadin-grid")).toBeVisible({ timeout: 30_000 });
    await page.waitForTimeout(1_500);

    // Select "All" validity dates
    await page.getByText(ALL_LABEL, { exact: true }).click();
    await page.waitForTimeout(500);

    // Collapse the metadata accordion to give more space to the grid
    const metadataAccordion = page.locator("vaadin-details").first();
    const isOpen = await metadataAccordion.evaluate(
      (el) => (el as any).opened,
    );
    if (isOpen) {
      // Click the summary to collapse
      await metadataAccordion
        .locator("[slot='summary'], vaadin-details-summary")
        .first()
        .click();
      await page.waitForTimeout(300);
    }
  });

  test.afterAll(async () => {
    await page.close();
  });

  // ── Test 1: Verify grid has timetable rows ─────────────────────

  test("1. verify grid has timetable rows", async () => {
    const grid = page.locator("vaadin-grid");
    await expect(grid).toBeVisible({ timeout: 10_000 });

    initialRowCount = await getGridSize(page);
    expect(initialRowCount).toBeGreaterThanOrEqual(2);
    console.log(`Grid has ${initialRowCount} rows (Olten -> ... -> Aarau)`);

    // Verify the origin (Olten) is shown in the editor panel title
    await expect(
      page.getByText(/Olten.*Origin|Olten.*Startpunkt/i),
    ).toBeVisible({ timeout: 5_000 });

    // Verify the route summary mentions the point count
    await expect(
      page.getByText(
        new RegExp(
          `${initialRowCount} points|${initialRowCount} Punkte`,
          "i",
        ),
      ),
    ).toBeVisible({ timeout: 5_000 });

    await screenshot(page, "01-grid-loaded");
  });

  // ── Test 2: Add a stop between existing rows ───────────────────

  test("2. add a stop between existing rows", async () => {
    await dismissDevBanner(page);

    // Scroll the grid into view first — the validity calendar may push it below the fold
    await page.locator("vaadin-grid").scrollIntoViewIfNeeded();
    await page.waitForTimeout(1_000);

    // Click the first "+" (insert) button in the grid action column
    await clickGridActionButton(page, "vaadin:plus");
    await page.waitForTimeout(1_000);

    // Find the operational point combo box by its label
    const pointComboIdx = await findComboBoxByLabel(
      page,
      /punkt|point|select/i,
    );

    let comboToUse: ReturnType<Page["locator"]>;
    if (pointComboIdx >= 0) {
      comboToUse = page.locator("vaadin-combo-box").nth(pointComboIdx);
    } else {
      const allCombos = page.locator("vaadin-combo-box");
      const count = await allCombos.count();
      comboToUse = allCombos.nth(count - 2);
    }

    await comboToUse.click();
    await comboToUse.locator("input").fill("D\u00e4niken");

    const opOption = page
      .locator("vaadin-combo-box-item")
      .filter({ hasText: /D.niken/ })
      .first();

    let optionFound = true;
    try {
      await expect(opOption).toBeVisible({ timeout: 5_000 });
    } catch {
      optionFound = false;
    }

    if (optionFound) {
      await opOption.click();
    } else {
      await comboToUse.locator("input").clear();
      await comboToUse.locator("input").fill("Dulliken");
      const dullikenOpt = page
        .locator("vaadin-combo-box-item")
        .filter({ hasText: /Dulliken/ })
        .first();
      await expect(dullikenOpt).toBeVisible({ timeout: 5_000 });
      await dullikenOpt.click();
    }

    // Select activity "0001"
    const activityComboIdx = await findComboBoxByLabel(
      page,
      /haltegrund|activity|reason/i,
    );

    let actCombo: ReturnType<Page["locator"]>;
    if (activityComboIdx >= 0) {
      actCombo = page.locator("vaadin-combo-box").nth(activityComboIdx);
    } else {
      const allCombos = page.locator("vaadin-combo-box");
      const count = await allCombos.count();
      actCombo = allCombos.nth(count - 1);
    }

    await actCombo.click();
    await actCombo.locator("input").fill("0001");
    const activityOption = page
      .locator("vaadin-combo-box-item")
      .filter({ hasText: /0001/ })
      .first();
    await expect(activityOption).toBeVisible({ timeout: 5_000 });
    await activityOption.click();

    // Click the "Add stop" button
    await dismissDevBanner(page);
    const addBtn = page
      .locator("vaadin-button")
      .filter({ hasText: /Halt hinzuf|Add stop/i })
      .first();
    if ((await addBtn.count()) > 0) {
      await addBtn.click({ force: true });
    } else {
      await page.evaluate(() => {
        const buttons = document.querySelectorAll("vaadin-button");
        for (const btn of buttons) {
          const icon = btn.querySelector("vaadin-icon");
          if (
            icon &&
            icon.getAttribute("icon") === "vaadin:check" &&
            (btn as HTMLElement).offsetParent !== null
          ) {
            (btn as HTMLElement).click();
            return;
          }
        }
      });
    }
    await page.waitForTimeout(1_500);

    // Verify grid now has one more row
    const newRowCount = await getGridSize(page);
    expect(newRowCount).toBe(initialRowCount + 1);
    initialRowCount = newRowCount;

    await screenshot(page, "02-stop-added");
  });

  // ── Test 3: Edit arrival time on a row ─────────────────────────

  test("3. edit arrival time on a row", async () => {
    await dismissDevBanner(page);

    // Select the second row (index 1)
    await selectGridRow(page, 1);
    await page.waitForTimeout(1_000);

    // Verify the editor shows row 2
    await expect(page.getByText(/^2\./)).toBeVisible({ timeout: 5_000 });

    // Set arrival mode to EXACT
    await page.evaluate(() => {
      const selects = document.querySelectorAll("vaadin-select");
      for (const sel of selects) {
        const label = (sel as any).label || "";
        if (
          /ankunft|arrival mode/i.test(label) &&
          (sel as HTMLElement).offsetParent !== null
        ) {
          (sel as any).value = "EXACT";
          sel.dispatchEvent(new Event("change", { bubbles: true }));
          return;
        }
      }
    });
    await page.waitForTimeout(500);

    // Set arrival time to 08:15
    await page.evaluate(() => {
      const timePickers = document.querySelectorAll("vaadin-time-picker");
      for (const tp of timePickers) {
        const label = (tp as any).label || "";
        if (
          /genau|^exact$/i.test(label) &&
          (tp as HTMLElement).offsetParent !== null
        ) {
          (tp as any).value = "08:15";
          tp.dispatchEvent(new Event("change", { bubbles: true }));
          return;
        }
      }
    });
    await page.waitForTimeout(500);

    // Click "Apply"
    await dismissDevBanner(page);
    await clickVaadinButton(page, APPLY_BTN);
    await page.waitForTimeout(1_000);

    await screenshot(page, "03-time-edited");
  });

  // ── Test 4: Toggle halt and select activity ────────────────────

  test("4. toggle halt and select activity", async () => {
    await dismissDevBanner(page);

    // Select a middle row (not origin/destination)
    const rowCount = await getGridSize(page);
    const targetRow = Math.min(3, rowCount - 2);
    await selectGridRow(page, targetRow);
    await page.waitForTimeout(1_000);

    // Find the "Intermediate stop" checkbox
    const haltCheckbox = page
      .locator("vaadin-checkbox")
      .filter({ hasText: /Zwischenhalt|Intermediate stop/i })
      .first();
    await expect(haltCheckbox).toBeVisible({ timeout: 5_000 });

    // Check if currently ON
    const isChecked = await page.evaluate(() => {
      const cbs = document.querySelectorAll("vaadin-checkbox");
      for (const cb of cbs) {
        if (/zwischenhalt|intermediate stop/i.test(cb.textContent || "")) {
          return (cb as any).checked;
        }
      }
      return false;
    });

    // Toggle ON if not checked
    if (!isChecked) {
      await haltCheckbox.click({ force: true });
      await page.waitForTimeout(500);
    }

    // Find the visible activity combo
    const activityComboIdx = await findComboBoxByLabel(
      page,
      /haltegrund|reason|activity/i,
    );
    expect(activityComboIdx).toBeGreaterThanOrEqual(0);
    const actCombo = page.locator("vaadin-combo-box").nth(activityComboIdx);
    await expect(actCombo).toBeVisible({ timeout: 5_000 });

    // Select activity "0001"
    await dismissDevBanner(page);
    await actCombo.click({ force: true });
    await actCombo.locator("input").fill("0001");
    const actOption = page
      .locator("vaadin-combo-box-item")
      .filter({ hasText: /0001/ })
      .first();
    await expect(actOption).toBeVisible({ timeout: 5_000 });
    await dismissDevBanner(page);
    await actOption.click({ force: true });
    await page.waitForTimeout(500);

    // Click Apply
    await dismissDevBanner(page);
    await clickVaadinButton(page, APPLY_BTN);
    await page.waitForTimeout(1_000);

    await screenshot(page, "04-halt-activity");
  });

  // ── Test 5: Soft-delete a stop ─────────────────────────────────

  test("5. soft-delete a stop", async () => {
    await dismissDevBanner(page);

    // Click the first trash button in the grid
    await clickGridActionButton(page, "vaadin:trash");
    await page.waitForTimeout(1_000);

    // Verify at least one undo (arrow-backward) button appeared
    const undoCount = await countGridActionButtons(
      page,
      "vaadin:arrow-backward",
    );
    expect(undoCount).toBeGreaterThan(0);

    await screenshot(page, "05-stop-deleted");
  });

  // ── Test 6: Save the modified timetable ────────────────────────

  test("6. save the modified timetable", async () => {
    await dismissDevBanner(page);
    await clickVaadinButton(page, SAVE_BTN);

    // Should navigate back to order detail
    await page.waitForURL(/\/orders\/[0-9a-f-]+$/, { timeout: 20_000 });

    // The position should appear in the list
    await expect(page.getByText(POSITION_NAME)).toBeVisible({
      timeout: 15_000,
    });

    await screenshot(page, "06-saved");
  });

  // ── Test 7: Cleanup — delete the entire test order ─────────────

  test("7. cleanup — delete the test order", async () => {
    await page.goto(orderUrl);
    await expect(page.getByText(ORDER_NR)).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(1_000);
    await dismissDevBanner(page);

    // Click the delete (trash) button on the order header
    const deleteBtn = page.locator("vaadin-button").filter({
      has: page.locator("vaadin-icon[icon*='trash']"),
    });
    await deleteBtn.first().click();

    // Confirm deletion in the dialog
    const confirmOverlay = page.locator("vaadin-confirm-dialog-overlay");
    await expect(confirmOverlay).toBeVisible({ timeout: 5_000 });
    await screenshot(page, "07-delete-confirmation");

    await dismissDevBanner(page);
    await confirmOverlay
      .locator("vaadin-button")
      .filter({ hasText: /Delete|Löschen/ })
      .click({ force: true });

    // Should navigate back to orders list
    await page.waitForURL(/\/orders$/, { timeout: 20_000 });

    // Verify order is gone
    await page.waitForTimeout(1_000);
    await expect(page.getByText(ORDER_NR)).not.toBeVisible({ timeout: 5_000 });

    await screenshot(page, "07-cleanup");
  });
});
