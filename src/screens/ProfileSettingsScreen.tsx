import React, { useState } from 'react';
import { UserProfile, ExamInfo } from '../types';
import { User, Settings, Globe, Clock, Award, Shield, RotateCcw, Check, Sparkles } from 'lucide-react';

interface ProfileSettingsScreenProps {
  user: UserProfile;
  activeExam: ExamInfo;
  allExams: ExamInfo[];
  onUpdateUser: (updated: Partial<UserProfile>) => void;
  onSelectExam: (exam: ExamInfo) => void;
  onResetData: () => void;
  onBack: () => void;
  onOpenPrivacy?: () => void;
}

export const ProfileSettingsScreen: React.FC<ProfileSettingsScreenProps> = ({
  user,
  activeExam,
  allExams,
  onUpdateUser,
  onSelectExam,
  onResetData,
  onBack,
  onOpenPrivacy,
}) => {
  const [name, setName] = useState(user.name);
  const [goalMinutes, setGoalMinutes] = useState(user.studyGoalMinutesPerDay);
  const [preferredLang, setPreferredLang] = useState<'English' | 'Hindi' | 'Hinglish'>(
    user.preferredLanguage || 'Hinglish'
  );
  const [savedSuccess, setSavedSuccess] = useState(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    onUpdateUser({
      name,
      studyGoalMinutesPerDay: goalMinutes,
      preferredLanguage: preferredLang,
    });
    setSavedSuccess(true);
    setTimeout(() => setSavedSuccess(false), 2000);
  };

  return (
    <div className="max-w-2xl mx-auto space-y-6 pb-24 animate-in fade-in duration-300">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-extrabold text-white tracking-tight flex items-center gap-2">
            <Settings className="h-6 w-6 text-sky-400" />
            Profile & Aspirant Settings
          </h1>
          <p className="text-xs text-slate-400">
            Customize target exams, daily targets, and AI tutor dialect
          </p>
        </div>
        <button
          onClick={onBack}
          className="px-3.5 py-1.5 rounded-xl bg-white/[0.08] hover:bg-white/[0.14] text-xs font-semibold text-slate-300 cursor-pointer"
        >
          Back
        </button>
      </div>

      <form onSubmit={handleSave} className="space-y-5">
        {/* User Card */}
        <div className="p-6 rounded-3xl glass-panel border border-white/10 space-y-4">
          <div className="flex items-center gap-4">
            <div className="h-16 w-16 rounded-2xl bg-gradient-to-tr from-sky-400 to-indigo-600 flex items-center justify-center text-2xl font-extrabold text-slate-950 shadow-lg shadow-sky-500/20">
              {name.charAt(0).toUpperCase()}
            </div>
            <div>
              <div className="text-base font-bold text-white">{name}</div>
              <p className="text-xs text-slate-400">Preparing for {activeExam.shortName}</p>
            </div>
          </div>

          <div>
            <label className="text-xs font-bold text-slate-300 block mb-1">Aspirant Name</label>
            <input
              type="text"
              value={name}
              onChange={(e) => setName(e.target.value)}
              className="w-full px-4 py-2.5 rounded-xl bg-slate-950 border border-white/15 text-xs sm:text-sm text-white outline-none focus:border-sky-400"
            />
          </div>
        </div>

        {/* Target Exam Selection */}
        <div className="p-6 rounded-3xl glass-panel border border-white/10 space-y-3">
          <label className="text-xs font-bold text-slate-300 uppercase tracking-wider block">
            Target Examination
          </label>
          <div className="space-y-2">
            {allExams.map((exam) => (
              <div
                key={exam.id}
                onClick={() => onSelectExam(exam)}
                className={`p-3.5 rounded-2xl border transition-all flex items-center justify-between cursor-pointer ${
                  exam.id === activeExam.id
                    ? 'bg-sky-500/20 border-sky-400 text-sky-300 font-bold'
                    : 'glass-card border-white/5 text-slate-300 hover:bg-white/[0.06]'
                }`}
              >
                <div>
                  <div className="text-xs font-bold text-white">{exam.name}</div>
                  <div className="text-[11px] text-slate-400">
                    Exam Date: {exam.examDate} ({exam.daysRemaining} days left)
                  </div>
                </div>
                {exam.id === activeExam.id && <Check className="h-4 w-4 text-sky-400" />}
              </div>
            ))}
          </div>
        </div>

        {/* Daily Study Goal */}
        <div className="p-6 rounded-3xl glass-panel border border-white/10 space-y-3">
          <div className="flex items-center justify-between">
            <label className="text-xs font-bold text-slate-300 uppercase tracking-wider">
              Daily Focus Target
            </label>
            <span className="text-sm font-bold text-sky-400">{goalMinutes} Minutes / Day</span>
          </div>

          <input
            type="range"
            min={30}
            max={360}
            step={15}
            value={goalMinutes}
            onChange={(e) => setGoalMinutes(Number(e.target.value))}
            className="w-full accent-sky-400 cursor-pointer"
          />

          <div className="flex justify-between text-[10px] text-slate-500">
            <span>30 mins</span>
            <span>2 hours</span>
            <span>4 hours</span>
            <span>6 hours</span>
          </div>
        </div>

        {/* AI Tutor Language Preference */}
        <div className="p-6 rounded-3xl glass-panel border border-white/10 space-y-3">
          <label className="text-xs font-bold text-slate-300 uppercase tracking-wider block">
            AI Tutor Language Dialect
          </label>
          <div className="grid grid-cols-3 gap-2">
            {(['English', 'Hindi', 'Hinglish'] as const).map((lang) => (
              <button
                type="button"
                key={lang}
                onClick={() => setPreferredLang(lang)}
                className={`py-2.5 rounded-xl text-xs font-bold transition-all border cursor-pointer ${
                  preferredLang === lang
                    ? 'bg-sky-500/20 text-sky-300 border-sky-400'
                    : 'bg-white/[0.04] text-slate-400 border-white/5 hover:text-white'
                }`}
              >
                {lang}
              </button>
            ))}
          </div>
        </div>

        {/* Play Store Compliance & Privacy */}
        {onOpenPrivacy && (
          <div className="p-5 rounded-3xl glass-panel border border-sky-500/20 flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 rounded-xl bg-sky-500/20 text-sky-400">
                <Shield className="w-5 h-5" />
              </div>
              <div>
                <div className="text-xs font-bold text-white">Google Play Store & Data Privacy</div>
                <div className="text-[11px] text-slate-400">Review terms, encrypted local storage & permissions</div>
              </div>
            </div>
            <button
              type="button"
              onClick={onOpenPrivacy}
              className="px-3 py-1.5 rounded-xl bg-sky-500/10 hover:bg-sky-500/20 text-sky-400 border border-sky-500/30 text-xs font-bold transition-all cursor-pointer"
            >
              View Terms
            </button>
          </div>
        )}

        {/* Save Button */}
        <button
          type="submit"
          className="w-full py-3 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 text-slate-950 font-bold text-xs sm:text-sm shadow-xl shadow-sky-500/25 flex items-center justify-center gap-2 cursor-pointer"
        >
          {savedSuccess ? (
            <>
              <Check className="h-4 w-4" />
              Settings Saved Successfully!
            </>
          ) : (
            'Save Preferences'
          )}
        </button>

        {/* Reset / Restore Demo Data */}
        <div className="pt-4 text-center">
          <button
            type="button"
            onClick={onResetData}
            className="text-xs text-rose-400 hover:text-rose-300 flex items-center justify-center gap-1 mx-auto cursor-pointer"
          >
            <RotateCcw className="h-3 w-3" />
            Reset all local progress & restore sample data
          </button>
        </div>
      </form>
    </div>
  );
};
