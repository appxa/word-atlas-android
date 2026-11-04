import { ArrowRight } from "lucide-react";
import { useState } from "react";
import { useAtlas } from "../atlas/context";
import { store } from "../store/store";

const LEVELS = [
  ["A2", "پایه", "واژه‌های روزمره‌ات هنوز محکم نیست"],
  ["B1", "میانی", "هدف نمرهٔ ۵٫۵ تا ۶"],
  ["B2", "متوسط-بالا", "هدف نمرهٔ ۶٫۵ تا ۷"],
  ["C1", "پیشرفته", "هدف نمرهٔ بالاتر از ۷٫۵"],
];

export function Onboarding() {
  const atlas = useAtlas();
  const [level, setLevel] = useState(1);
  const [goal, setGoal] = useState(10);
  const finish = () => store.settings({ onboarded: true, startLevel: atlas.cefr.indexOf(LEVELS[level][0]), dailyNew: goal });
  return (
    <div className="onboard" role="dialog" aria-modal="true" aria-label="خوش آمدی">
      <div className="onboard-card">
        <img className="onboard-art" src={`${import.meta.env.BASE_URL}brand/hero.webp`} alt="" />
        <div className="onboard-body">
          <p className="eyebrow">به واژه‌نگار خوش آمدی</p>
          <h1 className="display">
            {atlas.count.toLocaleString()} ستاره،
            <br />
            منتظر روشن‌شدن‌اند، یکی‌یکی.
          </h1>
          <fieldset>
            <legend>از کجا شروع می‌کنی؟</legend>
            <div className="choice-grid">
              {LEVELS.map(([code, name, desc], i) => (
                <button key={code} className={`choice ${level === i ? "is-on" : ""}`} onClick={() => setLevel(i)} aria-pressed={level === i}>
                  <span className={`chip chip-level lv-${code}`}>{code}</span>
                  <b>{name}</b>
                  <small>{desc}</small>
                </button>
              ))}
            </div>
          </fieldset>
          <fieldset>
            <legend>هر روز چند واژهٔ جدید یاد بگیری؟</legend>
            <div className="segmented segmented-wide">
              {[5, 10, 15, 20].map((n) => (
                <button key={n} className={goal === n ? "is-on" : ""} onClick={() => setGoal(n)} aria-pressed={goal === n}>
                  {n} ستاره{n === 10 ? " · پیشنهادی"  : ""}
                </button>
              ))}
            </div>
          </fieldset>
          <button className="btn btn-primary btn-xl" onClick={finish}>
            شروع  <ArrowRight size={20} />
          </button>
          <p className="muted">ثبت‌نام لازم نیست. پیشرفتت فقط روی همین دستگاه ذخیره می‌شود. هر زمان بخواهی می‌توانی تنظیمات را در «پروفایل» تغییر دهی.</p>
        </div>
      </div>
    </div>
  );
}
