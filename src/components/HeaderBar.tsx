import React from 'react';
import { UserProfile, ExamInfo } from '../types';
import { Flame, Bell, Sparkles, ChevronDown, Award, Clock } from 'lucide-react';

interface HeaderBarProps {
  user: UserProfile;
  activeExam: ExamInfo;
  allExams: ExamInfo[];
  unreadNotifsCount: number;
  onSelectExam: (exam: ExamInfo) => void;
  onOpenNotifications: () => void;
  onOpenProfile: () => void;
  onOpenReadiness: () => void;
}

export const HeaderBar: React.FC<HeaderBarProps> = ({
  user,
  activeExam,
  allExams,
  unreadNotifsCount,
  onSelectExam,
  onOpenNotifications,
  onOpenProfile,
  onOpenReadiness,
}) => {
  const [showExamDropdown, setShowExamDropdown] = React.useState(false);

  return (
    <header className="sticky top-0 z-40 w-full glass-panel border-b border-white/10 px-4 py-2.5 sm:px-6">
      <div className="max-w-7xl mx-auto flex items-center justify-between gap-3">
        {/* Left: App Logo & Exam Badge Switcher */}
        <div className="flex items-center gap-3">
          <div className="flex items-center gap-2">
            <div className="h-9 w-9 rounded-xl bg-gradient-to-br from-sky-400 via-sky-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-sky-500/20">
              <Sparkles className="h-5 w-5 text-white" />
            </div>
            <div className="hidden sm:block">
              <span className="text-base font-bold tracking-tight text-white flex items-center gap-1.5">
                StudyMate <span className="text-xs px-1.5 py-0.5 rounded-full bg-sky-500/20 text-sky-400 font-semibold border border-sky-500/30">AI</span>
              </span>
            </div>
          </div>

          {/* Exam Selector Dropdown */}
          <div className="relative">
            <button
              id="header-exam-selector-btn"
              onClick={() => setShowExamDropdown(!showExamDropdown)}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-white/[0.07] hover:bg-white/[0.12] border border-white/10 text-xs sm:text-sm font-medium text-slate-200 transition-all cursor-pointer"
            >
              <span className="h-2 w-2 rounded-full bg-emerald-400 animate-pulse"></span>
              <span className="max-w-[130px] sm:max-w-[180px] truncate font-semibold text-sky-300">
                {activeExam.shortName}
              </span>
              <span className="text-[11px] text-slate-400 hidden md:inline">
                ({activeExam.daysRemaining}d left)
              </span>
              <ChevronDown className="h-3.5 w-3.5 text-slate-400" />
            </button>

            {showExamDropdown && (
              <>
                <div
                  className="fixed inset-0 z-40"
                  onClick={() => setShowExamDropdown(false)}
                />
                <div className="absolute left-0 mt-2 w-72 rounded-2xl glass-panel shadow-2xl z-50 p-2 border border-white/15 animate-in fade-in zoom-in-95 duration-150">
                  <div className="px-3 py-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">
                    Select Target Examination
                  </div>
                  <div className="space-y-1">
                    {allExams.map((exam) => (
                      <button
                        key={exam.id}
                        id={`select-exam-${exam.id}`}
                        onClick={() => {
                          onSelectExam(exam);
                          setShowExamDropdown(false);
                        }}
                        className={`w-full text-left px-3 py-2 rounded-xl text-xs sm:text-sm flex items-center justify-between transition-colors cursor-pointer ${
                          exam.id === activeExam.id
                            ? 'bg-sky-500/20 text-sky-300 font-semibold border border-sky-500/30'
                            : 'text-slate-300 hover:bg-white/10'
                        }`}
                      >
                        <div className="truncate mr-2">
                          <div className="font-medium truncate">{exam.name}</div>
                          <div className="text-[11px] text-slate-400">{exam.examDate}</div>
                        </div>
                        <span className="text-[11px] px-2 py-0.5 rounded-md bg-white/10 text-slate-300 whitespace-nowrap">
                          {exam.daysRemaining}d
                        </span>
                      </button>
                    ))}
                  </div>
                </div>
              </>
            )}
          </div>
        </div>

        {/* Right: Streak, Daily Focus, Readiness & Notifs */}
        <div className="flex items-center gap-2 sm:gap-3">
          {/* Streak Counter */}
          <div
            id="header-streak-badge"
            className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-300 text-xs sm:text-sm font-bold"
            title={`${user.streakDays} Day Study Streak`}
          >
            <Flame className="h-4 w-4 text-amber-400 fill-amber-400 animate-bounce" />
            <span>{user.streakDays}</span>
            <span className="text-[11px] font-normal text-amber-200/70 hidden sm:inline">Days</span>
          </div>

          {/* Daily Focus Goal */}
          <div
            id="header-daily-focus-badge"
            className="hidden sm:flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-sky-500/10 border border-sky-500/20 text-sky-300 text-xs font-semibold"
          >
            <Clock className="h-3.5 w-3.5 text-sky-400" />
            <span>{user.todayFocusedMinutes}/{user.studyGoalMinutesPerDay}m</span>
          </div>

          {/* Readiness Score button */}
          <button
            id="header-readiness-btn"
            onClick={onOpenReadiness}
            className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-xl bg-indigo-500/10 hover:bg-indigo-500/20 border border-indigo-500/20 text-indigo-300 text-xs font-bold transition-colors cursor-pointer"
            title="View Exam Readiness Score"
          >
            <Award className="h-4 w-4 text-indigo-400" />
            <span className="hidden md:inline font-normal text-indigo-200/80">Readiness:</span>
            <span>{activeExam.readinessScore}%</span>
          </button>

          {/* Notifications Button */}
          <button
            id="header-notifs-btn"
            onClick={onOpenNotifications}
            className="relative p-2 rounded-xl bg-white/[0.06] hover:bg-white/[0.12] border border-white/10 text-slate-300 transition-colors cursor-pointer"
            title="Notifications"
          >
            <Bell className="h-4 w-4" />
            {unreadNotifsCount > 0 && (
              <span className="absolute -top-1 -right-1 h-4 w-4 rounded-full bg-rose-500 text-white text-[10px] font-bold flex items-center justify-center ring-2 ring-[#070B19]">
                {unreadNotifsCount}
              </span>
            )}
          </button>

          {/* User Profile Avatar */}
          <button
            id="header-profile-avatar-btn"
            onClick={onOpenProfile}
            className="h-8 w-8 rounded-xl bg-gradient-to-tr from-sky-400 to-indigo-600 p-[1.5px] cursor-pointer hover:ring-2 hover:ring-sky-400/50 transition-all"
            title="User Profile & Settings"
          >
            <div className="h-full w-full rounded-[10px] bg-slate-900 flex items-center justify-center text-xs font-bold text-sky-300">
              {user.name.charAt(0).toUpperCase()}
            </div>
          </button>
        </div>
      </div>
    </header>
  );
};
