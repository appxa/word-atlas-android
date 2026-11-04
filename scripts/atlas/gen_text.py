"""Generate original learner entries for every headword (CC BY 4.0 content).

Usage: python3 scripts/atlas/gen_text.py [--limit N] [--workers W] [--batch B]
Output: .local/forge/text/<batch>.json (resumable)
"""
import argparse
import concurrent.futures as cf
import json
import re
import sys

from common import FORGE, load_env, stream_chat, with_retry

TOPICS = [
    "education", "work", "technology", "science", "environment", "health",
    "society", "government", "economy", "media", "arts", "travel", "city",
    "nature", "daily", "emotion", "thinking", "sport", "history",
]

SYSTEM = """You are the lead lexicographer of an open-source English vocabulary atlas for Chinese learners preparing for IELTS (band 5.5-7.5).
Write ORIGINAL content in your own words. The raw notes you receive are only a hint for which senses matter; never copy them.
Accuracy beats creativity: IPA, part of speech, Chinese meanings and grammar must be correct. Prefer standard British IPA for ipa_uk and General American for ipa_us, with slashes.
Return strict JSON only: {"entries":[...]} with one entry per input word, same order, using exactly this shape:
{
 "w": headword exactly as given (fix only obvious typos, and then set "flag"),
 "uk": "/IPA/", "us": "/IPA/",
"senses": [{"pos": "n.|v.|adj.|adv.|prep.|conj.|pron.|phr.|num.|int.|det."، "zh": "تعریف چینی مختصر، در دسترس؛ جدا شده، ≤14 کلمه"، "en": "آموزنده پسند. تعریف انگلیسی"، ≤1 آیلتس-مفید اول
 "ex": [
{"en": "جمله روزمره، 7 تا 13 کلمه، طبیعی، واضح، از سر واژه دقیقاً یا شکل عطف استفاده می کند"، "zh": "ترجمه چینی طبیعی"}،
{"en": "جملات به سبک آیلتس، 12 تا 22 کلمه، ثبت نام علمی/بحثی در مورد موضوع آیلتس"، "zh": "ترجمه چینی طبیعی"}
 ],
"col": ["2-4 ترکیب با ارزش بالا، هر کدام به عنوان "عبارت انگلیسی｜فارسی""]،
 "fam": ["0-4 word-family members as 'word (pos)'"],
 "syn": ["0-3 synonyms"], "ant": ["0-2 antonyms"],
"نکته": "≤36 کلمه در زبان چینی: مفیدترین نکته استفاده/تحلیل/حافظه. ریشه شناسی را ایجاد نکنید؛ صحبت در مورد استفاده بدون ریشه شناسی قابل اعتماد ممکن است منجر به کلمات گیج کننده شود."،
 "cefr": "A1|A2|B1|B2|C1|C2",
 "topic": one of [TOPICS],
 "scene": "Depicting the FIRST sense: one concrete, visually clear illustration idea (≤28 words, English) that makes the core meaning obvious at a glance: a single moment with people/objects/places. Abstract words become a clear metaphorical scene. Never mention text, letters, signs, labels or numbers.",
 "flag": "" or short note if the headword is malformed, offensive, or not a real English word
}
Sentences must be safe for all ages, culturally neutral, and must not mention real brands or living public figures. Do not reuse the same names across entries; avoid 'Tom' and 'Mary'."""


def prompt_for(batch):
    lines = []
    for g in batch:
        raw = " / ".join(g["raw"])[:160]
        lines.append(f'- {g["word"]}  (notes: {raw})')
    return "Words:\n" + "\n".join(lines)


def run_batch(env, model, idx, batch, outdir):
    out = outdir / f"{idx:04d}.json"
    if out.exists():
        return idx, "cached"
    payload = {
        "model": model,
        "messages": [
            {"role": "system", "content": SYSTEM.replace("[TOPICS]", json.dumps(TOPICS))},
            {"role": "user", "content": prompt_for(batch)},
        ],
    }

    def call():
        text = stream_chat(env, payload)
        text = re.sub(r"^```(?:json)?|```$", "", text.strip()).strip()
        data = json.loads(text)
        entries = data["entries"]
        if len(entries) != len(batch):
            raise ValueError(f"expected {len(batch)} got {len(entries)}")
        return entries

    entries = with_retry(call, tries=4, base=5)
    for g, e in zip(batch, entries):
        e["hid"] = g["hid"]
        e["src"] = g["word"]
    out.write_text(json.dumps(entries, ensure_ascii=False))
    return idx, "ok"


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--limit", type=int, default=0)
    ap.add_argument("--start", type=int, default=0)
    ap.add_argument("--workers", type=int, default=8)
    ap.add_argument("--batch", type=int, default=20)
    ap.add_argument("--model", default="gpt-6")
    ap.add_argument("--out", default="text")
    a = ap.parse_args()
    env = load_env()
    hw = json.loads((FORGE / "headwords.json").read_text())
    hw = hw[a.start:]
    if a.limit:
        hw = hw[: a.limit]
    outdir = FORGE / a.out
    outdir.mkdir(parents=True, exist_ok=True)
    batches = [hw[i : i + a.batch] for i in range(0, len(hw), a.batch)]
    base = a.start // a.batch
    done = fail = 0
    with cf.ThreadPoolExecutor(a.workers) as ex:
        futs = {ex.submit(run_batch, env, a.model, base + i, b, outdir): base + i for i, b in enumerate(batches)}
        for f in cf.as_completed(futs):
            try:
                f.result()
                done += 1
            except Exception as e:
                fail += 1
                print("FAIL", futs[f], str(e)[:200], flush=True)
            if (done + fail) % 10 == 0:
                print(f"progress {done+fail}/{len(batches)} fail={fail}", flush=True)
    print(f"finished ok={done} fail={fail}", flush=True)


if __name__ == "__main__":
    sys.exit(main())
