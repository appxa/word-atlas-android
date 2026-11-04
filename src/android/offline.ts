export interface LevelState { level: string; total: number; downloaded: number; state: "idle" | "checking" | "downloading" | "complete" | "error"; error?: string; }
interface OfflineBridge { status(level: string): string; isDownloaded(level: string): boolean; start(level: string): void; remove(level: string): void; saveBackup(data: string, name: string): void; }
declare global { interface Window { NativeOffline?: OfflineBridge; } }
const REMOTE = "https://appxa.github.io/word-atlas/atlas/";
export function mediaUrl(path: string, level: string): string {
  if (!window.NativeOffline) return `${import.meta.env.BASE_URL}atlas/${path}`;
  return window.NativeOffline.isDownloaded(level)
    ? `https://appassets.androidplatform.net/offline/${path}`
    : `${REMOTE}${path}`;
}
