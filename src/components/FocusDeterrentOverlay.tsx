import React from 'react';
import { ShieldAlert, ArrowLeft, Flame, Sparkles, CheckCircle2 } from 'lucide-react';
import { triggerHaptic } from '../lib/haptics';

interface FocusDeterrentOverlayProps {
  isOpen: boolean;
  topic: string;
  subject: string;
  streakDays: number;
  distractionAttempts: number;
  onReturnToStudy: () => void;
  onGiveUp: () => void;
  isStrictMode: boolean;
}

export const FocusDeterrentOverlay: React.FC<FocusDeterrentOverlayProps> = ({
  isOpen,
  topic,
  subject,
  streakDays,
  distractionAttempts,
  onReturnToStudy,
  onGiveUp,
  isStrictMode,
}) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/95 backdrop-blur-xl animate-in fade-in duration-200">
      <div 
        id="focus-deterrent-overlay-panel"
        className="w-full max-w-md p-6 rounded-3xl glass-panel border border-rose-500/40 shadow-2xl bg-gradient-to-b from-slate-900 via-rose-950/20 to-slate-950 text-center space-y-5"
      >
        <div className="mx-auto w-14 h-14 rounded-3xl bg-rose-500/20 border border-rose-500/30 flex items-center justify-center text-rose-400 shadow-lg shadow-rose-500/20 animate-bounce">
          <ShieldAlert className="h-7 w-7" />
        </div>

        <div>
          <span className="text-[10px] px-3 py-1 rounded-full bg-rose-500/20 text-rose-300 font-bold border border-rose-500/30 uppercase tracking-wider">
            Distraction Shield Active
          </span>
          <h3 className="text-xl font-black text-white mt-3">Stay in the Focus Zone!</h3>
          <p className="text-xs text-slate-400 mt-1">
            You are in an active study sprint for <strong className="text-slate-200">{topic}</strong> ({subject}).
          </p>
        </div>

        {/* Motivational Card */}
        <div className="p-4 rounded-2xl bg-slate-950/80 border border-white/5 space-y-2 text-left text-xs">
          <div className="flex items-center justify-between text-amber-300 font-bold">
            <span className="flex items-center gap-1.5">
              <Flame className="h-4 w-4 fill-amber-400" />
              {streakDays} Day Streak at Stake
            </span>
            <span className="text-rose-400 text-[11px]">
              {distractionAttempts} distraction attempts thwarted
            </span>
          </div>
          <p className="text-slate-400 text-[11px] leading-relaxed">
            Every uninterrupted minute primes your brain for long-term recall and boosts exam readiness.
          </p>
        </div>

        {/* Action Buttons */}
        <div className="space-y-2 pt-2">
          <button
            id="btn-return-to-focus"
            onClick={() => {
              triggerHaptic('success');
              onReturnToStudy();
            }}
            className="w-full py-3.5 px-4 rounded-2xl bg-gradient-to-r from-sky-400 to-sky-500 hover:from-sky-300 text-slate-950 font-black text-sm shadow-xl shadow-sky-500/25 flex items-center justify-center gap-2 cursor-pointer transition-all"
          >
            <Sparkles className="h-4 w-4 fill-slate-950" />
            <span>Resume Focus Sprint</span>
          </button>

          {!isStrictMode && (
            <button
              id="btn-quit-focus-sprint"
              onClick={() => {
                triggerHaptic('warning');
                onGiveUp();
              }}
              className="w-full py-2 px-4 rounded-2xl text-slate-500 hover:text-rose-400 text-xs font-semibold transition-colors cursor-pointer"
            >
              End Sprint Early
            </button>
          )}
        </div>
      </div>
    </div>
  );
};
