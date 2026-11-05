import test from "node:test";
import assert from "node:assert/strict";
import { mediaUrl } from "./offline";

test("downloaded levels use private offline files; other levels use online media", () => {
  const bridge = { isDownloaded: (level: string) => level === "A1", hasMedia: (path: string) => path === "audio/uk/existing.mp3" };
  Object.assign(globalThis, { window: { NativeOffline: bridge } });
  assert.equal(mediaUrl("img/library.webp", "A1"), "https://appassets.androidplatform.net/offline/img/library.webp");
  assert.equal(mediaUrl("audio/uk/existing.mp3", "B1"), "https://appassets.androidplatform.net/offline/audio/uk/existing.mp3");
  assert.equal(mediaUrl("audio/uk/hut.mp3", "B1"), "https://appxa.github.io/word-atlas/atlas/audio/uk/hut.mp3");
  assert.equal(mediaUrl("audio/ex2/avenge.mp3", "C2", true), "https://0xlei.tech/words/atlas/audio/ex2/avenge.mp3");
  assert.equal(mediaUrl("audio/ex2/library.mp3", "A1", true), "https://appassets.androidplatform.net/offline/audio/ex2/library.mp3");
});
