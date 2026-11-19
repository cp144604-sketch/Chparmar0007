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
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("grains").where("catalogId", "==", "mahadev_traders").get()
  );
  await assertFails(unauthDb.collection("customer_sessions").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("my_list").get()
  );
  await assertFails(
    unauthDb.collection("grains").doc("wheat").set({
      catalogId: "mahadev_traders",
      name: "ઘઉં",
      category: "અનાજ",
      iconKey: "wheat",
      pricePerKg: 32,
      weightLabel: "50 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      createdBy: ALICE_UID,
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Bootstrapped admin email check enforces email_verified == true", async () => {
  const unverifiedAdminDb = testEnv
    .authenticatedContext("spoof_admin", {
      email: "chparmar0007@gmail.com",
      email_verified: false,
    })
    .firestore();

  const past = new Date(Date.now() - 2000);
  await assertFails(
    unverifiedAdminDb.collection("grains").doc("wheat").set({
      catalogId: "mahadev_traders",
      name: "ઘઉં",
      category: "અનાજ",
      iconKey: "wheat",
      pricePerKg: 32,
      weightLabel: "50 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      createdBy: "spoof_admin",
      createdAt: past,
      updatedAt: past,
    })
  );

  const verifiedAdminDb = testEnv
    .authenticatedContext("real_admin", {
      email: "chparmar0007@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(
    verifiedAdminDb.collection("grains").doc("wheat").set({
      catalogId: "mahadev_traders",
      name: "ઘઉં",
      category: "અનાજ",
      iconKey: "wheat",
      pricePerKg: 32,
      weightLabel: "50 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      createdBy: "real_admin",
      createdAt: past,
      updatedAt: past,
    })
  );
});

test("Admin credential verification grants grain CRUD and rejects wrong password/mobile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const past = new Date(Date.now() - 5000);

  // Wrong password fails
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      adminMobile: "9879730448",
      passcode: "wrong@123",
      displayName: "Fake Admin",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Wrong mobile fails
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      adminMobile: "9999999999",
      passcode: "mahadav@2001",
      displayName: "Fake Admin",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Valid admin credentials succeed
  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      adminMobile: "9727630403",
      passcode: "mahadav@2001",
      displayName: "Admin (0403)",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Admin can create, partial-update, and delete grains
  await assertSucceeds(
    adminDb.collection("grains").doc("rice_1").set({
      catalogId: "mahadev_traders",
      name: "ચોખા",
      category: "અનાજ",
      iconKey: "rice",
      pricePerKg: 58,
      weightLabel: "25 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      createdBy: ADMIN_UID,
      createdAt: past,
      updatedAt: past,
    })
  );

  await assertSucceeds(
    adminDb.collection("grains").doc("rice_1").update({
      pricePerKg: 62,
      updatedByMobile: "9727630403",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Shadow update with ghost field fails
  await assertFails(
    adminDb.collection("grains").doc("rice_1").update({
      pricePerKg: 65,
      isVerified: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Immutable createdAt change fails
  await assertFails(
    adminDb.collection("grains").doc("rice_1").update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Future timestamp fails
  await assertFails(
    adminDb.collection("grains").doc("rice_2").set({
      catalogId: "mahadev_traders",
      name: "બાજરી",
      category: "અનાજ",
      iconKey: "bajri",
      pricePerKg: 40,
      weightLabel: "50 Kg (બેગ)",
      stockStatus: "ઉપલબ્ધ",
      createdBy: ADMIN_UID,
      createdAt: new Date(Date.now() + 60000),
      updatedAt: new Date(Date.now() + 60000),
    })
  );

  // Non-admin customer can query filtered catalog, cannot write/delete, cannot run unfiltered query
  await assertSucceeds(
    aliceDb.collection("grains").where("catalogId", "==", "mahadev_traders").get()
  );
  await assertFails(aliceDb.collection("grains").get());
  await assertFails(
    aliceDb.collection("grains").doc("rice_1").update({
      pricePerKg: 1,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("grains").doc("rice_1").delete());

  // Admin can delete
  await assertSucceeds(adminDb.collection("grains").doc("rice_1").delete());
});

test("Customer sessions enforce PII isolation and allow admin audit list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const past = new Date(Date.now() - 4000);

  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      adminMobile: "9879730448",
      passcode: "mahadav@2001",
      displayName: "Admin (0448)",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Alice logs in with her mobile number
  await assertSucceeds(
    aliceDb.collection("customer_sessions").doc(`${ALICE_UID}_9876543210`).set({
      userId: ALICE_UID,
      mobileNumber: "9876543210",
      customerName: "રમેશભાઈ",
      email: "ramesh@example.com",
      roleLabel: "customer",
      loginCount: 1,
      createdAt: past,
      lastLoginAt: past,
    })
  );

  // Alice updates her own login count
  await assertSucceeds(
    aliceDb.collection("customer_sessions").doc(`${ALICE_UID}_9876543210`).update({
      loginCount: 2,
      lastLoginAt: new Date(Date.now() - 1000),
    })
  );

  // Alice can query her own sessions
  await assertSucceeds(
    aliceDb.collection("customer_sessions").where("userId", "==", ALICE_UID).get()
  );

  // Bob CANNOT read Alice's PII or list all customer_sessions
  await assertFails(
    bobDb.collection("customer_sessions").doc(`${ALICE_UID}_9876543210`).get()
  );
  await assertFails(bobDb.collection("customer_sessions").get());

  // Admin CAN list all customer_sessions
  await assertSucceeds(adminDb.collection("customer_sessions").get());
});

test("User my_list subcollection is strictly owner-isolated", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("my_list")
      .doc("wheat")
      .set({
        grainId: "wheat",
        grainName: "ઘઉં",
        pricePerKg: 32,
        weightLabel: "50 Kg (બેગ)",
        quantityNote: "2 બેગ",
        createdAt: past,
        updatedAt: past,
      })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("my_list").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("my_list").get()
  );
});

test("App download config is readable by signed-in users and writable only by admin", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  const past = new Date(Date.now() - 3000);

  await assertSucceeds(
    adminDb.collection("admins").doc(ADMIN_UID).set({
      userId: ADMIN_UID,
      adminMobile: "9879730448",
      passcode: "mahadav@2001",
      displayName: "Admin (0448)",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Non-admin cannot write download_config
  await assertFails(
    aliceDb.collection("app_settings").doc("download_config").set({
      downloadUrl: "https://example.com/mahadev.apk",
      titleGu: "મહાદેવ ટ્રેડર્સ એપ ડાઉનલોડ",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Admin can create and update valid URL
  await assertSucceeds(
    adminDb.collection("app_settings").doc("download_config").set({
      downloadUrl: "https://example.com/mahadev.apk",
      titleGu: "મહાદેવ ટ્રેડર્સ એપ ડાઉનલોડ",
      updatedByMobile: "9879730448",
      createdAt: past,
      updatedAt: past,
    })
  );

  // Invalid non-http URL fails
  await assertFails(
    adminDb.collection("app_settings").doc("download_config").update({
      downloadUrl: "javascript:alert(1)",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Signed-in customer can read download_config
  await assertSucceeds(
    aliceDb.collection("app_settings").doc("download_config").get()
  );
});
