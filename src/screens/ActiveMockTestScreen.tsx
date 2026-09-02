import React, { useState, useEffect } from 'react';
import { Question, MockTestAttempt } from '../types';
import { getTestDiagnostic } from '../services/api';
import confetti from 'canvas-confetti';
import ReactMarkdown from 'react-markdown';
import {
  Clock,
  CheckCircle2,
  XCircle,
  Flag,
  Award,
  Sparkles,
  ArrowRight,
  ArrowLeft,
  RotateCcw,
  Check,
  AlertTriangle,
  X
} from 'lucide-react';

interface ActiveMockTestScreenProps {
  testTitle: string;
  subject: string;
  examName: string;
  questions: Question[];
  onCompleteTest: (attempt: MockTestAttempt) => void;
  onExitTest: () => void;
}

export const ActiveMockTestScreen: React.FC<ActiveMockTestScreenProps> = ({
  testTitle,
  subject,
  examName,
  questions,
  onCompleteTest,
  onExitTest,
}) => {
  const [currentIndex, setCurrentIndex] = useState(0);
  const [userAnswers, setUserAnswers] = useState<{ [qId: string]: number }>({});
  const [markedForReview, setMarkedForReview] = useState<{ [qId: string]: boolean }>({});
  const [secondsRemaining, setSecondsRemaining] = useState(questions.length * 120); // 2 mins per question
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [diagnosticReport, setDiagnosticReport] = useState<string | null>(null);
  const [isGeneratingDiagnostic, setIsGeneratingDiagnostic] = useState(false);

  // Timer countdown
  useEffect(() => {
    if (isSubmitted) return;
    const interval = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          handleSubmitTest();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => clearInterval(interval);
  }, [isSubmitted]);

  const currentQ = questions[currentIndex] || questions[0];

  const handleSelectOption = (optIdx: number) => {
    if (isSubmitted) return;
    setUserAnswers((prev) => ({ ...prev, [currentQ.id]: optIdx }));
  };

  const toggleMarkForReview = () => {
    setMarkedForReview((prev) => ({ ...prev, [currentQ.id]: !prev[currentQ.id] }));
  };

  const handleSubmitTest = async () => {
    setIsSubmitted(true);

    let correctCount = 0;
    let incorrectCount = 0;
    const incorrectList: any[] = [];
    const weakTopics: string[] = [];
    const strongTopics: string[] = [];

    questions.forEach((q) => {
      const ans = userAnswers[q.id];
      if (ans !== undefined) {
        if (ans === q.correctOptionIndex) {
          correctCount++;
          if (!strongTopics.includes(q.topic)) strongTopics.push(q.topic);
        } else {
          incorrectCount++;
          if (!weakTopics.includes(q.topic)) weakTopics.push(q.topic);
          incorrectList.push({
            question: q.questionText,
            selected: q.options[ans],
            correct: q.options[q.correctOptionIndex],
            topic: q.topic,
          });
        }
      }
    });

    const unattemptedCount = questions.length - (correctCount + incorrectCount);
    const score = correctCount;
    const accuracy = questions.length > 0 ? Math.round((correctCount / questions.length) * 100) : 0;
    const timeSpent = questions.length * 120 - secondsRemaining;

    if (accuracy >= 70) {
      try {
        confetti({
          particleCount: 80,
          spread: 70,
          origin: { y: 0.6 },
        });
      } catch (e) {
        // ignore
      }
    }

    // Generate AI diagnostic automatically
    setIsGeneratingDiagnostic(true);
    try {
      const diagRes = await getTestDiagnostic(
        examName,
        subject,
        score,
        questions.length,
        accuracy,
        timeSpent,
        weakTopics,
        strongTopics,
        incorrectList
      );
      setDiagnosticReport(diagRes.diagnosticMarkdown);

      const attemptObj: MockTestAttempt = {
        id: `attempt_${Date.now()}`,
        testTitle,
        examName,
        subject,
        timestamp: Date.now(),
        totalQuestions: questions.length,
        correctCount,
        incorrectCount,
        unattemptedCount,
        score,
        accuracyPercent: accuracy,
        timeSpentSeconds: timeSpent,
        weakTopics,
        strongTopics,
        diagnosticReport: diagRes.diagnosticMarkdown,
        userAnswers,
        markedForReview: Object.keys(markedForReview).filter((k) => markedForReview[k]),
        questions,
      };
      onCompleteTest(attemptObj);
    } catch (e) {
      const attemptObj: MockTestAttempt = {
        id: `attempt_${Date.now()}`,
        testTitle,
        examName,
        subject,
        timestamp: Date.now(),
        totalQuestions: questions.length,
        correctCount,
        incorrectCount,
        unattemptedCount,
        score,
        accuracyPercent: accuracy,
        timeSpentSeconds: timeSpent,
        weakTopics,
        strongTopics,
        userAnswers,
        markedForReview: Object.keys(markedForReview).filter((k) => markedForReview[k]),
        questions,
      };
      onCompleteTest(attemptObj);
    } finally {
      setIsGeneratingDiagnostic(false);
    }
  };

  const minutes = Math.floor(secondsRemaining / 60);
  const seconds = secondsRemaining % 60;

  // -------------------------------------------------------------
  // TEST SUBMITTED: RESULT SCORECARD & QUESTION REVIEW
  // -------------------------------------------------------------
  if (isSubmitted) {
    let correctCount = 0;
    let incorrectCount = 0;
    questions.forEach((q) => {
      const ans = userAnswers[q.id];
      if (ans !== undefined) {
        if (ans === q.correctOptionIndex) correctCount++;
        else incorrectCount++;
      }
    });
    const accuracy = Math.round((correctCount / questions.length) * 100);

    return (
      <div className="max-w-4xl mx-auto space-y-6 pb-24 animate-in fade-in duration-300">
        {/* Scorecard Hero Banner */}
        <div className="p-6 sm:p-8 rounded-3xl glass-panel border border-sky-500/30 text-center space-y-4 bg-gradient-to-b from-slate-900 via-sky-950/30 to-slate-900">
          <div className="h-16 w-16 mx-auto rounded-3xl bg-gradient-to-tr from-sky-400 to-indigo-600 flex items-center justify-center shadow-xl shadow-sky-500/30">
            <Award className="h-8 w-8 text-white" />
          </div>

          <h2 className="text-2xl font-extrabold text-white">Test Completed!</h2>
          <p className="text-xs text-slate-300">{testTitle} • {subject}</p>

          <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 max-w-xl mx-auto pt-2">
            <div className="p-3 rounded-2xl bg-white/[0.04] border border-white/10">
              <div className="text-xl font-extrabold text-sky-300">{correctCount}/{questions.length}</div>
              <div className="text-[11px] text-slate-400">Total Score</div>
            </div>
            <div className="p-3 rounded-2xl bg-white/[0.04] border border-white/10">
              <div className="text-xl font-extrabold text-emerald-400">{accuracy}%</div>
              <div className="text-[11px] text-slate-400">Accuracy Rate</div>
            </div>
            <div className="p-3 rounded-2xl bg-white/[0.04] border border-white/10">
              <div className="text-xl font-extrabold text-rose-400">{incorrectCount}</div>
              <div className="text-[11px] text-slate-400">Mistakes</div>
            </div>
            <div className="p-3 rounded-2xl bg-white/[0.04] border border-white/10">
              <div className="text-xl font-extrabold text-purple-400">
                {Math.floor((questions.length * 120 - secondsRemaining) / 60)}m
              </div>
              <div className="text-[11px] text-slate-400">Time Taken</div>
            </div>
          </div>

          <button
            onClick={onExitTest}
            className="px-6 py-2.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 text-slate-950 font-bold text-xs shadow-lg shadow-sky-500/20 cursor-pointer"
          >
            Back to Practice Hub
          </button>
        </div>

        {/* AI Diagnostic Report */}
        <div className="p-6 rounded-3xl glass-panel border border-purple-500/30 space-y-3 bg-gradient-to-r from-purple-950/20 via-slate-900 to-indigo-950/20">
          <div className="flex items-center gap-2">
            <Sparkles className="h-5 w-5 text-purple-400" />
            <h3 className="text-base font-bold text-white">NOVA 2.0 AI Diagnostic Analysis</h3>
          </div>

          {isGeneratingDiagnostic ? (
            <div className="flex items-center gap-2 text-xs text-purple-300 py-3">
              <Sparkles className="h-4 w-4 animate-spin" />
              Generating deep error analysis & high-yield remediation plan...
            </div>
          ) : diagnosticReport ? (
            <div className="prose prose-invert prose-xs sm:prose-sm max-w-none text-slate-200">
              <ReactMarkdown>{diagnosticReport}</ReactMarkdown>
            </div>
          ) : null}
        </div>

        {/* Question-by-Question Review */}
        <div className="space-y-4">
          <h3 className="text-base font-bold text-white">Detailed Solutions & Review</h3>
          {questions.map((q, idx) => {
            const userAns = userAnswers[q.id];
            const isCorrect = userAns === q.correctOptionIndex;
            const isSkipped = userAns === undefined;

            return (
              <div
                key={q.id}
                className={`p-5 rounded-3xl glass-card border space-y-3 ${
                  isSkipped
                    ? 'border-slate-700/50'
                    : isCorrect
                    ? 'border-emerald-500/40 bg-emerald-950/10'
                    : 'border-rose-500/40 bg-rose-950/10'
                }`}
              >
                <div className="flex items-center justify-between text-xs">
                  <span className="font-bold text-slate-400">Question {idx + 1}</span>
                  <span
                    className={`px-2 py-0.5 rounded-full font-bold text-[10px] ${
                      isSkipped
                        ? 'bg-slate-700 text-slate-300'
                        : isCorrect
                        ? 'bg-emerald-500/20 text-emerald-300'
                        : 'bg-rose-500/20 text-rose-300'
                    }`}
                  >
                    {isSkipped ? 'Skipped' : isCorrect ? 'Correct (+1)' : 'Incorrect (0)'}
                  </span>
                </div>

                <p className="text-sm font-semibold text-white whitespace-pre-line">
                  {q.questionText}
                </p>

                <div className="space-y-1.5">
                  {q.options.map((opt, optIdx) => {
                    const isSelected = userAns === optIdx;
                    const isRight = optIdx === q.correctOptionIndex;

                    return (
                      <div
                        key={optIdx}
                        className={`p-2.5 rounded-xl text-xs flex items-center justify-between ${
                          isRight
                            ? 'bg-emerald-500/20 text-emerald-200 font-bold border border-emerald-500/40'
                            : isSelected
                            ? 'bg-rose-500/20 text-rose-200 border border-rose-500/40'
                            : 'bg-white/[0.03] text-slate-300'
                        }`}
                      >
                        <div className="flex items-center gap-2">
                          <span className="font-bold">{String.fromCharCode(65 + optIdx)}.</span>
                          <span>{opt}</span>
                        </div>
                        {isRight && <CheckCircle2 className="h-4 w-4 text-emerald-400" />}
                        {isSelected && !isRight && <XCircle className="h-4 w-4 text-rose-400" />}
                      </div>
                    );
                  })}
                </div>

                <div className="p-3 rounded-2xl bg-white/[0.04] border border-white/5 text-xs space-y-1">
                  <div className="font-bold text-sky-400">💡 Explanation & Concept:</div>
                  <p className="text-slate-300 leading-relaxed">{q.explanation}</p>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------
  // ACTIVE TEST IN PROGRESS
  // -------------------------------------------------------------
  return (
    <div className="max-w-4xl mx-auto space-y-4 pb-24 animate-in fade-in duration-200">
      {/* Test Top Bar */}
      <div className="p-4 rounded-2xl glass-panel border border-white/10 flex items-center justify-between gap-3 sticky top-16 z-30 backdrop-blur-xl">
        <div className="min-w-0">
          <h2 className="text-sm font-bold text-white truncate">{testTitle}</h2>
          <p className="text-[11px] text-slate-400">
            Question {currentIndex + 1} of {questions.length} • {currentQ.subject}
          </p>
        </div>

        {/* Countdown Timer */}
        <div className="flex items-center gap-3">
          <div
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl font-mono text-xs font-bold ${
              secondsRemaining < 120
                ? 'bg-rose-500/20 text-rose-300 border border-rose-500/40 animate-pulse'
                : 'bg-sky-500/15 text-sky-300 border border-sky-500/25'
            }`}
          >
            <Clock className="h-3.5 w-3.5" />
            <span>
              {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
            </span>
          </div>

          <button
            onClick={handleSubmitTest}
            className="px-3.5 py-1.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 text-slate-950 font-bold text-xs shadow-md cursor-pointer"
          >
            Submit Test
          </button>
        </div>
      </div>

      {/* Main Question Card */}
      <div className="p-6 rounded-3xl glass-panel border border-white/10 space-y-5">
        <div className="flex items-center justify-between text-xs pb-3 border-b border-white/10">
          <span className="text-sky-400 font-bold">Topic: {currentQ.topic}</span>
          <button
            onClick={toggleMarkForReview}
            className={`flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-semibold transition-colors cursor-pointer ${
              markedForReview[currentQ.id]
                ? 'bg-purple-500/30 text-purple-300 border border-purple-500/40'
                : 'bg-white/[0.05] text-slate-400 hover:text-white'
            }`}
          >
            <Flag className="h-3 w-3" />
            {markedForReview[currentQ.id] ? 'Marked for Review' : 'Mark for Review'}
          </button>
        </div>

        <p className="text-sm sm:text-base font-bold text-white whitespace-pre-line leading-relaxed">
          {currentQ.questionText}
        </p>

        {/* Options */}
        <div className="space-y-2.5">
          {currentQ.options.map((opt, idx) => {
            const isSelected = userAnswers[currentQ.id] === idx;
            return (
              <button
                key={idx}
                onClick={() => handleSelectOption(idx)}
                className={`w-full text-left p-3.5 rounded-2xl border transition-all flex items-center gap-3 cursor-pointer ${
                  isSelected
                    ? 'bg-sky-500/20 border-sky-400 text-white font-bold shadow-lg shadow-sky-500/10'
                    : 'glass-card border-white/5 text-slate-300 hover:bg-white/[0.06]'
                }`}
              >
                <div
                  className={`h-7 w-7 rounded-xl flex items-center justify-center text-xs font-bold shrink-0 ${
                    isSelected
                      ? 'bg-sky-400 text-slate-950'
                      : 'bg-slate-800 text-slate-400 border border-white/10'
                  }`}
                >
                  {String.fromCharCode(65 + idx)}
                </div>
                <span className="text-xs sm:text-sm">{opt}</span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Navigation & Question Palette */}
      <div className="flex items-center justify-between gap-3">
        <button
          onClick={() => setCurrentIndex((prev) => Math.max(0, prev - 1))}
          disabled={currentIndex === 0}
          className="px-4 py-2.5 rounded-xl bg-white/[0.08] hover:bg-white/[0.14] disabled:opacity-40 text-xs font-bold text-white flex items-center gap-1.5 cursor-pointer"
        >
          <ArrowLeft className="h-3.5 w-3.5" />
          Previous
        </button>

        {/* Question Palette Bar */}
        <div className="flex items-center gap-1.5 overflow-x-auto max-w-sm py-1 px-2">
          {questions.map((q, idx) => {
            const isAnswered = userAnswers[q.id] !== undefined;
            const isMarked = markedForReview[q.id];
            const isCur = idx === currentIndex;

            return (
              <button
                key={q.id}
                onClick={() => setCurrentIndex(idx)}
                className={`h-7 w-7 rounded-lg text-xs font-bold shrink-0 transition-all cursor-pointer ${
                  isCur
                    ? 'ring-2 ring-sky-400 scale-105'
                    : ''
                } ${
                  isMarked
                    ? 'bg-purple-600 text-white'
                    : isAnswered
                    ? 'bg-emerald-600 text-white'
                    : 'bg-slate-800 text-slate-400 border border-white/10'
                }`}
              >
                {idx + 1}
              </button>
            );
          })}
        </div>

        <button
          onClick={() => setCurrentIndex((prev) => Math.min(questions.length - 1, prev + 1))}
          disabled={currentIndex === questions.length - 1}
          className="px-4 py-2.5 rounded-xl bg-white/[0.08] hover:bg-white/[0.14] disabled:opacity-40 text-xs font-bold text-white flex items-center gap-1.5 cursor-pointer"
        >
          Next
          <ArrowRight className="h-3.5 w-3.5" />
        </button>
      </div>
    </div>
  );
};
