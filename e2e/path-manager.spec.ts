import { expect, Page, test } from "@playwright/test";

// ── Credentials ────────────────────────────────────────────────────
const KC_USER = "sebastian";
const KC_PASS = "sebastian";

const BASE_API = "http://localhost:8085";

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

async function dismissDevBanner(page: Page) {
  await page
    .evaluate(() => {
      const dt = document.querySelector("vaadin-dev-tools");
      if (dt) (dt as HTMLElement).style.display = "none";
    })
    .catch(() => {});
}

async function screenshot(page: Page, name: string) {
  await page.screenshot({
    path: `test-results/pm-${name}.png`,
    fullPage: true,
  });
}

/**
 * Executes a fetch() in the browser context so cookies & CSRF tokens
 * are sent automatically. Returns {status, body}.
 */
async function apiFetch(
  page: Page,
  method: string,
  path: string,
  body?: unknown,
): Promise<{ status: number; body: unknown }> {
  return page.evaluate(
    async ({ method, url, body }) => {
      const opts: RequestInit = {
        method,
        headers: { "Content-Type": "application/json", Accept: "application/json" },
      };
      if (body !== undefined) {
        opts.body = JSON.stringify(body);
      }
      const resp = await fetch(url, opts);
      const text = await resp.text();
      let parsed: unknown;
      try {
        parsed = JSON.parse(text);
      } catch {
        parsed = text;
      }
      return { status: resp.status, body: parsed };
    },
    { method, url: `${path}`, body },
  );
}

// ── Test Data ───────────────────────────────────────────────────────
const TS = Date.now();
const OTN = `99${String(TS).slice(-3)}`; // short unique train number

// ── Path Manager E2E Test ──────────────────────────────────────────

