import test from "node:test";
import assert from "node:assert/strict";
import { mediaUrl } from "./offline";

test("downloaded levels use private offline files; other levels use online media", () => {
  const bridge = { isDownloaded: (level: string) => level === "A1" };
  Object.assign(globalThis, { window: { NativeOffline: bridge } });
  assert.equal(mediaUrl("img/library.webp", "A1"), "https://appassets.androidplatform.net/offline/img/library.webp");
  assert.equal(mediaUrl("audio/uk/hut.mp3", "B1"), "https://appxa.github.io/word-atlas/atlas/audio/uk/hut.mp3");
});
