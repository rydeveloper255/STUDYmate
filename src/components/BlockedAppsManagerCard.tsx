import React from 'react';
import { Smartphone, Lock, Check, Plus, Trash2, Ban } from 'lucide-react';
import { DEFAULT_BLOCKED_APPS, BlockedAppInfo } from '../lib/distractionShield';
import { triggerHaptic } from '../lib/haptics';

interface BlockedAppsManagerCardProps {
  blockAppsEnabled: boolean;
  blockedAppList: string[];
  onToggleBlockApps: (enabled: boolean) => void;
  onToggleAppBlocked: (packageName: string) => void;
  onSelectAll: () => void;
  onDeselectAll: () => void;
}

export const BlockedAppsManagerCard: React.FC<BlockedAppsManagerCardProps> = ({
  blockAppsEnabled,
  blockedAppList,
  onToggleBlockApps,
  onToggleAppBlocked,
  onSelectAll,
  onDeselectAll,
}) => {
  return (
    <div 
      id="blocked-apps-manager-card"
      className="p-5 rounded-3xl bg-slate-900/50 border border-white/10 space-y-4"
    >
      {/* Header */}
      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-rose-500/20 text-rose-400 border border-rose-500/30">
            <Ban className="h-5 w-5" />
          </div>
          <div>
            <h3 className="text-sm font-extrabold text-white">Block Distracting Apps</h3>
            <p className="text-[11px] text-slate-400 mt-0.5">
              Restricts distracting apps & feeds during active study sprints
            </p>
          </div>
        </div>

        {/* Master Switch */}
        <button
          id="btn-toggle-block-apps"
          type="button"
          onClick={() => {
            triggerHaptic('medium');
            onToggleBlockApps(!blockAppsEnabled);
          }}
          className={`relative inline-flex h-6 w-11 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
            blockAppsEnabled ? 'bg-rose-500' : 'bg-slate-800'
          }`}
          role="switch"
          aria-checked={blockAppsEnabled}
        >
          <span
            className={`pointer-events-none inline-block h-5 w-5 transform rounded-full bg-white shadow-lg ring-0 transition duration-200 ease-in-out ${
              blockAppsEnabled ? 'translate-x-5' : 'translate-x-0'
            }`}
          />
        </button>
      </div>

      {/* App Chips Grid when Block Apps is Enabled */}
      {blockAppsEnabled && (
        <div className="pt-3 border-t border-white/10 space-y-3 animate-in fade-in duration-200">
          <div className="flex items-center justify-between text-xs">
            <span className="text-slate-400 font-medium">Selected for blocking: {blockedAppList.length}</span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                onClick={() => {
                  triggerHaptic('light');
                  onSelectAll();
                }}
                className="text-[11px] text-sky-400 hover:text-sky-300 font-semibold cursor-pointer"
              >
                Select All
              </button>
              <span className="text-slate-600">•</span>
              <button
                type="button"
                onClick={() => {
                  triggerHaptic('light');
                  onDeselectAll();
                }}
                className="text-[11px] text-slate-400 hover:text-slate-300 font-semibold cursor-pointer"
              >
                Clear All
              </button>
            </div>
          </div>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-2">
            {DEFAULT_BLOCKED_APPS.map((app) => {
              const isSelected = blockedAppList.includes(app.packageName);
              return (
                <button
                  key={app.id}
                  type="button"
                  onClick={() => {
                    triggerHaptic('light');
                    onToggleAppBlocked(app.packageName);
                  }}
                  className={`p-2.5 rounded-2xl border text-left text-xs transition-all flex items-center justify-between gap-2 cursor-pointer ${
                    isSelected
                      ? 'bg-rose-500/15 border-rose-500/40 text-rose-200 font-bold shadow-sm'
                      : 'bg-white/[0.03] border-white/5 text-slate-400 hover:bg-white/[0.06] hover:text-slate-200'
                  }`}
                >
                  <div className="truncate">
                    <div className="truncate text-[12px]">{app.name}</div>
                    <div className="text-[10px] text-slate-500 font-normal">{app.category}</div>
                  </div>
                  <div className={`h-4 w-4 rounded-full border flex items-center justify-center shrink-0 ${
                    isSelected ? 'bg-rose-500 border-rose-400 text-white' : 'border-white/20'
                  }`}>
                    {isSelected && <Check className="h-3 w-3 stroke-[3]" />}
                  </div>
                </button>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
};
