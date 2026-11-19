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

test("Unauthenticated user: denied across private and public collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("public_profiles").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("study_groups").get());
  await assertFails(unauthDb.collection("challenges").get());
  await assertFails(unauthDb.collection("announcements").get());
});

test("Profile PII isolation & role escalation guards", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: "bob@example.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 2000);

  // Alice creates her private user doc (role USER, planTier FREE)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      username: "alice_study",
      displayName: "Alice",
      country: "USA",
      dailyGoalMinutes: 120,
      themePreference: "SYSTEM",
      planTier: "FREE",
      role: "USER",
      notificationsMuted: false,
      onboardingCompleted: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Alice can switch themePreference to GRADIENT, but invalid theme names fail
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themePreference: "GRADIENT",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themePreference: "NEON",
      updatedAt: new Date(Date.now() - 900),
    })
  );

  // Alice can switch fontPreference to any supported font, invalid font fails
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fontPreference: "GENERAL_SANS",
      updatedAt: new Date(Date.now() - 800),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      fontPreference: "COMIC_SANS",
      updatedAt: new Date(Date.now() - 700),
    })
  );

  // Alice cannot self-escalate to ADMIN or PREMIUM
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "ADMIN",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      planTier: "PREMIUM",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob cannot read Alice's private PII doc
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());

  // Alice creates her public profile (no PII)
  await assertSucceeds(
    aliceDb.collection("public_profiles").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_study",
      displayName: "Alice",
      bio: "CS Major",
      country: "USA",
      avatarEmoji: "🦉",
      avatarColorHex: "#6C63FF",
      planTier: "FREE",
      totalXp: 0,
      level: 1,
      totalStudyMinutes: 0,
      currentStreakDays: 0,
      longestStreakDays: 0,
      dailyGoalMinutes: 120,
      onlineStatus: "ONLINE",
      hideFromLeaderboard: false,
      publicProfile: true,
      suspended: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Ghost field rejected
  await assertFails(
    aliceDb.collection("public_profiles").doc(ALICE_UID).update({
      isVerified: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob can read public_profiles
  await assertSucceeds(bobDb.collection("public_profiles").doc(ALICE_UID).get());
});

test("Subjects, Tasks, Notes & Verified Study Sessions under user subcollections", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("subjects").doc("sub_math").set({
      userId: ALICE_UID,
      name: "Linear Algebra",
      iconKey: "calculate",
      colorHex: "#6C63FF",
      description: "Eigenvalues and vector spaces",
      targetMinutes: 600,
      completedMinutes: 0,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cross-user subcollection read/write denied
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("subjects").get());

  // Valid verified 25m session succeeds
  const endMs = Date.now();
  const startMs = endMs - 25 * 60 * 1000;
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_1").set({
      userId: ALICE_UID,
      subjectId: "sub_math",
      subjectName: "Linear Algebra",
      durationMinutes: 25,
      mode: "POMODORO_25",
      notes: "Completed chapter 3 exercises",
      xpEarned: 125,
      verifiedStartMillis: startMs,
      verifiedEndMillis: endMs,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Cheated session (claims 60m in 1 second) fails security rule
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("sessions").doc("sess_cheat").set({
      userId: ALICE_UID,
      subjectId: "sub_math",
      subjectName: "Linear Algebra",
      durationMinutes: 60,
      mode: "CUSTOM",
      notes: "Fake session",
      xpEarned: 300,
      verifiedStartMillis: endMs - 1000,
      verifiedEndMillis: endMs,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Task & Note CRUD
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("tasks").doc("task_1").set({
      userId: ALICE_UID,
      subjectId: "sub_math",
      subjectName: "Linear Algebra",
      title: "Midterm Practice",
      description: "Solve past paper",
      priority: "HIGH",
      dueDateMillis: Date.now() + 86400000,
      reminderEnabled: true,
      recurring: "NONE",
      examFlag: true,
      completed: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("notes").doc("note_1").set({
      userId: ALICE_UID,
      subjectId: "sub_math",
      subjectName: "Linear Algebra",
      title: "Spectral Theorem",
      content: "Symmetric matrices have orthogonal eigenvectors.",
      pinned: true,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Private direct chats & friend requests enforce participant isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext("charlie_999").firestore();
  const now = new Date(Date.now() - 5000);

  // Friend request
  await assertSucceeds(
    aliceDb.collection("friend_requests").doc("req_1").set({
      fromUid: ALICE_UID,
      fromName: "Alice",
      toUid: BOB_UID,
      toName: "Bob",
      status: "PENDING",
      participantUids: [ALICE_UID, BOB_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob accepts
  await assertSucceeds(
    bobDb.collection("friend_requests").doc("req_1").update({
      status: "ACCEPTED",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Charlie cannot read Alice-Bob friend request
  await assertFails(charlieDb.collection("friend_requests").doc("req_1").get());

  // Direct chat between Alice and Bob
  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_ab").set({
      participantUids: [ALICE_UID, BOB_UID],
      participantLabels: ["Alice", "Bob"],
      lastMessage: "Hey, ready to study?",
      lastSenderId: ALICE_UID,
      mutedByUids: [],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_1").set({
      chatId: "chat_ab",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Hey, ready to study?",
      memberUids: [ALICE_UID, BOB_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered queries succeed; Charlie cannot read Alice-Bob chat or messages
  await assertSucceeds(
    bobDb.collection("chats").where("participantUids", "array-contains", BOB_UID).get()
  );
  await assertSucceeds(
    bobDb.collection("chats").doc("chat_ab").collection("messages").where("memberUids", "array-contains", BOB_UID).get()
  );
  await assertFails(charlieDb.collection("chats").doc("chat_ab").get());
  await assertFails(charlieDb.collection("chats").doc("chat_ab").collection("messages").doc("msg_1").get());
});

test("Study groups, challenges, and admin-only announcements + email spoofing protection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: "alice@example.com", email_verified: true }).firestore();
  const spoofAdminDb = testEnv.authenticatedContext("spoof_1", { email: "muhinpetrol@gmail.com", email_verified: false }).firestore();
  const realAdminDb = testEnv.authenticatedContext(ADMIN_UID, { email: "muhinpetrol@gmail.com", email_verified: true }).firestore();
  const now = new Date(Date.now() - 5000);

  // Email spoofing test: unverified admin email MUST fail
  await assertFails(
    spoofAdminDb.collection("announcements").doc("ann_1").set({
      title: "Spoof",
      message: "Fake announcement",
      authorId: "spoof_1",
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Verified admin succeeds
  await assertSucceeds(
    realAdminDb.collection("announcements").doc("ann_1").set({
      title: "Welcome to Study Time",
      message: "Join the weekend Pomodoro sprint!",
      authorId: ADMIN_UID,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Normal user cannot write announcements
  await assertFails(
    aliceDb.collection("announcements").doc("ann_2").set({
      title: "User attempt",
      message: "Should fail",
      authorId: ALICE_UID,
      active: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Study group creation & messaging
  await assertSucceeds(
    aliceDb.collection("study_groups").doc("grp_cs").set({
      name: "Algorithms Squad",
      description: "Daily DP & Graphs practice",
      ownerId: ALICE_UID,
      goalMinutes: 1200,
      totalGroupMinutes: 0,
      memberCount: 1,
      publicGroup: true,
      memberUids: [ALICE_UID],
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("study_groups").doc("grp_cs").collection("messages").doc("m1").set({
      groupId: "grp_cs",
      senderId: ALICE_UID,
      senderName: "Alice",
      text: "Starting a 50m deep work block!",
      createdAt: now,
      updatedAt: now,
    })
  );
});
