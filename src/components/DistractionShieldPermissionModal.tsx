import React from 'react';
import { Shield, ShieldAlert, CheckCircle2, ArrowRight, X, BellOff, Volume2, Lock } from 'lucide-react';
import { triggerHaptic } from '../lib/haptics';

interface DistractionShieldPermissionModalProps {
  isOpen: boolean;
  onGrantPermission: () => void;
  onContinueWithoutShield: () => void;
  onClose: () => void;
}

export const DistractionShieldPermissionModal: React.FC<DistractionShieldPermissionModalProps> = ({
  isOpen,
  onGrantPermission,
  onContinueWithoutShield,
  onClose,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-in fade-in duration-200">
      <div 
        id="distraction-shield-permission-modal"
        className="w-full max-w-md p-6 rounded-3xl glass-panel border border-sky-500/40 shadow-2xl bg-gradient-to-b from-slate-900 via-slate-900 to-slate-950 text-slate-100 space-y-5"
      >
        {/* Header */}
        <div className="flex items-start justify-between">
          <div className="flex items-center gap-3">
            <div className="p-3 rounded-2xl bg-sky-500/20 border border-sky-500/30 text-sky-400">
              <ShieldAlert className="h-6 w-6" />
            </div>
            <div>
              <h3 className="text-base font-extrabold text-white">Enable Distraction Shield</h3>
              <p className="text-xs text-sky-300/80 font-medium">Notification Policy Access Required</p>
            </div>
          </div>
          <button
            onClick={() => {
              triggerHaptic('light');
              onClose();
            }}
            className="p-1.5 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition-all cursor-pointer"
            title="Close"
          >
            <X className="h-4 w-4" />
          </button>
        </div>

        {/* Informative Body */}
        <div className="space-y-3 text-xs text-slate-300 leading-relaxed bg-slate-950/60 p-4 rounded-2xl border border-white/5">
          <p>
            StudyMate utilizes Android’s standard <strong className="text-white">Do Not Disturb (DND)</strong> system controls to quiet distracting app notifications and message spam while your focus sprint is running.
          </p>
          <div className="space-y-2 pt-1">
            <div className="flex items-center gap-2 text-emerald-300">
              <CheckCircle2 className="h-4 w-4 shrink-0" />
              <span>Prioritizes alarms and essential calls</span>
            </div>
            <div className="flex items-center gap-2 text-emerald-300">
              <CheckCircle2 className="h-4 w-4 shrink-0" />
              <span>Restores your original phone settings automatically</span>
            </div>
            <div className="flex items-center gap-2 text-emerald-300">
              <CheckCircle2 className="h-4 w-4 shrink-0" />
              <span>Zero hidden background telemetry or rooting</span>
            </div>
          </div>
        </div>

        {/* Action Buttons */}
        <div className="space-y-2 pt-1">
          <button
            id="btn-grant-shield-permission"
            onClick={() => {
              triggerHaptic('medium');
              onGrantPermission();
            }}
            className="w-full py-3 px-4 rounded-2xl bg-gradient-to-r from-sky-400 to-sky-500 hover:from-sky-300 text-slate-950 font-extrabold text-xs shadow-lg shadow-sky-500/25 flex items-center justify-center gap-2 cursor-pointer transition-all"
          >
            <span>Allow Access in Android Settings</span>
            <ArrowRight className="h-4 w-4" />
          </button>

          <button
            id="btn-continue-without-shield"
            onClick={() => {
              triggerHaptic('light');
              onContinueWithoutShield();
            }}
            className="w-full py-2.5 px-4 rounded-2xl bg-white/[0.06] hover:bg-white/[0.12] text-slate-300 hover:text-white font-semibold text-xs border border-white/10 transition-all cursor-pointer"
          >
            Continue Focus Sprint Without Shield
          </button>
        </div>
      </div>
    </div>
  );
};
