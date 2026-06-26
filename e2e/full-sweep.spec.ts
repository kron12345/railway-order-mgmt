import { test, expect, Page } from "@playwright/test";
import * as fs from "fs";

/**
 * Full functional sweep (admin): visit every route + exercise the tech-pass-touched paths,
 * screenshot each, and collect console/page errors + Vaadin server-error overlays.
 */

const KC_USER = "sebastian";
const KC_PASS = "sebastian";
const SHOT = "test-results/sweep";

const errors: string[] = [];

function watch(page: Page) {
    page.on("console", (m) => {
        if (m.type() === "error") errors.push(`[console] ${page.url()} :: ${m.text()}`);
    });
    page.on("pageerror", (e) => errors.push(`[pageerror] ${page.url()} :: ${e.message}`));
}

async function login(page: Page) {
    await page.goto("/");
    await page.locator('a[href*="oauth2/authorization/keycloak"]').click();
    const u = page.locator('input[name="username"], #username');
    await expect(u).toBeVisible({ timeout: 20_000 });
    await u.fill(KC_USER);
    await page.locator('input[name="password"], #password').fill(KC_PASS);
    await page.locator('#kc-login, input[type="submit"], button[type="submit"]').click();
    await page.waitForURL(/localhost:8085/, { timeout: 30_000 });
    await expect(page.locator("vaadin-app-layout")).toBeVisible({ timeout: 30_000 });
}

/** Navigate, settle, screenshot, and assert no Vaadin server-error overlay appeared. */
async function visit(page: Page, path: string, name: string) {
    await page.goto(path);
    await page.waitForTimeout(1500);
    await page.screenshot({ path: `${SHOT}/${name}.png`, fullPage: false });
    // Vaadin renders an "Internal error"/exception overlay on a server exception.
    const textOverlay = page.getByText(/Internal error|There was an exception while trying/i);
    const elOverlay = page.locator("vaadin-dev-tools-error, .v-system-error");
    if (
        ((await textOverlay.count()) > 0 && (await textOverlay.first().isVisible())) ||
        ((await elOverlay.count()) > 0 && (await elOverlay.first().isVisible()))
    ) {
        errors.push(`[server-error overlay] ${path}`);
    }
}

test("full functional sweep", async ({ page }) => {
    test.setTimeout(240_000);
    fs.mkdirSync(SHOT, { recursive: true });
    watch(page);
    await login(page);

    // --- every route ---
    await visit(page, "/", "dashboard");
    await visit(page, "/orders", "orders");
    await visit(page, "/businesses", "businesses");
    await visit(page, "/offene-positionen", "offene-positionen");
    await visit(page, "/fristen", "fristen");
    await visit(page, "/pathmanager", "pathmanager");
    await visit(page, "/r2p-inbox", "r2p-inbox");
    await visit(page, "/rollingstock", "rollingstock");
    await visit(page, "/settings", "settings");
    await visit(page, "/profile", "profile");

    // --- order detail + expand a position's resources (exercises ResourcePanel tech change) ---
    await page.goto("/orders");
    const firstOrder = page.locator("#order-list .md-card-wrapper").first();
    await expect(firstOrder).toBeVisible({ timeout: 20_000 });
    await firstOrder.click();
    await page.waitForURL(/\/orders\/[0-9a-f-]{36}/, { timeout: 15_000 });
    await page.waitForTimeout(1500);
    await page.screenshot({ path: `${SHOT}/order-detail.png` });
    // Expand the first position row (lazy body builds the ResourcePanel).
    const posRow = page.locator("vaadin-details, .order-position-row, [class*=position]").first();
    if ((await posRow.count()) > 0) {
        await posRow.first().click().catch(() => {});
        await page.waitForTimeout(1200);
        await page.screenshot({ path: `${SHOT}/order-detail-expanded.png` });
    }

    // --- business detail ---
    await page.goto("/businesses");
    const firstBiz = page.locator("#biz-list .md-card-wrapper").first();
    await expect(firstBiz).toBeVisible({ timeout: 20_000 });
    await firstBiz.click();
    await page.waitForURL(/\/businesses\/[0-9a-f-]{36}/, { timeout: 15_000 });
    await page.waitForTimeout(1200);
    await page.screenshot({ path: `${SHOT}/business-detail.png` });

    // --- ⌃K command palette (exercises the new flat projection) ---
    await page.goto("/orders");
    await expect(page.locator("vaadin-app-layout")).toBeVisible();
    await page.keyboard.press("Control+k");
    await page.waitForTimeout(800);
    const palette = page.locator(".cmd-palette, [class*=command], [class*=palette], input[placeholder]");
    await page.screenshot({ path: `${SHOT}/command-palette.png` });
    // Type a query and screenshot results.
    await page.keyboard.type("a");
    await page.waitForTimeout(600);
    await page.screenshot({ path: `${SHOT}/command-palette-typed.png` });
    await page.keyboard.press("Escape");

    console.log("collected errors:\n" + (errors.length ? errors.join("\n") : "NONE"));
    // Assert no server-error overlays (console errors are reported but not auto-failed — dev mode
    // emits benign noise; we inspect the list manually from the log).
    const serverErrors = errors.filter((e) => e.startsWith("[server-error overlay]") || e.includes("pageerror"));
    expect(serverErrors, "server errors / page exceptions:\n" + serverErrors.join("\n")).toHaveLength(0);
});
