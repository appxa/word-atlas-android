export interface LevelState { level: string; total: number; downloaded: number; state: "idle" | "checking" | "downloading" | "cancelling" | "deleting" | "complete" | "error"; error?: string; }
interface OfflineBridge { status(level: string): string; isDownloaded(level: string): boolean; hasMedia(path: string): boolean; start(level: string): void; cancel(level: string): void; remove(level: string): void; saveBackup(data: string, name: string): void; }
declare global { interface Window { NativeOffline?: OfflineBridge; } }
const REMOTE = "https://appxa.github.io/word-atlas/atlas/";
const SUPPLEMENTAL = "https://appxa.github.io/word-atlas-audio/";
export function mediaUrl(path: string, level: string, upstream = false): string {
  if (!window.NativeOffline) return upstream ? `${SUPPLEMENTAL}${path}` : `${import.meta.env.BASE_URL}atlas/${path}`;
  return window.NativeOffline.isDownloaded(level) || window.NativeOffline.hasMedia(path)
    ? `https://appassets.androidplatform.net/offline/${path}`
    : `${upstream ? SUPPLEMENTAL : REMOTE}${path}`;
}
