import { expect, Page, test } from "@playwright/test";

const KC_USER = "sebastian";
const KC_PASS = "sebastian";
const ORDER_NUMBER = "4711";
const TIMETABLE_BUTTON = /\+\s*(Fahrplan|Timetable)/i;
const EDIT_BUTTON = /^(Bearbeiten|Edit)$/i;
const POSITION_NAME_LABEL = /^(Positionsname|Position Name)$/i;
const FROM_LABEL = /^(Von|From)$/i;
const TO_LABEL = /^(Nach|To)$/i;
const DEPARTURE_ANCHOR_LABEL = /^(Abfahrt am Start|Departure at origin)$/i;
const CALCULATE_ROUTE_BUTTON = /^(Route berechnen|Calculate route)$/i;
const STEP_TWO_TITLE = /(Schritt 2 · Fahrplan nacharbeiten|Step 2 · Refine Timetable)/i;
const ALL_LABEL = /^(Alle|All)$/i;
const SAVE_BUTTON = /^(Speichern|Save)$/i;

async function login(page: Page) {
    await page.goto("/");
    await page.locator('a[href*="oauth2/authorization/keycloak"]').click();
    await page.locator('input[name="username"], #username').fill(KC_USER);
    await page.locator('input[name="password"], #password').fill(KC_PASS);
    await page.locator('#kc-login, input[type="submit"], button[type="submit"]').click();
    await page.waitForURL(/localhost:8085/, { timeout: 30_000 });
    await expect(page.locator("vaadin-app-layout")).toBeVisible({ timeout: 20_000 });
}

async function selectComboBoxOption(
    page: Page,
    label: RegExp,
    search: string,
    optionText: string | RegExp
) {
    const combo = page.getByLabel(label).locator("..");
    await expect(combo).toBeVisible();
    await combo.click();
    await combo.locator("input").fill(search);

    const option = page.locator("vaadin-combo-box-item").filter(
        typeof optionText === "string" ? { hasText: optionText } : { hasText: optionText }
    ).first();
    await expect(option).toBeVisible({ timeout: 10_000 });
    await option.click();
}

test("create timetable position for order 4711", async ({ page }) => {
    const positionName = `E2E Fahrplan ${Date.now()}`;

    await login(page);
    await page.goto("/orders");

    const orderRow = page.locator("vaadin-details").filter({ hasText: ORDER_NUMBER }).first();
    await expect(orderRow).toBeVisible({ timeout: 20_000 });
    await orderRow.getByRole("button", { name: EDIT_BUTTON }).click();
    await page.waitForURL(/\/orders\/[0-9a-f-]+$/, { timeout: 20_000 });

    await expect(page.getByText(/Order Positions|Auftragspositionen/i)).toBeVisible();
    await page.getByRole("button", { name: TIMETABLE_BUTTON }).click();
    await page.waitForURL(/\/orders\/[0-9a-f-]+\/timetable-builder/, {
        timeout: 20_000,
    });

    await page.getByLabel(POSITION_NAME_LABEL).fill(positionName);
    await selectComboBoxOption(page, FROM_LABEL, "Olten", "Olten (CH00218)");
    await selectComboBoxOption(page, TO_LABEL, "Aarau", "Aarau GB (CH02136)");
    await page.getByLabel(DEPARTURE_ANCHOR_LABEL).fill("08:15");

    await page.getByRole("button", { name: CALCULATE_ROUTE_BUTTON }).click();
    await expect(page.getByText(STEP_TWO_TITLE)).toBeVisible({
        timeout: 20_000,
    });

    await page.getByText(ALL_LABEL, { exact: true }).click();
    await page.getByRole("button", { name: SAVE_BUTTON }).click();

    await page.waitForURL(/\/orders\/[0-9a-f-]+$/, { timeout: 20_000 });
    await expect(page.getByText(positionName)).toBeVisible({ timeout: 20_000 });
});
