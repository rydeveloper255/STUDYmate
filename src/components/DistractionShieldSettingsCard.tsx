import React from 'react';
import { Shield, ShieldCheck, ShieldAlert, BellOff, Volume2, Check, Settings, Sparkles, RefreshCw } from 'lucide-react';
import { DistractionShieldSettings } from '../types';
import { triggerHaptic } from '../lib/haptics';

interface DistractionShieldSettingsCardProps {
  settings: DistractionShieldSettings;
  hasPermission: boolean;
  isActiveSprint: boolean;
  onToggleShield: (enabled: boolean) => void;
  onRequestPermission: () => void;
  onUpdateSettings: (newSettings: Partial<DistractionShieldSettings>) => void;
}

export const DistractionShieldSettingsCard: React.FC<DistractionShieldSettingsCardProps> = ({
  settings,
  hasPermission,
  isActiveSprint,
  onToggleShield,
  onRequestPermission,
  onUpdateSettings,
}) => {
  return (
    <div 
      id="distraction-shield-settings-card"
      className={`p-5 rounded-3xl border transition-all duration-300 ${
        settings.enabled
          ? 'bg-gradient-to-b from-sky-950/40 via-slate-900/60 to-slate-900/90 border-sky-500/40 shadow-xl shadow-sky-950/30'
          : 'bg-slate-900/50 border-white/10'
      }`}
    >
      {/* Top Bar: Icon + Title + Switch */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className={`p-2.5 rounded-2xl border transition-colors ${
            settings.enabled 
              ? 'bg-sky-500/20 text-sky-400 border-sky-500/30' 
              : 'bg-white/5 text-slate-400 border-white/10'
          }`}>
            <Shield className="h-5 w-5" />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h3 className="text-sm font-extrabold text-white">Distraction Shield (DND)</h3>
              {isActiveSprint && settings.enabled && (
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-bold border border-emerald-500/30 flex items-center gap-1">
                  <span className="h-1.5 w-1.5 rounded-full bg-emerald-400 animate-ping" />
                  Protected
                </span>
              )}
            </div>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Reduces notification & call interruptions with Android DND during deep sprints
            </p>
          </div>
        </div>

        {/* Master Switch */}
        <button
          id="btn-toggle-distraction-shield"
          type="button"
          onClick={() => {
            triggerHaptic('medium');
            onToggleShield(!settings.enabled);
          }}
          className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
            settings.enabled ? 'bg-sky-500' : 'bg-slate-800'
          }`}
          role="switch"
          aria-checked={settings.enabled}
        >
          <span
            className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out ${
              settings.enabled ? 'translate-x-5' : 'translate-x-0'
            }`}
          />
        </button>
      </div>

      {/* Expanded Controls when Shield is enabled */}
      {settings.enabled && (
        <div className="mt-4 pt-4 border-t border-white/10 space-y-3 animate-in fade-in duration-200">
          {/* Permission Status Banner */}
          {!hasPermission ? (
            <div className="p-3 rounded-2xl bg-amber-500/10 border border-amber-500/30 flex items-center justify-between gap-3 text-xs">
              <div className="flex items-center gap-2 text-amber-300">
                <ShieldAlert className="h-4 w-4 shrink-0" />
                <span>Notification Policy permission required for automatic DND</span>
              </div>
              <button
                id="btn-request-dnd-permission"
                onClick={() => {
                  triggerHaptic('light');
                  onRequestPermission();
                }}
                className="px-3 py-1 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold text-[11px] whitespace-nowrap cursor-pointer transition-all"
              >
                Allow Access
              </button>
            </div>
          ) : (
            <div className="p-2.5 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center gap-2 text-xs text-emerald-300">
              <ShieldCheck className="h-4 w-4 shrink-0" />
              <span>System DND access granted. Priority filter will activate when timer starts.</span>
            </div>
          )}

          {/* Sub-toggles */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 text-xs">
            <label className="flex items-center gap-2 p-2.5 rounded-xl bg-slate-950/40 border border-white/5 text-slate-300 cursor-pointer hover:bg-slate-950/60">
              <input
                type="checkbox"
                checked={settings.autoRestore}
                onChange={(e) => {
                  triggerHaptic('light');
                  onUpdateSettings({ autoRestore: e.target.checked });
                }}
                className="rounded border-white/20 text-sky-500 focus:ring-sky-500 h-3.5 w-3.5"
              />
              <span>Auto-restore previous DND state on finish</span>
            </label>

            <label className="flex items-center gap-2 p-2.5 rounded-xl bg-slate-950/40 border border-white/5 text-slate-300 cursor-pointer hover:bg-slate-950/60">
              <input
                type="checkbox"
                checked={settings.allowPriorityAlarms}
                onChange={(e) => {
                  triggerHaptic('light');
                  onUpdateSettings({ allowPriorityAlarms: e.target.checked });
                }}
                className="rounded border-white/20 text-sky-500 focus:ring-sky-500 h-3.5 w-3.5"
              />
              <span>Keep priority alarms & reminders audible</span>
            </label>
          </div>
        </div>
      )}
    </div>
  );
};
