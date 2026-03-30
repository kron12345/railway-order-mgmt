import { test, expect } from "@playwright/test";

/**
 * E2E test: Keycloak login flow -> Dashboard visible.
 *
 * Prerequisites:
 *   - App running on http://localhost:8085
 *   - Keycloak reachable at the configured issuer-uri
 *   - Test user "sebastian" / "sebastian" exists in Keycloak realm "railway"
 */

const KC_USER = "sebastian";
const KC_PASS = "sebastian";

test("login via Keycloak and see Dashboard", async ({ page }) => {
    // 1. Navigate to the app
    await page.goto("/");

    // 2. Wait for and click the SSO login button
    const loginButton = page.locator('a[href*="oauth2/authorization/keycloak"]');
    await expect(loginButton).toBeVisible({ timeout: 15_000 });
    console.log("Login view loaded, clicking SSO button...");
    await loginButton.click();

    // 3. Wait for Keycloak login page (look for username field)
    const usernameField = page.locator('input[name="username"], #username');
    await expect(usernameField).toBeVisible({ timeout: 20_000 });
    console.log("Keycloak login page loaded");

    // Take a screenshot of the Keycloak page for debugging
    await page.screenshot({ path: "test-results/keycloak-page.png" });

    // 4. Fill in credentials and submit
    await usernameField.fill(KC_USER);
    const passwordField = page.locator('input[name="password"], #password');
    await passwordField.fill(KC_PASS);

    // Click the login/submit button
    const submitButton = page.locator(
        '#kc-login, input[type="submit"], button[type="submit"]'
    );
    await submitButton.click();
    console.log("Credentials submitted, waiting for redirect...");

    // 5. Wait for redirect back to the app (away from Keycloak)
    await page.waitForURL(/localhost:8085/, { timeout: 30_000 });
    console.log("Redirected back to app: " + page.url());

    // Take a screenshot of what we see after redirect
    await page.screenshot({ path: "test-results/after-redirect.png" });

    // 6. Wait for Vaadin to initialize — the app-layout is the key indicator
    const appLayout = page.locator("vaadin-app-layout");
    await expect(appLayout).toBeVisible({ timeout: 30_000 });
    console.log("Vaadin app-layout visible");

    // 7. Verify Dashboard content is rendered (not a black/empty page)
    const sideNav = page.locator("vaadin-side-nav");
    await expect(sideNav).toBeVisible({ timeout: 10_000 });

    const brand = page.getByText("ROM");
    await expect(brand).toBeVisible();

    // Take a final screenshot of the dashboard
    await page.screenshot({ path: "test-results/dashboard.png" });
    console.log("Dashboard is visible — login flow successful!");
});
