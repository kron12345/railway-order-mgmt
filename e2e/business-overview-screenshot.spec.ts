import { test } from "@playwright/test";

/**
 * Local-only screenshot capture for design review. Skipped by default — set
 * RUN_SCREENSHOTS=true and provide BIZ_KC_USER / BIZ_KC_PASS / BIZ_BUSINESS_ID
 * env vars before running. Hard-coded fixtures would otherwise break CI and
 * other developer machines.
 */
const RUN_SCREENSHOTS = process.env.RUN_SCREENSHOTS === "true";
const KC_USER = process.env.BIZ_KC_USER ?? "";
const KC_PASS = process.env.BIZ_KC_PASS ?? "";
const EXISTING_BUSINESS_ID = process.env.BIZ_BUSINESS_ID ?? "";

test.use({ viewport: { width: 1920, height: 1080 } });

test("screenshots: business overview master-detail", async ({ page }) => {
    test.skip(!RUN_SCREENSHOTS,
            "Set RUN_SCREENSHOTS=true and BIZ_KC_USER/PASS/BUSINESS_ID to enable.");
    test.skip(!KC_USER || !KC_PASS || !EXISTING_BUSINESS_ID,
            "Missing BIZ_KC_USER / BIZ_KC_PASS / BIZ_BUSINESS_ID env vars.");
    test.setTimeout(180_000);

    await page.goto("/businesses");
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(1500);
    await page.screenshot({ path: "playwright-report/biz-debug-1-initial.png" });
    const loginAnchor = page.locator('a[href*="oauth2/authorization/keycloak"]').first();
    await loginAnchor.waitFor({ state: "visible", timeout: 20_000 });
    await loginAnchor.click();
    await page.locator('input[name="username"], #username').waitFor({ timeout: 30_000 });
    await page.screenshot({ path: "playwright-report/biz-debug-2-login.png" });
    await page.locator('input[name="username"], #username').fill(KC_USER);
    await page.locator('input[name="password"], #password').fill(KC_PASS);
    await page
        .locator('#kc-login, input[type="submit"], button[type="submit"]')
        .click();
    await page.waitForURL(/localhost:8085/, { timeout: 30_000 });
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(1500);
    await page.screenshot({ path: "playwright-report/biz-debug-3-postlogin.png" });
    await page.locator("vaadin-app-layout").waitFor({ timeout: 30_000 });

    // /businesses without selection
    await page.goto("/businesses");
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(1500);
    await page.screenshot({ path: "playwright-report/biz-list-only.png", fullPage: false });

    // /businesses/{id} → ReadView right
    await page.goto(`/businesses/${EXISTING_BUSINESS_ID}`);
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(2000);
    await page.screenshot({ path: "playwright-report/biz-master-detail.png", fullPage: false });

    // hover the first card to capture the assignee dropdown affordance
    const firstCard = page.locator(".md-card-wrapper").first();
    await firstCard.hover();
    await page.waitForTimeout(400);
    await page.screenshot({ path: "playwright-report/biz-card-hover.png", fullPage: false });

    // /businesses/{id}/edit → form right
    await page.goto(`/businesses/${EXISTING_BUSINESS_ID}/edit`);
    await page.waitForLoadState("networkidle");
    await page.waitForTimeout(1500);
    await page.screenshot({ path: "playwright-report/biz-edit.png", fullPage: false });
});
