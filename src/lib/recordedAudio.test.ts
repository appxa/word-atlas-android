import test from "node:test";
import assert from "node:assert/strict";
import { playRecording } from "./recordedAudio";

class FakeAudio extends EventTarget {
  ended = false;
  shouldReject = false;
  play() { return this.shouldReject ? Promise.reject(new Error("unavailable")) : Promise.resolve(); }
  asAudio() { return this as unknown as HTMLAudioElement; }
}

test("natural end is successful when pause arrives before ended", async () => {
  const audio = new FakeAudio();
  const result = playRecording(audio.asAudio(), () => false);
  audio.ended = true;
  audio.dispatchEvent(new Event("pause"));
  audio.dispatchEvent(new Event("ended"));
  assert.equal(await result, "recorded");
});

test("ended event on its own is successful", async () => {
  const audio = new FakeAudio();
  const result = playRecording(audio.asAudio(), () => false);
  audio.dispatchEvent(new Event("ended"));
  assert.equal(await result, "recorded");
});

test("pause or cancellation does not pretend the recording failed", async () => {
  const audio = new FakeAudio();
  const result = playRecording(audio.asAudio(), () => false);
  audio.dispatchEvent(new Event("pause"));
  assert.equal(await result, "stopped");
  const second = new FakeAudio();
  let cancelled = false;
  const pending = playRecording(second.asAudio(), () => cancelled);
  cancelled = true;
  second.dispatchEvent(new Event("error"));
  assert.equal(await pending, "stopped");
});

test("actual media error or rejected play triggers fallback", async () => {
  const audio = new FakeAudio();
  const result = playRecording(audio.asAudio(), () => false);
  audio.dispatchEvent(new Event("error"));
  assert.equal(await result, "failed");
  const rejected = new FakeAudio(); rejected.shouldReject = true;
  assert.equal(await playRecording(rejected.asAudio(), () => false), "failed");
});
