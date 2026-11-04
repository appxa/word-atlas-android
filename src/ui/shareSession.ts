import type { Atlas } from "../atlas/types";
import { sessionStats } from "../store/session";
import { Rating } from "../store/srs";
import type { ActiveSession } from "../store/store";
import { toast } from "./Toast";
import { formatPersianDate } from "../lib/date";

const SERIF = '"Fraunces Variable", "Songti SC", "Noto Serif SC", Georgia, serif';
const SANS = '"Vazirmatn Variable", Tahoma, sans-serif';

const load = (src: string) =>
  new Promise<HTMLImageElement | null>((r) => {
    const i = new Image();
    i.onload = () => r(i);
    i.onerror = () => r(null);
    i.src = src;
  });

/** A 4:5 "today I learned" card: a mosaic of the session's illustrations plus first-try accuracy. */
export async function shareSession(atlas: Atlas, s: ActiveSession) {
  try {
    const W = 1080;
    const H = 1350;
    const c = document.createElement("canvas");
    c.width = W;
    c.height = H;
    const ctx = c.getContext("2d")!;
    ctx.direction = "ltr";
    ctx.textAlign = "left";
    await Promise.all([document.fonts.load(`600 96px ${SERIF}`), document.fonts.load(`600 64px ${SANS}`, "فارسی")]).catch(() => null);
    ctx.fillStyle = "#F3ECDF";
    ctx.fillRect(0, 0, W, H);
    const ids = [...new Set(s.steps.map((x) => x.id))].slice(0, 9);
    const refs = ids.map((id) => atlas.byId.get(id)!).filter(Boolean);
    const pad = 64;
    const gapPx = 16;
    const cell = (W - pad * 2 - gapPx * 2) / 3;
    const imgs = await Promise.all(refs.map((r) => (atlas.img(r, "thumb") ? load(atlas.img(r, "thumb")!) : Promise.resolve(null))));
    refs.forEach((r, i) => {
      const x = pad + (i % 3) * (cell + gapPx);
      const y = pad + Math.floor(i / 3) * (cell + gapPx);
      ctx.save();
      ctx.beginPath();
      ctx.roundRect(x, y, cell, cell, 24);
      ctx.clip();
      ctx.fillStyle = "#EAE1D0";
      ctx.fillRect(x, y, cell, cell);
      const img = imgs[i];
      if (img) ctx.drawImage(img, x, y, cell, cell);
      ctx.fillStyle = "rgba(20,24,40,.0)";
      ctx.restore();
      ctx.font = `600 30px ${SERIF}`;
      ctx.fillStyle = "#FBF8F2";
      ctx.save();
      ctx.shadowColor = "rgba(0,0,0,.35)";
      ctx.shadowBlur = 12;
      ctx.fillText(r.word, x + 20, y + cell - 22, cell - 40);
      ctx.restore();
    });
    const st = sessionStats(s);
    const top = pad + 3 * cell + 2 * gapPx + 96;
    const d = new Date();
    ctx.save();
    ctx.direction = "rtl";
    ctx.textAlign = "right";
    ctx.fillStyle = "#8A8C96";
    ctx.font = `500 28px ${SANS}`;
    ctx.fillText(`${formatPersianDate(d)} · امروز ${ids.length} ستاره روشن کردی`, W - pad, top - 24);
    ctx.fillStyle = "#1E2640";
    ctx.font = `600 54px ${SANS}`;
    ctx.fillText(`${ids.length} واژه آموختی · دقت: ${st.accuracy.toLocaleString("fa-IR")}٪`, W - pad, top + 52, W - pad * 2);
    const wrong = Object.values(s.graded).filter((g) => g === Rating.Again).length;
    ctx.font = `400 30px ${SANS}`;
    ctx.fillStyle = "#5A6075";
    ctx.fillText(`واژهٔ جدید ${s.newIds.length} · مرور ${s.reviewIds.length} · پاسخ نادرست ${wrong}`, W - pad, top + 112);
    ctx.restore();
    ctx.fillStyle = "rgba(30,38,64,.14)";
    ctx.fillRect(pad, H - 116, W - pad * 2, 2);
    ctx.fillStyle = "#1E2640";
    ctx.font = `600 34px ${SERIF}`;
    ctx.fillText("Word Atlas", pad, H - 58);
    ctx.font = `400 26px ${SANS}`;
    ctx.fillStyle = "#8A8C96";
    const tag = "یک واژه، یک تصویر؛ آیلتس را روشن کن";
    ctx.direction = "rtl";
    ctx.textAlign = "right";
    ctx.fillText(tag, W - pad, H - 60);
    const blob = await new Promise<Blob | null>((r) => c.toBlob(r, "image/png"));
    if (!blob) throw new Error();
    const file = new File([blob], `word-atlas-${d.toISOString().slice(0, 10)}.png`, { type: "image/png" });
    if (navigator.canShare?.({ files: [file] })) {
      await navigator.share({ files: [file] }).catch(() => null);
      return;
    }
    const a = document.createElement("a");
    a.href = URL.createObjectURL(blob);
    a.download = file.name;
    a.click();
    setTimeout(() => URL.revokeObjectURL(a.href), 2000);
    toast("کارت امتیاز ذخیره شد", "ok");
  } catch {
    toast("ساخت کارت امتیاز ناموفق بود", "warn");
  }
}