test.describe("Path Manager: Basic Navigation and REST API", () => {
  test.describe.configure({ mode: "serial" });

  let page: Page;
  let trainId: string;

  test.beforeAll(async ({ browser }) => {
    page = await browser.newPage();
    await login(page);
  });

  test.afterAll(async () => {
    await page.close();
  });

  // ── 01: Navigate to Path Manager via sidebar ────────────────────

  test("01. navigate to Path Manager via sidebar", async () => {
    // Click the Path Manager link in the sidebar navigation
    const pmLink = page.locator("vaadin-side-nav-item").filter({
      hasText: /Path Manager|Fahrplanmanager/i,
    });
    await pmLink.click();

    await page.waitForURL(/\/pathmanager/, { timeout: 15_000 });
    expect(page.url()).toContain("/pathmanager");

    // Verify page loaded — TreeGrid or a heading should be visible
    const treeGrid = page.locator("vaadin-grid-tree-toggle, vaadin-grid");
    await expect(treeGrid.first()).toBeVisible({ timeout: 15_000 });

    await screenshot(page, "01-main-view");
  });

  // ── 02: Verify timetable year 2026 exists ───────────────────────

  test("02. verify timetable year 2026 exists", async () => {
    await page.goto("/pathmanager");
    await page.waitForTimeout(2_000);

    // The TreeGrid should show "Fahrplanjahr 2026" or "Timetable Year 2026"
    const yearNode = page.getByText(/Fahrplanjahr 2026|Timetable Year 2026/i);
    await expect(yearNode.first()).toBeVisible({ timeout: 15_000 });

    await screenshot(page, "02-year-2026");
  });

  // ── 03: Submit a train via REST API ─────────────────────────────

  test("03. submit a train via REST API", async () => {
    const result = await apiFetch(page, "POST", "/api/v1/pathmanager/trains", {
      sourcePositionId: null,
      operationalTrainNumber: OTN,
      trainType: "01",
      trafficTypeCode: "IC",
      locations: [
        {
          sequence: 1,
          countryCodeIso: "CH",
          locationPrimaryCode: "CH00218",
          primaryLocationName: "Olten",
          journeyLocationType: "01",
          departureTime: "08:00",
        },
        {
          sequence: 2,
          countryCodeIso: "CH",
          locationPrimaryCode: "CH02136",
          primaryLocationName: "Aarau GB",
          journeyLocationType: "03",
          arrivalTime: "08:33",
        },
      ],
    });

    expect(result.status).toBe(201);
    const body = result.body as Record<string, unknown>;
    expect(body.id).toBeTruthy();
    expect(body.operationalTrainNumber).toBe(OTN);
    expect(body.processState).toBe("NEW");
    trainId = body.id as string;
  });

  // ── 04: Verify train appears in TreeGrid ────────────────────────

  test("04. verify train appears in TreeGrid", async () => {
    await page.goto("/pathmanager");
    await page.waitForTimeout(2_000);

    // Expand the 2026 year node by clicking the tree toggle
    const yearToggle = page
      .locator("vaadin-grid-tree-toggle")
      .filter({ hasText: /Fahrplanjahr 2026|Timetable Year 2026/i });
    await yearToggle.first().click();
    await page.waitForTimeout(1_000);

    // The OTN or IC should appear somewhere in the grid
    const trainEntry = page.getByText(new RegExp(OTN));
    await expect(trainEntry.first()).toBeVisible({ timeout: 10_000 });

    await screenshot(page, "04-train-in-tree");
  });

  // ── 05: Verify available actions via REST ───────────────────────

  test("05. verify available actions via REST", async () => {
    const result = await apiFetch(
      page,
      "GET",
      `/api/v1/pathmanager/process/${trainId}/available-actions`,
    );

    expect(result.status).toBe(200);
    const body = result.body as Record<string, unknown>;
    expect(body.currentState).toBe("NEW");
    expect(body.actions).toContain("SEND_REQUEST");
  });

  // ── 06: Execute SEND_REQUEST transition via REST ────────────────

  test("06. execute SEND_REQUEST transition via REST", async () => {
    const result = await apiFetch(
      page,
      "POST",
      `/api/v1/pathmanager/process/${trainId}/step`,
      {
        action: "SEND_REQUEST",
        comment: "E2E test — submitting train",
      },
    );

    // PathProcessController uses @ResponseStatus(HttpStatus.CREATED)
    expect(result.status).toBe(201);
    const body = result.body as Record<string, unknown>;
    expect(body.toState).toBe("CREATED");
    expect(body.fromState).toBe("NEW");
    expect(body.stepType).toBe("SEND_REQUEST");
  });

  // ── 07: Verify Swagger UI accessible ────────────────────────────

  test("07. verify REST API list and detail endpoints", async () => {
    // Verify the trains list endpoint works
    const listResult = await apiFetch(page, "GET", "/api/v1/pathmanager/trains");
    expect(listResult.status).toBe(200);
    const trains = listResult.body as Array<Record<string, unknown>>;
    expect(trains.length).toBeGreaterThanOrEqual(1);

    // Verify our train appears in the list
    const ourTrain = trains.find(
      (t) => t.operationalTrainNumber === OTN,
    );
    expect(ourTrain).toBeTruthy();
    expect(ourTrain!.processState).toBe("CREATED");

    // Navigate back to PM view and take screenshot
    await page.goto("/pathmanager");
    await page.waitForTimeout(1_000);
    await screenshot(page, "07-api-verified");
  });

  // ── 08: Cleanup verification ────────────────────────────────────

  test("08. cleanup — verify test data is isolated", async () => {
    const result = await apiFetch(
      page,
      "GET",
      `/api/v1/pathmanager/trains/${trainId}`,
    );
    expect(result.status).toBe(200);
    const body = result.body as Record<string, unknown>;
    expect(body.processState).toBe("CREATED");

    // Verify process history exists
    const historyResult = await apiFetch(
      page,
      "GET",
      `/api/v1/pathmanager/process/${trainId}/history`,
    );
    expect(historyResult.status).toBe(200);
    const history = historyResult.body as Array<Record<string, unknown>>;
    expect(history.length).toBeGreaterThanOrEqual(1);
    expect(history[0].stepType).toBe("SEND_REQUEST");
  });
});
