import "@fontsource-variable/fraunces/opsz.css";
import "@fontsource-variable/fraunces/opsz-italic.css";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import App from "./App";
import { stopAudio } from "./lib/audio";
import { pauseAppSound, resumeAppSound } from "./lib/sound";
import "./styles/vazirmatn.css";
import "./styles/app.css";
import "./styles/persian.css";

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);

if (!window.NativeOffline && import.meta.env.PROD && "serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register(`${import.meta.env.BASE_URL}sw.js`).catch(() => null);
  });
}

// The Android wrapper notifies us on screen-off/background; visibilitychange
// covers transitions independently. Do not interrupt native offline downloads.
if (window.NativeOffline) {
  let nativePaused = false;
  const syncAudio = () => {
    if (nativePaused || document.hidden) { stopAudio(); pauseAppSound(); }
    else resumeAppSound();
  };
  window.addEventListener("wordatlas:pause", () => { nativePaused = true; syncAudio(); });
  window.addEventListener("wordatlas:resume", () => { nativePaused = false; syncAudio(); });
  document.addEventListener("visibilitychange", syncAudio);
  syncAudio();
}
