import { test, expect, Page } from "@playwright/test";

/**
 * E2E for the deadline-rules / automatic-businesses feature (K2 vision):
 *   - Admin CRUD for Frist-Regeln (/fristregeln)
 *   - Rules materialize as real "⚙ automatisch" businesses in the Geschäfte list (/businesses)
 *   - Deadline overview with the per-Verkehrstag detail + auto-order trigger (/fristen)
 *
 * Prerequisites: app on :8085, Keycloak reachable, ADMIN user "sebastian", seed rules (V43).
 */

const KC_USER = "sebastian";
const KC_PASS = "sebastian";
const SEED_RULE = "Final-Offer Jahresfahrplan 2027"; // seeded by V43 (locale-independent anchor)

async function login(page: Page) {
    await page.goto("/");
    const loginBtn = page.locator('a[href*="oauth2/authorization/keycloak"]');
    await loginBtn.click();
    await page.locator('input[name="username"], #username').fill(KC_USER);
    await page.locator('input[name="password"], #password').fill(KC_PASS);
    await page.locator('#kc-login, input[type="submit"], button[type="submit"]').click();
    await page.waitForURL(/localhost:8085/, { timeout: 30_000 });
    await page.locator("vaadin-app-layout").waitFor({ state: "visible", timeout: 20_000 });
}

test.describe("Deadline rules & automatic businesses", () => {
    test.beforeEach(async ({ page }) => {
        await login(page);
    });

    test("admin can list and create a deadline rule", async ({ page }) => {
        await page.goto("/fristregeln");
        // The seeded rules are listed in the grid.
        await expect(page.getByText(SEED_RULE).first()).toBeVisible({ timeout: 20_000 });
        await page.screenshot({ path: "test-results/fristregeln-list.png", fullPage: true });

        // Create a new rule via the dialog (name is the only required field; selects keep defaults).
        const ruleName = "E2E-Regel " + Date.now();
        await page.getByRole("button", { name: /Neue Regel|New rule/ }).first().click();
        const overlay = page.locator("vaadin-dialog-overlay");
        await overlay.waitFor({ state: "visible", timeout: 10_000 });
        await overlay.locator("vaadin-text-field input").first().fill(ruleName);
        // Save = the primary button in the dialog footer (locale-independent theme selector).
        await overlay.locator('vaadin-button[theme~="primary"]').click();

        // The new rule shows up in the grid.
        await expect(page.getByText(ruleName).first()).toBeVisible({ timeout: 15_000 });
        await page.screenshot({ path: "test-results/fristregeln-created.png", fullPage: true });
    });

    test("automatic businesses appear in the Geschäfte list", async ({ page }) => {
        await page.goto("/businesses");
        await page.waitForTimeout(2_000);
        // Auto-businesses are marked with the gear glyph (locale-independent) and carry the rule name.
        await expect(page.getByText("⚙").first()).toBeVisible({ timeout: 20_000 });
        await page.screenshot({ path: "test-results/businesses-automatic.png", fullPage: true });
    });

    test("deadline overview shows rules and triggers evaluation", async ({ page }) => {
        await page.goto("/fristen");
        await page.waitForTimeout(2_000);
        // The automatic-business section lists the seeded rules (gear marker).
        await expect(page.getByText("⚙").first()).toBeVisible({ timeout: 20_000 });
        await page.screenshot({ path: "test-results/fristen-overview.png", fullPage: true });

        // Triggering the evaluation shows an inline result banner (and re-syncs the businesses).
        const evaluate = page.getByRole("button", { name: /Regeln auswerten|Evaluate rules/ });
        if (await evaluate.count()) {
            await evaluate.first().click();
            await page.waitForTimeout(1_500);
            await page.screenshot({ path: "test-results/fristen-evaluated.png", fullPage: true });
        }

        // If a rolling FAHRT rule produced a per-Verkehrstag breakdown, it is an expandable detail.
        const details = page.locator("vaadin-details");
        if (await details.count()) {
            await details.first().locator("[slot='summary'], summary").first().click();
            await page.waitForTimeout(500);
            await page.screenshot({ path: "test-results/fristen-perday.png", fullPage: true });
        }
    });
});
