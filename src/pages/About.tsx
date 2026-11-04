import { ArrowUpRight, Database, Github, Heart } from "lucide-react";
import { useAtlas } from "../atlas/context";
import { REPO_URL } from "../config";

const BASE = import.meta.env.BASE_URL;

export function About() {
  const atlas = useAtlas();
  return (
    <div className="page about">
      <header className="about-hero">
        <img src={`${BASE}brand/og.webp`} alt="" />
        <div>
          <p className="eyebrow">درباره واژه‌نگار</p>
          <h1 className="display">یک واژه، یک تصویر؛ آیلتس را روشن کن.</h1>
          <p className="lede">
            واژه‌نگار {atlas.count.toLocaleString()} واژهٔ کاربردی آیلتس را در قالب یک واژه‌نامهٔ تصویری گرد آورده است. هر واژه همراه تصویر، مثال روزمره و آیلتسی، تلفظ بریتانیایی و آمریکایی، ترکیب‌های رایج و نکته‌های کاربردی ارائه می‌شود. با یادگیری هر واژه، ستاره‌ای در نقشه‌ات روشن می‌شود.
          </p>
          <p className="lede">رایگان، متن‌باز و بدون ثبت‌نام است. کد با MIT و واژه‌نامه با CC BY 4.0 منتشر شده؛ هر کسی می‌تواند استفاده کند.</p>
          <div className="about-actions">
            <a className="btn btn-primary" href={REPO_URL} target="_blank" rel="noreferrer">
              <Github size={18} /> مشاهده در GitHub
            </a>
            <a className="btn btn-ghost" href={`${REPO_URL}/tree/main/data`} target="_blank" rel="noreferrer">
              <Database size={18} /> دانلود واژه‌نامه JSON / CSV
            </a>
          </div>
        </div>
      </header>

      <div className="about-grid">
        <section className="panel">
          <h3 className="eyebrow">چرا در ذهن می‌ماند</h3>
          <ul className="feature-list">
            <li>
              <b>اول ببین.</b> هر واژه یک تصویر دارد که همان اتفاق مثال اول را نشان می‌دهد.
            </li>
            <li>
              <b>بعد به یاد بیاور.</b> انتخاب معنی، کامل‌کردن جمله با تصویر، تشخیص واژه از صدا و املای آن؛ از شناختن تا نوشتن.
            </li>
            <li>
              <b>در زمان مناسب مرور کن.</b> FSRS همان الگوریتم حافظه متن‌باز Anki جدید است؛ مرور را فقط وقتی برنامه‌ریزی می‌کند که نزدیک فراموشی باشی.
            </li>
            <li>
              <b>امتیاز واقعی.</b> فقط اولین پاسخ حساب می‌شود. واژه‌های اشتباه با شکل تمرین دیگری برمی‌گردند، اما درصد درستی را بالا نمی‌برند.
            </li>
            <li>
              <b>پیشرفتت مال خودت است.</b> بدون حساب کاربری، بدون سرور؛ داده‌ها در مرورگر خودت ذخیره می‌شود و هر وقت بخواهی می‌توانی خروجی بگیری.
            </li>
          </ul>
        </section>

        <section className="panel">
          <h3 className="eyebrow">کلیدهای میان‌بر</h3>
          <dl className="keys">
            <div>
              <dt>
                <kbd>⌘</kbd>
                <kbd>K</kbd> یا  <kbd>/</kbd>
              </dt>
              <dd>جستجوی هر واژه‌ای</dd>
            </div>
            <div>
              <dt>
                <kbd>1</kbd>–<kbd>4</kbd>
              </dt>
              <dd>انتخاب پاسخ</dd>
            </div>
            <div>
              <dt>
                <kbd>Enter</kbd>
              </dt>
              <dd>تأیید / ادامه</dd>
            </div>
            <div>
              <dt>
                <kbd>←</kbd> <kbd>→</kbd>
              </dt>
              <dd>در کارت واژه، جابه‌جایی قبلی/بعدی</dd>
            </div>
            <div>
              <dt>
                <kbd>Esc</kbd>
              </dt>
              <dd>بستن پنجره</dd>
            </div>
          </dl>
        </section>

        <section className="panel">
          <h3 className="eyebrow">محتوا از کجا می‌آید</h3>
          <p className="muted">
            معنی‌ها، مثال‌ها، هم‌نشین‌ها و نکته‌ها با مدل زبانی بزرگ طبق قواعد یکدست واژه‌نامه‌ای بازنویسی شده‌اند؛ تصویرسازی‌ها با مدل تصویر و در سبک واحد برای هر واژه کشیده شده‌اند؛ تلفظ و خواندن با ElevenLabs ساخته شده (بریتیش: Alice، امریکن: Sarah). همه محتوا بازرسی خودکار ساختار و یکدستی شده، اما ویرایش انسانی تک‌تک نشده. اگر خطایی دیدی، در GitHub ایشو بزن یا مستقیم داده را درست کن و PR بفرست.
          </p>
          <p className="muted">واژه‌نگار فهرست رسمی آیلتس نیست و وعده نمره نمی‌دهد. فقط می‌خواهد حفظ واژه را کمی زیباتر و محکم‌تر کند.</p>
        </section>

        <section className="panel">
          <h3 className="eyebrow">برای توسعه‌دهندگان</h3>
          <p className="muted">
            فایل‌های <code>data/word-atlas.json</code> و <code>data/word-atlas.csv</code> واژه‌نامهٔ کامل‌اند؛ داده‌های بخش‌بندی‌شده، تصاویر و صداهای برنامه در <code>public/atlas</code> قرار دارند. می‌توانی با رعایت مجوز و ذکر منبع از آن‌ها در ابزارهای یادگیری استفاده کنی.
          </p>
          <a className="link" href={`${REPO_URL}#readme`} target="_blank" rel="noreferrer">
            مطالعه مستندات  <ArrowUpRight size={14} />
          </a>
        </section>
      </div>
      <p className="colophon">
        <Heart size={14} /> ساخته‌شده برای هر کسی که واژه حفظ می‌کند.
      </p>
    </div>
  );
}
