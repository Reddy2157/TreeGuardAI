const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_arborist_123";
const BOB_UID = "bob_inspector_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validTreePayload(userId = ALICE_UID, now = new Date()) {
  return {
    userId,
    treeCode: "TG-VJA-0001",
    species: "Azadirachta indica (Neem)",
    latitude: 16.5062,
    longitude: 80.6480,
    zone: "MG Road, Benz Circle, Vijayawada",
    registrationType: "LIVE_GPS",
    visualCondition: "Monitor",
    currentPriority: "High",
    currentRiskScore: 62.5,
    structuralScore: 60.0,
    exposureScore: 80.0,
    declineScore: 55.0,
    weatherScore: 50.0,
    historyScore: 50.0,
    stressDebt: 48.2,
    previousStressDebt: 32.0,
    heatContribution: 68.0,
    drynessContribution: 54.0,
    observationContribution: 55.0,
    notes: "Canopy thinning on eastern crown near pedestrian walkway.",
    createdAt: now,
    updatedAt: now,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("1. Unauthenticated user cannot read or write trees", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("trees").doc("tree_1").get());
  await assertFails(unauthDb.collection("trees").doc("tree_1").set(validTreePayload()));
});

test("2. Authenticated user can create and read their own valid tree", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("trees").doc("tree_1").set(validTreePayload(ALICE_UID)));
  await assertSucceeds(aliceDb.collection("trees").doc("tree_1").get());
});

test("3. Authenticated user cannot read another user's tree (IDOR guard)", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("trees").doc("bob_tree").set(validTreePayload(BOB_UID));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(aliceDb.collection("trees").doc("bob_tree").get());
});

test("4. Client query alignment: list succeeds with userId filter and fails without filter", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("trees").doc("alice_tree").set(validTreePayload(ALICE_UID));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("trees").where("userId", "==", ALICE_UID).get());
  await assertFails(aliceDb.collection("trees").get());
});

test("5. Shadow field injection and identity spoofing are rejected", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("trees").doc("spoof_tree").set({
      ...validTreePayload(BOB_UID),
    })
  );
  await assertFails(
    aliceDb.collection("trees").doc("ghost_tree").set({
      ...validTreePayload(ALICE_UID),
      isVerifiedAdmin: true,
    })
  );
});

test("6. Immutable fields (userId, treeCode, createdAt) cannot be modified on update", async () => {
  const createdTime = new Date(Date.now() - 10000);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("trees").doc("tree_1").set(validTreePayload(ALICE_UID, createdTime));
  });
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("trees").doc("tree_1").update({
      visualCondition: "Needs inspection",
      currentPriority: "Critical",
      currentRiskScore: 81.0,
      updatedAt: new Date(),
    })
  );
  await assertFails(
    aliceDb.collection("trees").doc("tree_1").update({
      treeCode: "TG-HACKED-9999",
      updatedAt: new Date(),
    })
  );
});
