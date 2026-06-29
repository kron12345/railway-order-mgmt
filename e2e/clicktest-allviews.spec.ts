/**
 * Project-wide "every screen renders cleanly" sweep. Each navigable view is opened and checked
 * for (a) no Vaadin server-error overlay and (b) — via the shared fixture — no browser console
 * errors / uncaught page errors. A screenshot of each view is captured for visual review.
 *
 * This complements full-sweep.spec.ts (which ignores console errors) by failing on any browser
 * error, and adds a real sidenav click-through.
 */
import { test, expect, login, dismissDevOverlays, expectNoServerError, shot } from "./fixtures";

const VIEWS: Array<{ path: string; name: string }> = [
  { path: "/", name: "dashboard" },
  { path: "/orders", name: "orders" },
  { path: "/businesses", name: "businesses" },
  { path: "/offene-positionen", name: "open-positions" },
  { path: "/fristen", name: "deadlines" },
  { path: "/fristregeln", name: "deadline-rules" },
  { path: "/r2p-inbox", name: "r2p-inbox" },
  { path: "/pathmanager", name: "path-manager" },
  { path: "/rollingstock", name: "rolling-stock" },
  { path: "/settings", name: "settings" },
  { path: "/profile", name: "profile" },
];

test.describe("All views render clean (no server + no console errors)", () => {
  test.beforeEach(async ({ page }) => {
    await login(page);
  });

  for (const view of VIEWS) {
    test(`view loads cleanly: ${view.name}`, async ({ page }) => {
      await page.goto(view.path);
      await dismissDevOverlays(page);
      await expect(page.locator("vaadin-app-layout")).toBeVisible({ timeout: 15_000 });
      // give async data + push a moment to surface any late error
      await page.waitForTimeout(1200);
      await expectNoServerError(page);
      await shot(page, "view-" + view.name);
      // console/pageerror assertion happens automatically in the fixture teardown
    });
  }

  test("sidenav click-through reaches the top-level views", async ({ page }) => {
    await page.goto("/");
    await dismissDevOverlays(page);
    const targets: Array<[string, RegExp]> = [
      ["Orders", /\/orders$/],
      ["Open positions", /\/offene-positionen$/],
      ["Deadlines", /\/fristen$/],
      ["Businesses", /\/businesses$/],
      ["Dashboard", /localhost:8085\/$/],
    ];
    for (const [name, urlRe] of targets) {
      await page.getByRole("link", { name, exact: true }).first().click();
      await expect(page).toHaveURL(urlRe, { timeout: 10_000 });
      await expectNoServerError(page);
    }
  });
});
