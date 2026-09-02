import React from 'react';
import { UserProfile, ExamInfo, SubjectItem, MockTestAttempt } from '../types';
import {
  Award,
  TrendingUp,
  AlertTriangle,
  CheckCircle2,
  Clock,
  Play,
  ArrowRight,
  BookOpen,
  Sparkles
} from 'lucide-react';

interface ExamReadinessScreenProps {
  user: UserProfile;
  activeExam: ExamInfo;
  subjects: SubjectItem[];
  testAttempts: MockTestAttempt[];
  onStartFocusSprint: (minutes: number, subject: string, topic: string) => void;
  onBack: () => void;
}

export const ExamReadinessScreen: React.FC<ExamReadinessScreenProps> = ({
  user,
  activeExam,
  subjects,
  testAttempts,
  onStartFocusSprint,
  onBack,
}) => {
  const weakAreas = [
    { subject: 'Polity & Governance', topic: 'Article 14 vs 21 & Basic Structure', accuracy: 45 },
    { subject: 'Economy & Development', topic: 'Headline vs Core Inflation CPI Basket', accuracy: 52 },
    { subject: 'Environment & Ecology', topic: 'Ramsar Wetlands & National Biodiversity Act', accuracy: 58 },
  ];

  const daysOfWeek = ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'];
  const dailyFocusMinutes = [45, 60, 50, 75, 40, 90, user.todayFocusedMinutes];

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-24 animate-in fade-in duration-300">
      {/* Top Banner */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-extrabold text-white tracking-tight flex items-center gap-2">
            <Award className="h-6 w-6 text-indigo-400" />
            Exam Readiness Diagnostic Index
          </h1>
          <p className="text-xs text-slate-400">
            Comprehensive multi-metric analysis for {activeExam.name} ({activeExam.daysRemaining} days remaining)
          </p>
        </div>
        <button
          onClick={onBack}
          className="px-3.5 py-1.5 rounded-xl bg-white/[0.08] hover:bg-white/[0.14] text-xs font-semibold text-slate-300 cursor-pointer"
        >
          Back
        </button>
      </div>

      {/* Main Readiness Scorecard */}
      <div className="p-6 sm:p-8 rounded-3xl glass-panel border border-indigo-500/30 grid grid-cols-1 md:grid-cols-3 gap-6 items-center bg-gradient-to-br from-slate-900 via-indigo-950/20 to-slate-900">
        <div className="md:col-span-1 flex flex-col items-center justify-center text-center">
          <div className="relative h-32 w-32 rounded-full border-4 border-slate-800 flex items-center justify-center">
            <div
              className="absolute inset-0 rounded-full border-4 border-indigo-400 border-t-transparent"
              style={{ transform: `rotate(${activeExam.readinessScore * 3.6}deg)` }}
            />
            <div>
              <span className="text-3xl font-extrabold text-white">{activeExam.readinessScore}%</span>
              <div className="text-[10px] text-slate-400 uppercase font-bold">Overall Index</div>
            </div>
          </div>
          <div className="mt-2 text-xs font-bold text-emerald-400">Strong Momentum</div>
        </div>

        <div className="md:col-span-2 space-y-3">
          <h3 className="text-base font-bold text-white">Target Score Forecast</h3>
          <p className="text-xs text-slate-300 leading-relaxed">
            Based on your mock accuracy (78%), active flashcard retention, and daily study consistency, your estimated percentile is in the <strong>Top 8%</strong>.
          </p>

          <div className="grid grid-cols-3 gap-2 pt-2">
            <div className="p-3 rounded-2xl bg-white/[0.04] border border-white/10 text-center">
              <div className="text-sm font-bold text-white">{user.streakDays} Days</div>
              <div className="text-[10px] text-slate-400">Consistency Streak</div>
            </div>
            <div className="p-3 rounded-2xl bg-white/[0.04] border border-white/10 text-center">
              <div className="text-sm font-bold text-white">{testAttempts.length} Tests</div>
              <div className="text-[10px] text-slate-400">Mocks Attempted</div>
            </div>
            <div className="p-3 rounded-2xl bg-white/[0.04] border border-white/10 text-center">
              <div className="text-sm font-bold text-white">{user.totalFocusMinutes} Mins</div>
              <div className="text-[10px] text-slate-400">Total Focus Time</div>
            </div>
          </div>
        </div>
      </div>

      {/* Subject Mastery Radar Bars */}
      <div className="p-6 rounded-3xl glass-panel border border-white/10 space-y-4">
        <h3 className="text-base font-bold text-white flex items-center gap-2">
          <BookOpen className="h-4 w-4 text-sky-400" />
          Subject-Wise Mastery Breakdown
        </h3>

        <div className="space-y-3">
          {subjects.map((sub) => (
            <div key={sub.id} className="space-y-1.5">
              <div className="flex items-center justify-between text-xs">
                <span className="font-bold text-white">{sub.name}</span>
                <span className="text-sky-300 font-bold">{sub.masteryPercentage}%</span>
              </div>
              <div className="w-full h-2 rounded-full bg-slate-800 overflow-hidden">
                <div
                  className="h-full bg-gradient-to-r from-sky-400 to-indigo-500 rounded-full"
                  style={{ width: `${sub.masteryPercentage}%` }}
                />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Weak Areas Remediator */}
      <div className="p-6 rounded-3xl glass-panel border border-rose-500/30 space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <AlertTriangle className="h-4 w-4 text-rose-400" />
            <h3 className="text-base font-bold text-white">Priority Weak Areas</h3>
          </div>
          <span className="text-xs text-rose-300 font-semibold">Immediate Impact Sprints</span>
        </div>

        <div className="space-y-2.5">
          {weakAreas.map((w, idx) => (
            <div
              key={idx}
              className="p-3.5 rounded-2xl bg-white/[0.03] border border-white/10 flex items-center justify-between gap-3"
            >
              <div>
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-white">{w.topic}</span>
                  <span className="text-[10px] px-2 py-0.5 rounded bg-rose-500/20 text-rose-300 font-bold">
                    {w.accuracy}% Accuracy
                  </span>
                </div>
                <div className="text-[11px] text-slate-400 mt-0.5">{w.subject}</div>
              </div>

              <button
                onClick={() => onStartFocusSprint(25, w.subject, w.topic)}
                className="px-3 py-1.5 rounded-xl bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 border border-rose-500/30 text-xs font-bold flex items-center gap-1 cursor-pointer"
              >
                <Play className="h-3 w-3 fill-rose-400 text-rose-400" />
                <span>Sprint (25m)</span>
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Weekly Study Time Activity Histogram */}
      <div className="p-6 rounded-3xl glass-panel border border-white/10 space-y-4">
        <h3 className="text-base font-bold text-white flex items-center gap-2">
          <Clock className="h-4 w-4 text-emerald-400" />
          Weekly Study Time Distribution (Minutes)
        </h3>

        <div className="flex items-end justify-between gap-2 h-36 pt-6 px-2">
          {daysOfWeek.map((day, idx) => {
            const mins = dailyFocusMinutes[idx];
            const maxMins = 120;
            const heightPercent = Math.min(100, Math.round((mins / maxMins) * 100));

            return (
              <div key={day} className="flex-1 flex flex-col items-center gap-2 h-full justify-end">
                <div className="text-[10px] text-slate-400 font-mono">{mins}m</div>
                <div
                  className="w-full max-w-[36px] bg-gradient-to-t from-sky-600 to-sky-400 rounded-t-lg transition-all duration-500"
                  style={{ height: `${heightPercent}%` }}
                />
                <span className="text-xs text-slate-300 font-semibold">{day}</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
