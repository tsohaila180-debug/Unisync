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

test("Unauthenticated user cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("course_materials").get());
  await assertFails(unauthDb.collection("forum_posts").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("inbox").doc("e1").set({
      userId: ALICE_UID,
      senderName: "Dr. Ahmed",
      senderRole: "INSTRUCTOR",
      courseCode: "TM105",
      subject: "Quiz Notice",
      body: "Tomorrow at 10 AM",
      priority: "CRITICAL",
      answered: false,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("User profile creation, partial update, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Sara Al-Harbi",
      role: "STUDENT",
      aouBranch: "Saudi Arabia",
      majorId: "cs",
      preferredLanguage: "en",
      darkMode: false,
      pomodoroCharacter: "GIRL",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update allowed fields
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "ar",
      darkMode: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field / shadow update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject immutable createdAt update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user read & write rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Inbox create, AI summary update, reply update, and cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("inbox").doc("mail_1").set({
      userId: ALICE_UID,
      senderName: "Dr. Tariq",
      senderRole: "INSTRUCTOR",
      courseCode: "M251",
      subject: "TMA Deadline Extension",
      body: "TMA submission moved to Friday 23:59.",
      priority: "HIGH",
      answered: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("inbox").doc("mail_1").update({
      aiSummary: "TMA deadline extended to Friday 23:59.",
      extractedDeadline: "Friday 23:59",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("inbox").doc("mail_1").update({
      answered: true,
      replyText: "Thank you Dr. Tariq!",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject id injection
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("inbox").doc("mail_2").set({
      id: "mail_2",
      userId: ALICE_UID,
      senderName: "Test",
      senderRole: "ADMIN",
      courseCode: "GR101",
      subject: "Bad",
      body: "Body",
      priority: "LOW",
      answered: false,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("inbox").get());
});

test("Calendar events, personal materials, and direct chats owner-scoped", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("calendar_events").doc("ev_1").set({
      userId: ALICE_UID,
      title: "TM111 Midterm",
      courseCode: "TM111",
      eventType: "MIDTERM",
      triggerEpochMillis: Date.now() + 86400000,
      alarmEnabled: true,
      ringtoneId: "crystal_chime",
      volumePercent: 85,
      notes: "Hall 3B",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("personal_materials").doc("mat_1").set({
      userId: ALICE_UID,
      title: "My Chapter 2 Notes",
      noteContent: "OOP encapsulation notes",
      fileUri: "",
      fileName: "",
      materialType: "NOTE",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("direct_chats").doc("msg_1").set({
      userId: ALICE_UID,
      peerName: "Dr. Layla",
      peerRole: "INSTRUCTOR",
      senderIsSelf: true,
      messageText: "Could you clarify question 3?",
      imageUri: "",
      courseCode: "M269",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("calendar_events").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("personal_materials").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("direct_chats").get());
});

test("Course materials and forum posts allow signed-in reads and enforce author writes", async () => {
  const instructorDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const studentDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    instructorDb.collection("course_materials").doc("cm_1").set({
      authorUid: ALICE_UID,
      authorName: "Dr. Nadia",
      courseCode: "TM351",
      title: "Week 5 Data Management Slides",
      description: "Normalization and indexing overview",
      linkUrl: "https://lms.aou.edu.kw/tm351-w5",
      fileName: "TM351_Week5.pdf",
      materialKind: "LECTURE_SLIDES",
      scope: "AOU",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Student can read & list with query filter matching rules; unfiltered query fails
  await assertSucceeds(studentDb.collection("course_materials").where("scope", "==", "AOU").get());
  await assertFails(studentDb.collection("course_materials").get());
  // Student cannot delete or overwrite instructor material
  await assertFails(studentDb.collection("course_materials").doc("cm_1").delete());

  // Forum post create & reply
  await assertSucceeds(
    studentDb.collection("forum_posts").doc("fp_1").set({
      authorUid: BOB_UID,
      authorName: "Omar",
      category: "Registration",
      questionTitle: "When does EL112 placement open?",
      questionBody: "Looking for Fall semester dates.",
      repliesCount: 0,
      latestReply: "",
      scope: "AOU",
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(studentDb.collection("forum_posts").where("scope", "==", "AOU").get());

  await assertSucceeds(
    instructorDb.collection("forum_posts").doc("fp_1").update({
      repliesCount: 1,
      latestReply: "Placement opens next Sunday via SIS.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Non-author cannot edit questionTitle
  await assertFails(
    instructorDb.collection("forum_posts").doc("fp_1").update({
      questionTitle: "Tampered",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("General Student Community Chat enforces AOU scope, authorUid, and delete ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const created = new Date(Date.now() - 5000);

  await assertFails(unauthDb.collection("general_student_chat").where("scope", "==", "AOU").get());

  await assertSucceeds(
    aliceDb.collection("general_student_chat").doc("gc_1").set({
      authorUid: ALICE_UID,
      authorName: "Sara K.",
      authorBranch: "Kuwait",
      courseTag: "M251",
      messageText: "Anyone revising Chapter 5 OOP polymorphism tonight?",
      scope: "AOU",
      createdAt: created,
      updatedAt: created,
    })
  );

  // Unfiltered query fails; scoped query succeeds
  await assertFails(bobDb.collection("general_student_chat").get());
  await assertSucceeds(bobDb.collection("general_student_chat").where("scope", "==", "AOU").get());

  // Bob cannot spoof Alice's UID or delete Alice's message
  await assertFails(
    bobDb.collection("general_student_chat").doc("gc_2").set({
      authorUid: ALICE_UID,
      authorName: "Spoof",
      authorBranch: "Kuwait",
      courseTag: "M251",
      messageText: "Spoofed message",
      scope: "AOU",
      createdAt: created,
      updatedAt: created,
    })
  );
  await assertFails(bobDb.collection("general_student_chat").doc("gc_1").delete());
  await assertSucceeds(aliceDb.collection("general_student_chat").doc("gc_1").delete());
});
