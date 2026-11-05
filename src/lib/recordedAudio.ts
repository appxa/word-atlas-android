/** Observe a recording from before play() starts, including short clips and cancellation. */
export function playRecording(audio: HTMLAudioElement, cancelled: () => boolean): Promise<"recorded" | "stopped" | "failed"> {
  return new Promise((resolve) => {
    let settled = false;
    const finish = (result: "recorded" | "stopped" | "failed") => {
      if (settled) return;
      settled = true;
      audio.removeEventListener("ended", onEnded);
      audio.removeEventListener("pause", onPause);
      audio.removeEventListener("error", onError);
      resolve(cancelled() ? "stopped" : result);
    };
    const onEnded = () => finish("recorded");
    // A normal end may emit pause before ended. Pausing is never a loading error.
    const onPause = () => finish(audio.ended ? "recorded" : "stopped");
    const onError = () => finish("failed");
    audio.addEventListener("ended", onEnded);
    audio.addEventListener("pause", onPause);
    audio.addEventListener("error", onError);
    try {
      Promise.resolve(audio.play()).catch(() => finish("failed"));
    } catch {
      finish("failed");
    }
  });
}
