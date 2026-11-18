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

function sampleTrackFields(uid, overrides = {}) {
  return {
    userId: uid,
    trackKey: "audius_99123",
    source: "AUDIUS",
    title: "Glass Horizon",
    artist: "Kairo Synth",
    album: "Refractions",
    artworkUrl: "https://example.com/art.jpg",
    streamUrl: "https://discoveryprovider.audius.co/v1/tracks/99123/stream",
    externalUrl: "https://audius.co/kairosynth/glass-horizon",
    durationMs: 214000,
    capability: "FULL_STREAM",
    genre: "Electronic",
    ...overrides,
  };
}

test("Unauthenticated users cannot read or write any MasterWave collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("liked_tracks").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("playlists").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("liked_tracks").doc("audius_99123").set({
      ...sampleTrackFields(ALICE_UID),
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile creation, partial update, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Wave",
      email: "alice@example.com",
      preferredQuality: "HIGH",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Partial update of allowed fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredQuality: "STANDARD",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Ghost field / immutable createdAt / cross-user access rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 100),
      updatedAt: new Date(Date.now() - 100),
    })
  );
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Liked tracks enforce ownership, schema, and disallow @DocumentId id injection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 2000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("liked_tracks").doc("audius_99123").set({
      ...sampleTrackFields(ALICE_UID),
      createdAt: past,
    })
  );

  // List works even on empty collection for owner
  await assertSucceeds(
    bobDb.collection("users").doc(BOB_UID).collection("liked_tracks").get()
  );

  // Cross-user list denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("liked_tracks").get()
  );

  // Including @DocumentId "id" key fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("liked_tracks").doc("audius_99124").set({
      id: "audius_99124",
      ...sampleTrackFields(ALICE_UID, { trackKey: "audius_99124" }),
      createdAt: past,
    })
  );

  // Future timestamp rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("liked_tracks").doc("audius_99125").set({
      ...sampleTrackFields(ALICE_UID, { trackKey: "audius_99125" }),
      createdAt: new Date(Date.now() + 60000),
    })
  );
});

test("Playlists and playlist tracks enforce parent existence, ordering updates, and ownership", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  // Orphaned track without parent playlist fails
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID)
      .collection("playlists").doc("pl_missing")
      .collection("tracks").doc("entry_1").set({
        ...sampleTrackFields(ALICE_UID),
        playlistId: "pl_missing",
        position: 0,
        createdAt: created,
      })
  );

  // Create playlist
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_1").set({
      userId: ALICE_UID,
      name: "Late Night Glass",
      description: "Ambient electronic and nocturnal cuts",
      artworkUrl: "",
      trackCount: 0,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Add track to existing playlist
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID)
      .collection("playlists").doc("pl_1")
      .collection("tracks").doc("entry_1").set({
        ...sampleTrackFields(ALICE_UID),
        playlistId: "pl_1",
        position: 0,
        createdAt: created,
      })
  );

  // Reorder track position
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID)
      .collection("playlists").doc("pl_1")
      .collection("tracks").doc("entry_1").update({
        position: 1,
      })
  );

  // Rename playlist & update trackCount
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_1").update({
      name: "Midnight Optics",
      trackCount: 1,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bob cannot modify Alice's playlist or tracks
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("playlists").doc("pl_1").delete()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID)
      .collection("playlists").doc("pl_1")
      .collection("tracks").doc("entry_1").get()
  );
});

test("Saved albums, followed artists, and listening history enforce owner isolation and validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const past = new Date(Date.now() - 3000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("saved_albums").doc("itunes_555").set({
      userId: ALICE_UID,
      albumKey: "itunes_555",
      source: "ITUNES",
      title: "Midnight Prism",
      artist: "Nora Lumen",
      artworkUrl: "https://example.com/album.jpg",
      externalUrl: "https://music.apple.com/album/555",
      releaseYear: "2025",
      trackCount: 12,
      createdAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("followed_artists").doc("audius_art_1").set({
      userId: ALICE_UID,
      artistKey: "audius_art_1",
      source: "AUDIUS",
      name: "Kairo Synth",
      artworkUrl: "https://example.com/artist.jpg",
      genre: "Electronic",
      externalUrl: "https://audius.co/kairosynth",
      createdAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("listening_history").doc("audius_99123").set({
      ...sampleTrackFields(ALICE_UID),
      playedAt: past,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("listening_history").doc("audius_99123").update({
      playedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("saved_albums").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("followed_artists").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("listening_history").get()
  );
});
