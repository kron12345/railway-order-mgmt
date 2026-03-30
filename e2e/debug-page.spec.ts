import { test } from "@playwright/test";

const KC_USER = "sebastian";
const KC_PASS = "sebastian";

test("debug: check page state after login and refresh", async ({ page }) => {
    // Login
    await page.goto("/");
    const loginBtn = page.locator('a[href*="oauth2/authorization/keycloak"]');
    await loginBtn.click();
    await page.locator('input[name="username"], #username').fill(KC_USER);
    await page.locator('input[name="password"], #password').fill(KC_PASS);
    await page.locator('#kc-login, input[type="submit"], button[type="submit"]').click();
    await page.waitForURL(/localhost:8085/, { timeout: 30_000 });

    // Wait for Vaadin to fully load
    await page.waitForTimeout(5_000);
    await page.screenshot({ path: "test-results/debug-after-login.png", fullPage: true });
    console.log("After login URL:", page.url());

    // Refresh the page (simulates user hitting F5)
    await page.reload();
    await page.waitForTimeout(5_000);
    await page.screenshot({ path: "test-results/debug-after-refresh.png", fullPage: true });
    console.log("After refresh URL:", page.url());

    // Navigate to different routes
    await page.goto("http://localhost:8085/orders");
    await page.waitForTimeout(3_000);
    await page.screenshot({ path: "test-results/debug-orders-page.png", fullPage: true });
    console.log("Orders page URL:", page.url());

    // Back to dashboard
    await page.goto("http://localhost:8085/");
    await page.waitForTimeout(3_000);
    await page.screenshot({ path: "test-results/debug-back-to-dashboard.png", fullPage: true });
    console.log("Back to dashboard URL:", page.url());

    // Check for console errors
    const errors: string[] = [];
    page.on("console", msg => {
        if (msg.type() === "error") errors.push(msg.text());
    });
    await page.waitForTimeout(2_000);
    if (errors.length) console.log("JS Errors:", errors.join("\n"));

    // Check body content
    const text = await page.locator("body").innerText();
    console.log("Body text:", text.substring(0, 300));
});
