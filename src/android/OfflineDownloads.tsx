import { useEffect, useState } from "react";
import type { LevelState } from "./offline";
const LEVELS = ["A1", "A2", "B1", "B2", "C1", "C2"];
const fmt = (n: number) => `${(n / 1048576).toLocaleString("fa-IR", { maximumFractionDigits: 1 })} مگابایت`;
export function OfflineDownloads() {
  const [states, setStates] = useState<LevelState[]>([]);
  useEffect(() => {
    const update = () => setStates(LEVELS.map((level) => {
      try { return JSON.parse(window.NativeOffline!.status(level)) as LevelState; }
      catch { return { level, total: 0, downloaded: 0, state: "error", error: "دریافت وضعیت ناموفق بود." }; }
    }));
    update(); const timer = setInterval(update, 1000); return () => clearInterval(timer);
  }, []);
  const busy = states.some((s) => s.state === "downloading" || s.state === "checking");
  return <section className="panel offline-levels" aria-label="دانلود آفلاین">
    <h3 className="eyebrow">دانلود آفلاین</h3>
    <p className="muted">تصویر و صداهای هر سطح را دانلود کن. سطح‌های دانلودنشده با اینترنت نمایش داده می‌شوند.</p>
    <p className="muted">دانلود با اینترنت موبایل هم انجام می‌شود. برای ادامهٔ دانلود، برنامه را باز نگه دار.</p>
    {states.map((s) => {
      const active = s.state === "downloading" || s.state === "checking";
      const percent = s.total ? Math.min(100, Math.round(s.downloaded * 100 / s.total)) : 0;
      return <div className="offline-row" key={s.level}>
        <div className="offline-head"><strong dir="ltr">{s.level}</strong><span>{s.state === "complete" ? "آمادهٔ آفلاین" : `${fmt(s.total)} · ${s.state === "checking" ? "در حال بررسی…" : `${fmt(s.downloaded)} دانلود شده`}`}</span></div>
        {(active || (s.downloaded > 0 && s.state !== "complete")) && <progress max={100} value={percent} aria-label={`پیشرفت دانلود ${s.level}`} />}
        {s.state === "error" && <p className="warn-text">{s.error || "دانلود ناموفق بود؛ دوباره تلاش کن."}</p>}
        <div className="offline-controls">
          {s.state === "complete" ? <button className="btn btn-ghost btn-danger" onClick={() => { if (confirm(`فایل‌های آفلاین ${s.level} حذف شوند؟`)) window.NativeOffline?.remove(s.level); }}>حذف فایل‌ها</button>
          : <button className="btn btn-ghost" disabled={busy} onClick={() => window.NativeOffline?.start(s.level)}>{active ? `${percent.toLocaleString("fa-IR")}٪` : s.downloaded ? "ادامهٔ دانلود" : "دانلود"}</button>}
        </div>
      </div>;
    })}
  </section>;
}
