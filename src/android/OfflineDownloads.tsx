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
    update(); const timer = setInterval(update, 500); return () => clearInterval(timer);
  }, []);
  const busy = states.some((s) => ["downloading", "checking", "cancelling", "deleting"].includes(s.state));
  return <section className="panel offline-levels" aria-label="دانلود آفلاین">
    <h3 className="eyebrow">دانلود آفلاین</h3>
    <p className="muted">تصویر و صدای هر سطح را برای استفادهٔ آفلاین دانلود کن.</p>
    <p className="muted">برای دانلود، برنامه را باز نگه دار. فایل‌های سالم پس از لغو حفظ می‌شوند و می‌توانی دانلود را ادامه دهی.</p>
    {states.map((s) => {
      const active = s.state === "downloading" || s.state === "checking";
      const stopping = s.state === "cancelling" || s.state === "deleting";
      const percent = s.total ? Math.min(100, Math.round(s.downloaded * 100 / s.total)) : 0;
      return <div className="offline-row" key={s.level}>
        <div className="offline-head">
          <strong dir="ltr">{s.level}</strong>
          <span className="offline-size">{fmt(s.total)}</span>
          {s.state === "complete" ? <button className="btn btn-ghost btn-danger" aria-label={`حذف فایل‌های ${s.level}`} disabled={busy} onClick={() => { if (confirm(`فایل‌های آفلاین ${s.level} حذف شوند؟`)) window.NativeOffline?.remove(s.level); }}>حذف</button>
          : active ? <button className="btn btn-ghost" aria-label={`لغو دانلود ${s.level}`} onClick={() => window.NativeOffline?.cancel(s.level)}>لغو</button>
          : <button className="btn btn-ghost" disabled={busy || stopping} aria-label={`دانلود ${s.level}`} onClick={() => window.NativeOffline?.start(s.level)}>{s.state === "cancelling" ? "در حال لغو…" : s.state === "deleting" ? "در حال حذف…" : s.downloaded ? "ادامه" : "دانلود"}</button>}
        </div>
        {(active || s.state === "cancelling") && <div className="offline-progress"><progress max={100} value={percent} aria-label={`پیشرفت دانلود ${s.level}`} /><span>{s.state === "checking" ? "در حال بررسی…" : `${percent.toLocaleString("fa-IR")}٪`}</span></div>}
        {s.state === "error" && <p className="warn-text">{s.error || "دانلود ناموفق بود؛ دوباره تلاش کن."}</p>}
      </div>;
    })}
  </section>;
}
