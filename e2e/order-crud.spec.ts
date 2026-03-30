import { test, expect, Page } from "@playwright/test";

const KC_USER = "sebastian";
const KC_PASS = "sebastian";

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

test.describe("Order CRUD — Concept C Accordion", () => {
    test.beforeEach(async ({ page }) => {
        await login(page);
    });

    test("accordion view with heatmap and tiles", async ({ page }) => {
        // Go to orders list
        await page.goto("/orders");
        await page.waitForTimeout(3_000);
        await page.screenshot({ path: "test-results/concept-c-list.png", fullPage: true });

        // Expand first accordion
        const allDetails = page.locator("vaadin-details");
        const count = await allDetails.count();
        console.log(`Found ${count} order accordions`);

        if (count > 0) {
            await allDetails.first().locator("[slot='summary'], summary").first().click();
            await page.waitForTimeout(1_000);
            await page.screenshot({ path: "test-results/concept-c-expanded.png", fullPage: true });

            // Expand all
            for (let i = 1; i < count; i++) {
                await allDetails.nth(i).locator("[slot='summary'], summary").first().click();
                await page.waitForTimeout(500);
            }
            await page.screenshot({ path: "test-results/concept-c-all-expanded.png", fullPage: true });
        }

        // Test narrow viewport (responsive)
        await page.setViewportSize({ width: 768, height: 900 });
        await page.waitForTimeout(500);
        await page.screenshot({ path: "test-results/concept-c-narrow.png", fullPage: true });

        console.log("Concept C accordion view OK");
    });
});
