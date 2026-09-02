import React from 'react';
import { ExamInfo, UserProfile } from '../types';
import { Sparkles, X, Target, Flame, Play, Clock, Award } from 'lucide-react';

interface DailyBriefingModalProps {
  isOpen: boolean;
  onClose: () => void;
  activeExam: ExamInfo;
  user: UserProfile;
  onStartSprint: (minutes: number, subject: string, topic: string) => void;
}

export const DailyBriefingModal: React.FC<DailyBriefingModalProps> = ({
  isOpen,
  onClose,
  activeExam,
  user,
  onStartSprint,
}) => {
  if (!isOpen) return null;

  const quote = {
    text: "Success doesn't come from what you do occasionally, it comes from what you do consistently.",
    author: "Marie Forleo"
  };

  const topPriorities = [
    { subject: activeExam.subjects[0] || 'Core Subject', topic: 'High-Yield Revision & PYQ Analysis', minutes: 30, tag: 'Weak Area Fix' },
    { subject: activeExam.subjects[1] || 'Current Affairs', topic: 'Active Recall Flashcard Deck (20 cards)', minutes: 20, tag: 'Spaced Repetition' },
    { subject: activeExam.subjects[2] || 'Aptitude', topic: 'Speed Quiz & Diagnostic Review', minutes: 25, tag: 'Exam Pacing' }
  ];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="w-full max-w-lg rounded-3xl glass-panel p-5 sm:p-6 border border-sky-500/30 shadow-2xl relative overflow-hidden">
        {/* Background decorative glow */}
        <div className="absolute top-0 right-0 w-48 h-48 bg-sky-500/10 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-0 w-48 h-48 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none" />

        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-white/10 relative z-10">
          <div className="flex items-center gap-2.5">
            <div className="h-10 w-10 rounded-2xl bg-gradient-to-tr from-sky-400 to-indigo-600 flex items-center justify-center shadow-lg shadow-sky-500/20">
              <Sparkles className="h-5 w-5 text-white" />
            </div>
            <div>
              <h3 className="text-base sm:text-lg font-bold text-white flex items-center gap-1.5">
                Daily AI Briefing
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-sky-500/20 text-sky-300 font-semibold border border-sky-500/30">
                  Live
                </span>
              </h3>
              <p className="text-xs text-slate-400">Target: {activeExam.shortName} ({activeExam.daysRemaining} days remaining)</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1.5 rounded-xl bg-white/[0.06] hover:bg-white/[0.12] text-slate-400 hover:text-white transition-colors cursor-pointer"
          >
            <X className="h-5 w-5" />
          </button>
        </div>

        {/* Motivational Card */}
        <div className="my-4 p-3.5 rounded-2xl bg-gradient-to-r from-sky-950/50 to-indigo-950/50 border border-sky-500/20 relative z-10">
          <p className="text-xs sm:text-sm text-sky-200 italic leading-relaxed">
            "{quote.text}"
          </p>
          <p className="text-[11px] text-sky-400 font-semibold mt-1 text-right">— {quote.author}</p>
        </div>

        {/* Snapshot Metrics */}
        <div className="grid grid-cols-3 gap-2 mb-4 relative z-10">
          <div className="p-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-center">
            <div className="flex items-center justify-center gap-1 text-amber-400 text-xs font-bold mb-0.5">
              <Flame className="h-3.5 w-3.5 fill-amber-400" />
              <span>{user.streakDays} Days</span>
            </div>
            <span className="text-[10px] text-slate-400">Current Streak</span>
          </div>

          <div className="p-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-center">
            <div className="flex items-center justify-center gap-1 text-sky-400 text-xs font-bold mb-0.5">
              <Clock className="h-3.5 w-3.5" />
              <span>{user.todayFocusedMinutes}/{user.studyGoalMinutesPerDay}m</span>
            </div>
            <span className="text-[10px] text-slate-400">Daily Target</span>
          </div>

          <div className="p-2.5 rounded-xl bg-white/[0.04] border border-white/10 text-center">
            <div className="flex items-center justify-center gap-1 text-indigo-400 text-xs font-bold mb-0.5">
              <Award className="h-3.5 w-3.5" />
              <span>{activeExam.readinessScore}%</span>
            </div>
            <span className="text-[10px] text-slate-400">Readiness</span>
          </div>
        </div>

        {/* High-Impact Actions for Today */}
        <div className="mb-4 relative z-10">
          <div className="text-xs font-bold text-slate-300 uppercase tracking-wider mb-2 flex items-center justify-between">
            <span>NOVA's High-Yield Plan for Today</span>
            <span className="text-[11px] text-sky-400 lowercase font-normal">75 mins total</span>
          </div>

          <div className="space-y-2">
            {topPriorities.map((item, idx) => (
              <div
                key={idx}
                className="p-2.5 rounded-xl bg-white/[0.05] hover:bg-white/[0.09] border border-white/10 flex items-center justify-between gap-3 transition-colors"
              >
                <div className="min-w-0 flex-1">
                  <div className="flex items-center gap-1.5 mb-0.5">
                    <span className="text-[10px] px-1.5 py-0.5 rounded bg-sky-500/20 text-sky-300 font-medium">
                      {item.tag}
                    </span>
                    <span className="text-xs font-semibold text-white truncate">{item.subject}</span>
                  </div>
                  <p className="text-[11px] text-slate-300 truncate">{item.topic}</p>
                </div>
                <button
                  onClick={() => {
                    onClose();
                    onStartSprint(item.minutes, item.subject, item.topic);
                  }}
                  className="px-2.5 py-1.5 rounded-lg bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 text-xs font-semibold border border-sky-500/30 flex items-center gap-1 shrink-0 cursor-pointer"
                >
                  <Play className="h-3 w-3 fill-sky-400 text-sky-400" />
                  <span>{item.minutes}m</span>
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Footer Actions */}
        <div className="flex gap-2 relative z-10">
          <button
            onClick={() => {
              onClose();
              onStartSprint(25, topPriorities[0].subject, topPriorities[0].topic);
            }}
            className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-sky-500 via-sky-400 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-slate-950 font-bold text-xs sm:text-sm shadow-lg shadow-sky-500/20 flex items-center justify-center gap-2 cursor-pointer"
          >
            <Play className="h-4 w-4 fill-slate-950" />
            Launch First 25-Min Sprint Now
          </button>
        </div>
      </div>
    </div>
  );
};
