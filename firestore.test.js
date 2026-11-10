const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

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

// --- VEHICLES TESTS ---

test("Unauthenticated user: cannot read or write vehicles", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("vehicles").get());
  await assertFails(unauthDb.collection("vehicles").doc("veh_1").set({
    userId: ALICE_UID,
    familyMember: "My Car",
    make: "Toyota",
    model: "Camry",
    year: 2022,
    vin: "4T1B11HK5JU123456",
    licensePlate: "7XYZ123",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Authenticated user: can create valid vehicle for self", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("vehicles").doc("veh_alice_1").set({
    userId: ALICE_UID,
    familyMember: "My Car",
    nickname: "Daily Commuter",
    make: "Toyota",
    model: "Camry",
    year: 2022,
    vin: "4T1B11HK5JU123456",
    licensePlate: "7XYZ123",
    state: "CA",
    currentMileage: 32000,
    insuranceCompany: "State Farm",
    insurancePolicyNumber: "SF-987654",
    insuranceDuration: "6 Months",
    insuranceStartDate: "2024-01-01",
    insuranceEndDate: "2024-07-01",
    colorHex: "#1E40AF",
    notes: "Regular synthetic oil",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Cross-user isolation: Bob cannot read Alice's vehicle", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("vehicles").doc("alice_car").set({
      userId: ALICE_UID,
      familyMember: "Wife's SUV",
      make: "Honda",
      model: "Pilot",
      year: 2021,
      vin: "5FNYF6H59MB000000",
      licensePlate: "8ABC999",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("vehicles").doc("alice_car").get());
});

test("Query enforcer: user must query vehicles with whereEqualTo userId", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("vehicles").doc("alice_car").set({
      userId: ALICE_UID,
      familyMember: "Kid's Sedan",
      make: "Subaru",
      model: "Impreza",
      year: 2019,
      vin: "4S3GTAC68K3111111",
      licensePlate: "6KID777",
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("vehicles").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("vehicles").get());
});

// --- MAINTENANCE LOGS TESTS ---

test("Authenticated user: can create maintenance log for own vehicle", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("maintenance_logs").doc("log_1").set({
    userId: ALICE_UID,
    vehicleId: "veh_alice_1",
    title: "Front Brake Pads & Rotors",
    logType: "MAINTENANCE",
    serviceDate: "2024-09-15",
    mileage: 35000,
    cost: 340.50,
    serviceProvider: "Brembo Authorized Shop",
    notes: "Ceramic pads installed",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));
});

test("Cross-user isolation: Bob cannot read Alice's maintenance logs", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("maintenance_logs").doc("alice_log").set({
      userId: ALICE_UID,
      vehicleId: "veh_alice_1",
      title: "Oil Change",
      logType: "MAINTENANCE",
      serviceDate: "2024-08-01",
      mileage: 30000,
      createdAt: new Date(),
      updatedAt: new Date(),
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("maintenance_logs").doc("alice_log").get());
});

// --- SERVICE REMINDERS TESTS ---

test("Authenticated user: can create and update service reminder", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("service_reminders").doc("rem_1").set({
    userId: ALICE_UID,
    vehicleId: "veh_alice_1",
    serviceType: "Tire Rotation",
    dueDate: "2025-01-15",
    dueMileage: 40000,
    isCompleted: false,
    priority: "HIGH",
    notes: "Check tread depth",
    createdAt: new Date(),
    updatedAt: new Date(),
  }));

  // Update reminder
  await assertSucceeds(aliceDb.collection("service_reminders").doc("rem_1").update({
    isCompleted: true,
    updatedAt: new Date(),
  }));
});
