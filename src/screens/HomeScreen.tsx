import React from 'react';
import { UserProfile, ExamInfo, SubjectItem, VacancyItem } from '../types';
import {
  Sparkles,
  Play,
  Flame,
  Award,
  BookOpen,
  Target,
  Shield,
  FileText,
  BellRing,
  ArrowRight,
  TrendingUp,
  CheckCircle2,
  Clock,
  Layers,
  ChevronRight
} from 'lucide-react';

interface HomeScreenProps {
  user: UserProfile;
  activeExam: ExamInfo;
  subjects: SubjectItem[];
  vacancies: VacancyItem[];
  onNavigateTab: (tab: 'home' | 'nova' | 'study' | 'practice' | 'focus' | 'updates') => void;
  onOpenNovaWithPrompt: (prompt: string) => void;
  onStartFocusSprint: (minutes: number, subject: string, topic: string) => void;
  onOpenSummarizer: () => void;
  onOpenReadiness: () => void;
  onOpenDailyBriefing: () => void;
}

export const HomeScreen: React.FC<HomeScreenProps> = ({
  user,
  activeExam,
  subjects,
  vacancies,
  onNavigateTab,
  onOpenNovaWithPrompt,
  onStartFocusSprint,
  onOpenSummarizer,
  onOpenReadiness,
  onOpenDailyBriefing,
}) => {
  const topSubject = subjects[0] || { name: 'Core Subject', masteryPercentage: 75 };
  const weakTopic = 'Landmark Judgments & Fundamental Rights (Polity)';

  return (
    <div className="space-y-6 pb-24 animate-in fade-in duration-300">
      {/* 1. Live Target Exam Banner with Countdown & Readiness Score */}
      <section className="relative overflow-hidden rounded-3xl glass-panel p-5 sm:p-7 border border-white/15 shadow-2xl bg-gradient-to-br from-slate-900/90 via-sky-950/40 to-slate-900/90">
        <div className="absolute top-0 right-0 -mt-8 -mr-8 w-64 h-64 bg-sky-500/15 rounded-full blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-0 -mb-8 -ml-8 w-64 h-64 bg-indigo-500/15 rounded-full blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col md:flex-row md:items-center md:justify-between gap-5">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="px-2.5 py-1 rounded-full bg-sky-500/20 text-sky-300 text-xs font-bold border border-sky-500/30 flex items-center gap-1.5">
                <span className="h-2 w-2 rounded-full bg-sky-400 animate-pulse" />
                Target Exam 2026
              </span>
              <span className="text-xs text-slate-300 bg-white/[0.06] px-2.5 py-1 rounded-full border border-white/10">
                Exam Date: {activeExam.examDate}
              </span>
            </div>

            <h1 className="text-2xl sm:text-3xl font-extrabold text-white tracking-tight">
              {activeExam.name}
            </h1>

            <p className="text-sm text-slate-300 max-w-xl">
              <span className="font-semibold text-amber-300">{activeExam.daysRemaining} days</span> remaining until preliminary examination. Your preparation is currently in the{' '}
              <span className="font-semibold text-emerald-400">High Retention Window</span>.
            </p>

            <div className="pt-2 flex flex-wrap items-center gap-2">
              <button
                id="home-daily-briefing-btn"
                onClick={onOpenDailyBriefing}
                className="px-3.5 py-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white text-xs font-bold shadow-lg shadow-sky-500/25 flex items-center gap-1.5 transition-all cursor-pointer"
              >
                <Sparkles className="h-3.5 w-3.5" />
                Open Morning AI Briefing
              </button>
              <button
                id="home-view-readiness-btn"
                onClick={onOpenReadiness}
                className="px-3.5 py-2 rounded-xl bg-white/[0.08] hover:bg-white/[0.14] text-slate-200 text-xs font-semibold border border-white/10 flex items-center gap-1.5 transition-colors cursor-pointer"
              >
                <Award className="h-3.5 w-3.5 text-indigo-400" />
                Exam Readiness: {activeExam.readinessScore}%
              </button>
            </div>
          </div>

          {/* Readiness Dial Card */}
          <div className="flex md:flex-col items-center justify-between sm:justify-center p-4 rounded-2xl bg-white/[0.04] border border-white/10 text-center shrink-0 min-w-[160px]">
            <div className="relative flex items-center justify-center">
              <div className="h-20 w-20 rounded-full border-4 border-slate-700/60 flex items-center justify-center relative">
                <div
                  className="absolute inset-0 rounded-full border-4 border-sky-400 border-t-transparent animate-spin-slow"
                  style={{ transform: `rotate(${activeExam.readinessScore * 3.6}deg)` }}
                />
                <span className="text-xl font-extrabold text-white">
                  {activeExam.readinessScore}%
                </span>
              </div>
            </div>
            <div className="text-left md:text-center mt-2">
              <div className="text-xs font-bold text-sky-300">Exam Readiness</div>
              <div className="text-[11px] text-slate-400">High-Yield Index</div>
            </div>
          </div>
        </div>
      </section>

      {/* 2. NOVA 2.0 AI Coach Recommendation */}
      <section className="p-4 sm:p-5 rounded-2xl glass-card border border-sky-500/30 bg-gradient-to-r from-sky-950/40 via-indigo-950/30 to-slate-900/50 relative overflow-hidden">
        <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div className="flex items-start gap-3.5">
            <div className="h-11 w-11 rounded-2xl bg-gradient-to-tr from-sky-400 to-indigo-600 flex items-center justify-center text-white shrink-0 shadow-lg shadow-sky-500/20">
              <Sparkles className="h-6 w-6 animate-pulse" />
            </div>
            <div>
              <div className="flex items-center gap-2 mb-0.5">
                <span className="text-xs font-bold text-sky-400 uppercase tracking-wider">
                  Nova AI Recommendation
                </span>
                <span className="text-[10px] px-1.5 py-0.2 rounded bg-amber-500/20 text-amber-300 font-semibold">
                  Weak Topic Remediation
                </span>
              </div>
              <h3 className="text-sm sm:text-base font-bold text-white">
                Revise {weakTopic} (25 Mins)
              </h3>
              <p className="text-xs text-slate-300 mt-0.5 max-w-xl">
                Diagnostic reports show 2 recent incorrect mock answers on Article 14 vs 21. A 25-min active recall sprint will boost your accuracy by +8%.
              </p>
            </div>
          </div>

          <div className="flex items-center gap-2 w-full sm:w-auto">
            <button
              id="home-launch-ai-sprint-btn"
              onClick={() => onStartFocusSprint(25, 'Polity & Governance', 'Article 14 vs 21 & Basic Structure')}
              className="flex-1 sm:flex-none px-4 py-2.5 rounded-xl bg-gradient-to-r from-sky-400 to-sky-500 hover:from-sky-300 hover:to-sky-400 text-slate-950 font-bold text-xs shadow-lg shadow-sky-400/20 flex items-center justify-center gap-1.5 cursor-pointer"
            >
              <Play className="h-3.5 w-3.5 fill-slate-950" />
              Launch Focus Sprint (25m)
            </button>
            <button
              id="home-ask-nova-weak-btn"
              onClick={() => onOpenNovaWithPrompt(`Polity me Article 14 vs 21 conflict landmark cases samjhao.`)}
              className="px-3 py-2.5 rounded-xl bg-white/[0.08] hover:bg-white/[0.14] text-sky-300 text-xs font-semibold border border-white/10 cursor-pointer"
              title="Ask Nova to explain this concept"
            >
              Ask Nova
            </button>
          </div>
        </div>
      </section>

      {/* 3. Daily Goals & High-Impact Bento Grid */}
      <section className="grid grid-cols-1 md:grid-cols-3 gap-4">
        {/* Daily Study Progress Meter */}
        <div className="p-4 sm:p-5 rounded-2xl glass-card border border-white/10 flex flex-col justify-between">
          <div className="flex items-center justify-between mb-3">
            <div className="flex items-center gap-2">
              <Clock className="h-4 w-4 text-sky-400" />
              <span className="text-xs font-bold text-slate-200">Daily Study Target</span>
            </div>
            <span className="text-xs font-extrabold text-sky-300">
              {user.todayFocusedMinutes} / {user.studyGoalMinutesPerDay} mins
            </span>
          </div>

          <div className="w-full h-2.5 rounded-full bg-slate-800 overflow-hidden mb-3">
            <div
              className="h-full bg-gradient-to-r from-sky-400 to-indigo-500 rounded-full transition-all duration-500"
              style={{
                width: `${Math.min(100, Math.round((user.todayFocusedMinutes / user.studyGoalMinutesPerDay) * 100))}%`
              }}
            />
          </div>

          <div className="flex items-center justify-between text-[11px] text-slate-400">
            <span>{Math.round((user.todayFocusedMinutes / user.studyGoalMinutesPerDay) * 100)}% completed</span>
            <span>🔥 {user.streakDays} Day Streak</span>
          </div>
        </div>

        {/* Spaced Flashcards Queue */}
        <div
          onClick={() => onNavigateTab('study')}
          className="p-4 sm:p-5 rounded-2xl glass-card glass-card-hover border border-white/10 cursor-pointer flex flex-col justify-between"
        >
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <Layers className="h-4 w-4 text-emerald-400" />
              <span className="text-xs font-bold text-slate-200">Active Flashcards Queue</span>
            </div>
            <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-bold">
              12 Due Today
            </span>
          </div>

          <p className="text-xs text-slate-300">
            Review spaced repetition deck on High-Yield Formulas and Landmark SC Verdicts.
          </p>

          <div className="mt-3 flex items-center justify-between text-xs text-emerald-400 font-semibold">
            <span>Start 10-Min Flashcard Review</span>
            <ChevronRight className="h-4 w-4" />
          </div>
        </div>

        {/* Full Mock Test Launcher */}
        <div
          onClick={() => onNavigateTab('practice')}
          className="p-4 sm:p-5 rounded-2xl glass-card glass-card-hover border border-white/10 cursor-pointer flex flex-col justify-between"
        >
          <div className="flex items-center justify-between mb-2">
            <div className="flex items-center gap-2">
              <Target className="h-4 w-4 text-purple-400" />
              <span className="text-xs font-bold text-slate-200">Practice Hub & PYQs</span>
            </div>
            <span className="text-[10px] px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 font-bold">
              Live Mock Test
            </span>
          </div>

          <p className="text-xs text-slate-300">
            {activeExam.shortName} Full Prelims Simulation with authentic timer & AI error diagnosis.
          </p>

          <div className="mt-3 flex items-center justify-between text-xs text-purple-400 font-semibold">
            <span>Take Standard Mock Test</span>
            <ChevronRight className="h-4 w-4" />
          </div>
        </div>
      </section>

      {/* 4. Core Capabilities Quick Bento Actions */}
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-base sm:text-lg font-bold text-white tracking-tight">
            StudyMate AI Power Tools
          </h2>
          <span className="text-xs text-slate-400">Everything you need to conquer your exam</span>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {/* Tool 1: Nova AI Tutor */}
          <button
            onClick={() => onNavigateTab('nova')}
            className="p-3.5 rounded-2xl glass-card glass-card-hover text-left flex flex-col justify-between gap-3 border border-white/10 cursor-pointer"
          >
            <div className="h-9 w-9 rounded-xl bg-sky-500/20 text-sky-400 flex items-center justify-center border border-sky-500/30">
              <Sparkles className="h-5 w-5" />
            </div>
            <div>
              <div className="text-xs sm:text-sm font-bold text-white">Nova AI Tutor</div>
              <div className="text-[11px] text-slate-400">24/7 Socratic Mentor & Doubt Solver</div>
            </div>
          </button>

          {/* Tool 2: Study Plan & Syllabus */}
          <button
            onClick={() => onNavigateTab('study')}
            className="p-3.5 rounded-2xl glass-card glass-card-hover text-left flex flex-col justify-between gap-3 border border-white/10 cursor-pointer"
          >
            <div className="h-9 w-9 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center border border-emerald-500/30">
              <BookOpen className="h-5 w-5" />
            </div>
            <div>
              <div className="text-xs sm:text-sm font-bold text-white">Syllabus & Plan</div>
              <div className="text-[11px] text-slate-400">Topic Tracking & Daily Schedule</div>
            </div>
          </button>

          {/* Tool 3: Smart Focus Shield */}
          <button
            onClick={() => onNavigateTab('focus')}
            className="p-3.5 rounded-2xl glass-card glass-card-hover text-left flex flex-col justify-between gap-3 border border-white/10 cursor-pointer"
          >
            <div className="h-9 w-9 rounded-xl bg-indigo-500/20 text-indigo-400 flex items-center justify-center border border-indigo-500/30">
              <Shield className="h-5 w-5" />
            </div>
            <div>
              <div className="text-xs sm:text-sm font-bold text-white">Focus Shield</div>
              <div className="text-[11px] text-slate-400">Pomodoro, Ambient Waves & Blocker</div>
            </div>
          </button>

          {/* Tool 4: Notes & PDF Summarizer */}
          <button
            onClick={onOpenSummarizer}
            className="p-3.5 rounded-2xl glass-card glass-card-hover text-left flex flex-col justify-between gap-3 border border-white/10 cursor-pointer"
          >
            <div className="h-9 w-9 rounded-xl bg-amber-500/20 text-amber-400 flex items-center justify-center border border-amber-500/30">
              <FileText className="h-5 w-5" />
            </div>
            <div>
              <div className="text-xs sm:text-sm font-bold text-white">Notes Summarizer</div>
              <div className="text-[11px] text-slate-400">Extract Flashcards & Mind Maps</div>
            </div>
          </button>
        </div>
      </section>

      {/* 5. Subject Mastery & High-Yield Syllabus Breakdown */}
      <section className="p-5 rounded-3xl glass-panel border border-white/10 space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-base font-bold text-white">Subject Mastery Progress</h2>
            <p className="text-xs text-slate-400">Mapped to {activeExam.shortName} official syllabus</p>
          </div>
          <button
            onClick={() => onNavigateTab('study')}
            className="text-xs font-semibold text-sky-400 hover:text-sky-300 flex items-center gap-1 cursor-pointer"
          >
            View Full Syllabus
            <ArrowRight className="h-3.5 w-3.5" />
          </button>
        </div>

        <div className="space-y-3">
          {subjects.map((sub) => (
            <div key={sub.id} className="p-3.5 rounded-2xl bg-white/[0.03] border border-white/10 space-y-2">
              <div className="flex items-center justify-between text-xs">
                <div className="flex items-center gap-2">
                  <span className="font-bold text-white">{sub.name}</span>
                  <span className="text-slate-400 text-[11px]">
                    ({sub.completedTopicsCount}/{sub.totalTopicsCount} topics)
                  </span>
                </div>
                <span className="font-bold text-sky-300">{sub.masteryPercentage}% Mastery</span>
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
      </section>

      {/* 6. Live Vacancies & Recruitment Updates Ticker */}
      <section className="p-5 rounded-3xl glass-panel border border-white/10 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <BellRing className="h-4 w-4 text-amber-400" />
            <h2 className="text-base font-bold text-white">Live Exam Updates & Notifications</h2>
          </div>
          <button
            onClick={() => onNavigateTab('updates')}
            className="text-xs font-semibold text-sky-400 hover:text-sky-300 flex items-center gap-1 cursor-pointer"
          >
            View All Updates
            <ArrowRight className="h-3.5 w-3.5" />
          </button>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          {vacancies.slice(0, 2).map((vac) => (
            <div
              key={vac.id}
              onClick={() => onNavigateTab('updates')}
              className="p-3.5 rounded-2xl bg-white/[0.03] hover:bg-white/[0.07] border border-white/10 flex flex-col justify-between gap-2 cursor-pointer transition-colors"
            >
              <div>
                <div className="flex items-center justify-between gap-2 mb-1">
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30">
                    {vac.category}
                  </span>
                  <span className="text-[11px] text-slate-400">Deadline: {vac.lastDateToApply}</span>
                </div>
                <h4 className="text-xs font-bold text-white truncate">{vac.title}</h4>
                <p className="text-[11px] text-slate-400 line-clamp-1">{vac.organization}</p>
              </div>

              <div className="flex items-center justify-between text-[11px] pt-2 border-t border-white/5 text-slate-300">
                <span>Posts: <strong className="text-white">{vac.totalPosts}</strong></span>
                <span className="text-sky-400 font-semibold">View Details →</span>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
};
