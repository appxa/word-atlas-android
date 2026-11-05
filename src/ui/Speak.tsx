import { Volume2 } from "lucide-react";
import { useState } from "react";
import { play } from "../lib/audio";
import { toast } from "./Toast";

export function Speak({
  url,
  text,
  accent = "uk",
  label,
  size = "md",
  rate = 1,
  className = "",
  kbd,
}: {
  url: string | null;
  text: string;
  accent?: "uk" | "us";
  label?: React.ReactNode;
  size?: "sm" | "md" | "lg";
  rate?: number;
  className?: string;
  kbd?: string;
}) {
  const [on, setOn] = useState(false);
  const run = async (e: React.MouseEvent) => {
    e.stopPropagation();
    setOn(true);
    try {
      if (await play(url, text, accent, rate) === "unsupported") toast("پخش صدا ممکن نیست. اتصال اینترنت یا دانلود آفلاین این سطح را بررسی کن.", "warn");
    } finally {
      setOn(false);
    }
  };
  return (
    <button
      type="button"
      className={`speak speak-${size} ${on ? "is-on" : ""} ${label ? "has-label" : ""} ${className}`}
      onClick={run}
      aria-label={typeof label === "string" ? `پخش تلفظ ${label}` : `پخش تلفظ ${text}`}
      title={kbd ? `خواندن (${kbd})` : "پخش تلفظ"}
    >
      <span className="speak-icon">
        <Volume2 size={size === "lg" ? 22 : size === "sm" ? 15 : 18} strokeWidth={1.8} />
        <span className="speak-waves" />
      </span>
      {label && <span className="speak-label">{label}</span>}
    </button>
  );
}
