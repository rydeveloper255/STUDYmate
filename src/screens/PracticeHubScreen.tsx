import React, { useState } from 'react';
import { ExamInfo, Question, MockTestAttempt } from '../types';
import { generateQuizQuestions } from '../services/api';
import {
  Target,
  Clock,
  Sparkles,
  Award,
  BookOpen,
  Play,
  RotateCcw,
  CheckCircle2,
  AlertCircle,
  TrendingUp,
  History,
  FileCheck,
  ChevronRight
} from 'lucide-react';

interface PracticeHubScreenProps {
  activeExam: ExamInfo;
  pyqQuestions: Question[];
  testAttempts: MockTestAttempt[];
  onStartCustomTest: (title: string, subject: string, questions: Question[]) => void;
  onViewDiagnosticReport: (attempt: MockTestAttempt) => void;
}

export const PracticeHubScreen: React.FC<PracticeHubScreenProps> = ({
  activeExam,
  pyqQuestions,
  testAttempts,
  onStartCustomTest,
  onViewDiagnosticReport,
}) => {
  const [activeTab, setActiveTab] = useState<'mock_tests' | 'ai_generator' | 'attempts'>('mock_tests');

  // AI Generator Form State
  const [genSubject, setGenSubject] = useState(activeExam.subjects[0] || 'General Studies');
  const [genTopic, setGenTopic] = useState('High-Yield Concept Review');
  const [genDifficulty, setGenDifficulty] = useState<'Easy' | 'Medium' | 'Hard'>('Medium');
  const [genCount, setGenCount] = useState(5);
  const [isGenerating, setIsGenerating] = useState(false);

  const handleGenerateAndStart = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsGenerating(true);
    try {
      const res = await generateQuizQuestions(
        genSubject,
        genTopic,
        genDifficulty,
        genCount,
        activeExam.name,
        'English',
        'Practice'
      );
      if (res.questions && res.questions.length > 0) {
        onStartCustomTest(`Speed Quiz: ${genSubject}`, genSubject, res.questions);
      }
    } catch (err) {
      // Fallback
      onStartCustomTest(`Practice Test: ${genSubject}`, genSubject, pyqQuestions);
    } finally {
      setIsGenerating(false);
    }
  };

  const standardTests = [
    {
      id: 'std_full_1',
      title: `${activeExam.shortName} Full Prelims Mock 1`,
      questionsCount: 15,
      durationMinutes: 30,
      difficulty: 'Hard',
      syllabusCoverage: 'Full Exam Syllabus',
      tag: 'Official Standard'
    },
    {
      id: 'std_pyq_1',
      title: `${activeExam.shortName} Previous Year Questions (PYQ) Mastery`,
      questionsCount: 10,
      durationMinutes: 20,
      difficulty: 'Medium',
      syllabusCoverage: 'Verified Official PYQs',
      tag: 'PYQ Series'
    },
    {
      id: 'std_speed_1',
      title: 'Current Affairs & High-Yield Speed Sprint',
      questionsCount: 10,
      durationMinutes: 15,
      difficulty: 'Medium',
      syllabusCoverage: '2024 - 2025 National & International',
      tag: 'Speed Drill'
    }
  ];

  return (
    <div className="space-y-6 pb-24 animate-in fade-in duration-300">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-extrabold text-white tracking-tight flex items-center gap-2">
            <Target className="h-6 w-6 text-purple-400" />
            Practice Hub & Mock Test Engine
          </h1>
          <p className="text-xs text-slate-400">
            Full standard mock tests, authentic PYQs, and AI-generated speed quizzes
          </p>
        </div>

        {/* Tab Switcher */}
        <div className="flex items-center gap-1.5 p-1 bg-white/[0.05] rounded-2xl border border-white/10 shrink-0">
          <button
            onClick={() => setActiveTab('mock_tests')}
            className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              activeTab === 'mock_tests'
                ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Mock Tests & PYQs
          </button>
          <button
            onClick={() => setActiveTab('ai_generator')}
            className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'ai_generator'
                ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Sparkles className="h-3.5 w-3.5 text-purple-400" />
            AI Generator
          </button>
          <button
            onClick={() => setActiveTab('attempts')}
            className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'attempts'
                ? 'bg-purple-500/20 text-purple-300 border border-purple-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <History className="h-3.5 w-3.5" />
            History ({testAttempts.length})
          </button>
        </div>
      </div>

      {/* 1. STANDARD MOCK TESTS & PYQS */}
      {activeTab === 'mock_tests' && (
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {standardTests.map((test) => (
              <div
                key={test.id}
                className="p-5 rounded-3xl glass-card border border-white/10 flex flex-col justify-between gap-4 hover:border-purple-500/30 transition-all"
              >
                <div>
                  <div className="flex items-center justify-between gap-2 mb-2">
                    <span className="text-[10px] px-2.5 py-0.5 rounded-full bg-purple-500/20 text-purple-300 font-bold border border-purple-500/30">
                      {test.tag}
                    </span>
                    <span className="text-xs text-slate-400">{test.difficulty}</span>
                  </div>
                  <h3 className="text-sm font-bold text-white mb-1">{test.title}</h3>
                  <p className="text-xs text-slate-400">{test.syllabusCoverage}</p>
                </div>

                <div className="space-y-3 pt-3 border-t border-white/10">
                  <div className="flex items-center justify-between text-xs text-slate-300">
                    <span className="flex items-center gap-1">
                      <FileCheck className="h-3.5 w-3.5 text-purple-400" />
                      {test.questionsCount} Questions
                    </span>
                    <span className="flex items-center gap-1">
                      <Clock className="h-3.5 w-3.5 text-purple-400" />
                      {test.durationMinutes} Minutes
                    </span>
                  </div>

                  <button
                    onClick={() => onStartCustomTest(test.title, activeExam.subjects[0] || 'General Studies', pyqQuestions)}
                    className="w-full py-2.5 rounded-xl bg-gradient-to-r from-purple-500 to-indigo-600 hover:from-purple-400 hover:to-indigo-500 text-white text-xs font-bold shadow-lg shadow-purple-500/20 flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <Play className="h-3.5 w-3.5 fill-white" />
                    Start Live Mock Simulation
                  </button>
                </div>
              </div>
            ))}
          </div>

          {/* Authentic PYQ Question Showcase */}
          <div className="p-5 rounded-3xl glass-panel border border-white/10 space-y-3">
            <div className="flex items-center justify-between">
              <div>
                <h3 className="text-base font-bold text-white flex items-center gap-2">
                  <Award className="h-4 w-4 text-amber-400" />
                  Verified Previous Year Questions (PYQs)
                </h3>
                <p className="text-xs text-slate-400">
                  Real questions analyzed with official answer keys & detailed explanations
                </p>
              </div>
            </div>

            <div className="space-y-3">
              {pyqQuestions.slice(0, 3).map((q, idx) => (
                <div key={q.id} className="p-4 rounded-2xl bg-white/[0.03] border border-white/10 space-y-2">
                  <div className="flex items-center justify-between text-xs">
                    <span className="px-2 py-0.5 rounded bg-sky-500/20 text-sky-300 font-semibold">
                      {q.sourceLabel || q.yearOrTag}
                    </span>
                    <span className="text-slate-400 font-medium">{q.subject}</span>
                  </div>
                  <p className="text-xs sm:text-sm font-semibold text-white whitespace-pre-line">
                    {q.questionText}
                  </p>
                  <div className="text-[11px] text-emerald-400 font-semibold pt-1">
                    ✓ Correct Answer: Option {String.fromCharCode(65 + q.correctOptionIndex)} ({q.options[q.correctOptionIndex]})
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* 2. AI SPEED QUIZ GENERATOR */}
      {activeTab === 'ai_generator' && (
        <div className="max-w-xl mx-auto p-6 rounded-3xl glass-panel border border-purple-500/30 shadow-2xl space-y-5">
          <div>
            <h2 className="text-lg font-bold text-white flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-purple-400" />
              Generate Targeted AI Quiz
            </h2>
            <p className="text-xs text-slate-400">
              Gemini AI crafts custom questions specifically tailored to your syllabus & weak points.
            </p>
          </div>

          <form onSubmit={handleGenerateAndStart} className="space-y-4">
            <div>
              <label className="text-xs font-bold text-slate-300 block mb-1">Subject</label>
              <select
                value={genSubject}
                onChange={(e) => setGenSubject(e.target.value)}
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-white/15 text-xs text-white outline-none"
              >
                {activeExam.subjects.map((sub, i) => (
                  <option key={i} value={sub}>
                    {sub}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="text-xs font-bold text-slate-300 block mb-1">Topic / Focus</label>
              <input
                type="text"
                value={genTopic}
                onChange={(e) => setGenTopic(e.target.value)}
                placeholder="e.g., Inflation Indices, Article 32 Writs, or Mechanics"
                className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-white/15 text-xs text-white outline-none"
                required
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="text-xs font-bold text-slate-300 block mb-1">Difficulty</label>
                <select
                  value={genDifficulty}
                  onChange={(e) => setGenDifficulty(e.target.value as any)}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-white/15 text-xs text-white outline-none"
                >
                  <option value="Easy">Easy (Conceptual)</option>
                  <option value="Medium">Medium (Standard Exam Level)</option>
                  <option value="Hard">Hard (Landmark / Tricky)</option>
                </select>
              </div>

              <div>
                <label className="text-xs font-bold text-slate-300 block mb-1">
                  Questions Count
                </label>
                <select
                  value={genCount}
                  onChange={(e) => setGenCount(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-white/15 text-xs text-white outline-none"
                >
                  <option value={5}>5 Questions (Speed Drill)</option>
                  <option value={10}>10 Questions (Standard Sprint)</option>
                  <option value={15}>15 Questions (Full Checkup)</option>
                </select>
              </div>
            </div>

            <button
              type="submit"
              disabled={isGenerating}
              className="w-full py-3 rounded-xl bg-gradient-to-r from-purple-500 via-indigo-500 to-sky-500 hover:from-purple-400 hover:to-sky-400 text-white font-bold text-xs sm:text-sm shadow-xl shadow-purple-500/25 flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
            >
              {isGenerating ? (
                <>
                  <Sparkles className="h-4 w-4 animate-spin" />
                  Generating Questions with Gemini AI...
                </>
              ) : (
                <>
                  <Play className="h-4 w-4 fill-white" />
                  Generate & Start Live Quiz
                </>
              )}
            </button>
          </form>
        </div>
      )}

      {/* 3. ATTEMPT HISTORY & AI ERROR DIAGNOSTICS */}
      {activeTab === 'attempts' && (
        <div className="space-y-4">
          <div className="p-4 rounded-2xl glass-panel border border-white/10">
            <h2 className="text-base font-bold text-white flex items-center gap-2">
              <History className="h-4 w-4 text-purple-400" />
              Mock Test Attempt Logs & AI Diagnostic Reports
            </h2>
            <p className="text-xs text-slate-400">
              Track past scores, time-per-question metrics, and review flagged mistakes.
            </p>
          </div>

          {testAttempts.length === 0 ? (
            <div className="p-8 text-center rounded-3xl glass-card border border-white/10 space-y-3">
              <AlertCircle className="h-8 w-8 text-slate-500 mx-auto" />
              <div className="text-sm font-bold text-slate-300">No Mock Attempts Recorded Yet</div>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                Complete your first practice drill or standard mock test to generate AI error diagnosis.
              </p>
            </div>
          ) : (
            <div className="space-y-3">
              {testAttempts.map((att) => (
                <div
                  key={att.id}
                  className="p-4 rounded-2xl glass-card border border-white/10 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:border-purple-500/30 transition-all"
                >
                  <div className="space-y-1">
                    <div className="flex items-center gap-2">
                      <h4 className="text-sm font-bold text-white">{att.testTitle}</h4>
                      <span className="text-[10px] px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 font-bold">
                        {att.subject}
                      </span>
                    </div>
                    <div className="flex items-center gap-3 text-xs text-slate-400">
                      <span>Score: <strong className="text-white">{att.score}/{att.totalQuestions}</strong></span>
                      <span>•</span>
                      <span>Accuracy: <strong className="text-emerald-400">{att.accuracyPercent}%</strong></span>
                      <span>•</span>
                      <span>Time: {Math.floor(att.timeSpentSeconds / 60)}m {att.timeSpentSeconds % 60}s</span>
                    </div>
                  </div>

                  <button
                    onClick={() => onViewDiagnosticReport(att)}
                    className="px-4 py-2 rounded-xl bg-purple-500/20 hover:bg-purple-500/30 text-purple-300 text-xs font-bold border border-purple-500/30 flex items-center justify-center gap-1.5 cursor-pointer"
                  >
                    <Sparkles className="h-3.5 w-3.5" />
                    View AI Diagnostic Report
                  </button>
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
};
