export interface LevelState { level: string; total: number; downloaded: number; state: "idle" | "checking" | "downloading" | "complete" | "error"; error?: string; }
interface OfflineBridge { status(level: string): string; isDownloaded(level: string): boolean; hasMedia(path: string): boolean; start(level: string): void; remove(level: string): void; saveBackup(data: string, name: string): void; }
declare global { interface Window { NativeOffline?: OfflineBridge; } }
const REMOTE = "https://appxa.github.io/word-atlas/atlas/";
const LIVE = "https://0xlei.tech/words/atlas/";
export function mediaUrl(path: string, level: string, upstream = false): string {
  if (!window.NativeOffline) return upstream ? `${LIVE}${path}` : `${import.meta.env.BASE_URL}atlas/${path}`;
  return window.NativeOffline.isDownloaded(level) || window.NativeOffline.hasMedia(path)
    ? `https://appassets.androidplatform.net/offline/${path}`
    : `${upstream ? LIVE : REMOTE}${path}`;
}
