import { expect, Page, test } from "@playwright/test";

// ── Credentials ────────────────────────────────────────────────────
const KC_USER = "sebastian";
const KC_PASS = "sebastian";

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

// ── Path Manager REST API Tests ─────────────────────────────────────
// UI navigation and tree verification are covered by sbahn-integration.spec.ts.
// These tests focus on the REST API endpoints (unique coverage).

test.describe("Path Manager: REST API", () => {
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

  // ── 01: Submit a train via REST API ─────────────────────────────

  test("01. submit a train via REST API", async () => {
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

  // ── 02: Verify available actions via REST ───────────────────────

  test("02. verify available actions via REST", async () => {
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

  // ── 03: Execute SEND_REQUEST transition via REST ────────────────

  test("03. execute SEND_REQUEST transition via REST", async () => {
    const result = await apiFetch(
      page,
      "POST",
      `/api/v1/pathmanager/process/${trainId}/step`,
      {
        action: "SEND_REQUEST",
        comment: "E2E test — submitting train",
      },
    );

    expect(result.status).toBe(201);
    const body = result.body as Record<string, unknown>;
    expect(body.toState).toBe("CREATED");
    expect(body.fromState).toBe("NEW");
    expect(body.stepType).toBe("SEND_REQUEST");
  });

  // ── 04: Verify REST list and detail endpoints ──────────────────

  test("04. verify REST API list and detail endpoints", async () => {
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

    // Verify detail endpoint
    const detailResult = await apiFetch(
      page,
      "GET",
      `/api/v1/pathmanager/trains/${trainId}`,
    );
    expect(detailResult.status).toBe(200);
    const detail = detailResult.body as Record<string, unknown>;
    expect(detail.processState).toBe("CREATED");

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

    await screenshot(page, "04-api-verified");
  });
});
