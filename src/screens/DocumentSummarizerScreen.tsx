import React, { useState } from 'react';
import { ExamInfo, Flashcard, Question } from '../types';
import { summarizeDocument } from '../services/api';
import ReactMarkdown from 'react-markdown';
import {
  FileText,
  Sparkles,
  Layers,
  CheckCircle2,
  GitBranch,
  ArrowRight,
  Copy,
  Plus,
  RotateCcw
} from 'lucide-react';

interface DocumentSummarizerScreenProps {
  activeExam: ExamInfo;
  onAddFlashcards: (cards: Flashcard[]) => void;
  onStartQuizWithQuestions: (title: string, subject: string, questions: Question[]) => void;
  onBack: () => void;
}

export const DocumentSummarizerScreen: React.FC<DocumentSummarizerScreenProps> = ({
  activeExam,
  onAddFlashcards,
  onStartQuizWithQuestions,
  onBack,
}) => {
  const [inputText, setInputText] = useState('');
  const [selectedSubject, setSelectedSubject] = useState(activeExam.subjects[0] || 'General Studies');
  const [isLoading, setIsLoading] = useState(false);
  const [summaryData, setSummaryData] = useState<{
    summaryMarkdown: string;
    flashcards: { front: string; back: string; hint?: string }[];
    keyPoints: string[];
    mindMapOutline: { heading: string; subpoints: string[] }[];
    quizQuestions: Question[];
  } | null>(null);

  const [activeTab, setActiveTab] = useState<'summary' | 'flashcards' | 'mindmap' | 'quiz'>('summary');
  const [copied, setCopied] = useState(false);
  const [addedCards, setAddedCards] = useState(false);

  const sampleTexts = [
    {
      title: 'Landmark Supreme Court Verdicts (Polity)',
      text: `In Kesavananda Bharati v. State of Kerala (1973), the Supreme Court ruled that Parliament's constituent power to amend the Constitution under Article 368 is not unlimited; it cannot alter the Basic Structure of the Constitution. Later in Minerva Mills (1980), the balance between Fundamental Rights (Part III) and Directive Principles of State Policy (Part IV) was held as a basic feature. In Maneka Gandhi (1978), Article 21's 'procedure established by law' was interpreted to mean 'due process of law'—the procedure must be just, fair, and reasonable.`,
    },
    {
      title: 'Monetary Policy & Inflation Dynamics (Economy)',
      text: `The Reserve Bank of India (RBI) operates a flexible inflation targeting framework under the RBI Act, 1934, targeting 4% Headline Consumer Price Index (CPI) with a tolerance band of +/- 2%. Key instruments include the Repo Rate (rate at which RBI lends short term to commercial banks), Reverse Repo Rate, and Cash Reserve Ratio (CRR). Headline inflation reflects overall CPI including volatile food and fuel components, while Core Inflation strips out food and energy volatility.`,
    },
  ];

  const handleSummarize = async () => {
    if (!inputText.trim()) return;
    setIsLoading(true);
    setAddedCards(false);

    try {
      const res = await summarizeDocument(inputText, activeExam.name, selectedSubject);
      setSummaryData(res);
      setActiveTab('summary');
    } catch (e) {
      console.error(e);
    } finally {
      setIsLoading(false);
    }
  };

  const handleImportFlashcards = () => {
    if (!summaryData) return;
    const newCards: Flashcard[] = summaryData.flashcards.map((f, i) => ({
      id: `ai_card_${Date.now()}_${i}`,
      deckId: 'deck_ai_notes',
      front: f.front,
      back: f.back,
      hint: f.hint,
      subject: selectedSubject,
      topic: 'AI Generated Notes',
      difficulty: 'MEDIUM',
      intervalDays: 1,
      repetitionCount: 0,
      easeFactor: 2.5,
      nextReviewDate: new Date(Date.now() + 86400000).toISOString().split('T')[0],
    }));
    onAddFlashcards(newCards);
    setAddedCards(true);
  };

  const handleCopySummary = () => {
    if (summaryData?.summaryMarkdown) {
      navigator.clipboard.writeText(summaryData.summaryMarkdown);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6 pb-24 animate-in fade-in duration-300">
      {/* Top Banner */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-extrabold text-white tracking-tight flex items-center gap-2">
            <FileText className="h-6 w-6 text-amber-400" />
            Lecture & Document Intelligence Summarizer
          </h1>
          <p className="text-xs text-slate-400">
            Paste syllabus text, PDF chapters, or current affairs to extract instant flashcards & mind maps
          </p>
        </div>
        <button
          onClick={onBack}
          className="px-3.5 py-1.5 rounded-xl bg-white/[0.08] hover:bg-white/[0.14] text-xs font-semibold text-slate-300 cursor-pointer"
        >
          Back
        </button>
      </div>

      {/* Input Form */}
      <div className="p-6 rounded-3xl glass-panel border border-white/10 space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="flex items-center gap-2">
            <label className="text-xs font-bold text-slate-300">Target Subject:</label>
            <select
              value={selectedSubject}
              onChange={(e) => setSelectedSubject(e.target.value)}
              className="px-3 py-1.5 rounded-xl bg-slate-950 border border-white/15 text-xs text-white outline-none"
            >
              {activeExam.subjects.map((sub, i) => (
                <option key={i} value={sub}>
                  {sub}
                </option>
              ))}
            </select>
          </div>

          {/* Sample Snippet Loaders */}
          <div className="flex items-center gap-2">
            <span className="text-[11px] text-slate-400">Load Sample:</span>
            {sampleTexts.map((s, idx) => (
              <button
                key={idx}
                onClick={() => setInputText(s.text)}
                className="px-2.5 py-1 rounded-lg bg-white/[0.06] hover:bg-white/[0.12] text-[11px] text-sky-300 border border-white/10 cursor-pointer"
              >
                {s.title.split(' ')[0]}
              </button>
            ))}
          </div>
        </div>

        <textarea
          rows={6}
          value={inputText}
          onChange={(e) => setInputText(e.target.value)}
          placeholder="Paste textbook excerpt, lecture transcripts, newspaper editorial, or study notes here..."
          className="w-full p-4 rounded-2xl bg-slate-950/90 border border-white/15 focus:border-amber-400/50 text-xs sm:text-sm text-white placeholder:text-slate-500 outline-none leading-relaxed"
        />

        <button
          onClick={handleSummarize}
          disabled={isLoading || !inputText.trim()}
          className="w-full py-3 rounded-xl bg-gradient-to-r from-amber-500 via-sky-500 to-indigo-600 hover:from-amber-400 hover:to-indigo-500 text-slate-950 font-extrabold text-xs sm:text-sm shadow-xl shadow-amber-500/20 flex items-center justify-center gap-2 cursor-pointer disabled:opacity-50"
        >
          {isLoading ? (
            <>
              <Sparkles className="h-4 w-4 animate-spin text-slate-950" />
              Synthesizing Document Intelligence with Gemini AI...
            </>
          ) : (
            <>
              <Sparkles className="h-4 w-4 fill-slate-950" />
              Summarize & Generate Study Assets
            </>
          )}
        </button>
      </div>

      {/* Generated Intelligence Output */}
      {summaryData && (
        <div className="p-6 rounded-3xl glass-panel border border-amber-500/30 space-y-5 animate-in fade-in duration-200">
          {/* Sub Navigation Bar */}
          <div className="flex items-center justify-between pb-3 border-b border-white/10 flex-wrap gap-2">
            <div className="flex items-center gap-1.5 bg-white/[0.05] p-1 rounded-2xl border border-white/10">
              <button
                onClick={() => setActiveTab('summary')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  activeTab === 'summary'
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                Executive Summary
              </button>
              <button
                onClick={() => setActiveTab('flashcards')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  activeTab === 'flashcards'
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                Flashcards ({summaryData.flashcards.length})
              </button>
              <button
                onClick={() => setActiveTab('mindmap')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  activeTab === 'mindmap'
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                Mind Map Outline
              </button>
              <button
                onClick={() => setActiveTab('quiz')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  activeTab === 'quiz'
                    ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                    : 'text-slate-400 hover:text-white'
                }`}
              >
                Practice Quiz ({summaryData.quizQuestions.length})
              </button>
            </div>

            {activeTab === 'summary' && (
              <button
                onClick={handleCopySummary}
                className="px-3 py-1.5 rounded-xl bg-white/[0.08] hover:bg-white/[0.12] text-xs font-semibold text-slate-300 flex items-center gap-1 cursor-pointer"
              >
                <Copy className="h-3.5 w-3.5" />
                <span>{copied ? 'Copied!' : 'Copy Summary'}</span>
              </button>
            )}

            {activeTab === 'flashcards' && (
              <button
                onClick={handleImportFlashcards}
                disabled={addedCards}
                className="px-3 py-1.5 rounded-xl bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 border border-emerald-500/30 text-xs font-bold flex items-center gap-1 cursor-pointer disabled:opacity-50"
              >
                <Plus className="h-3.5 w-3.5" />
                <span>{addedCards ? '✓ Added to Flashcard Deck' : 'Add to Flashcard Deck'}</span>
              </button>
            )}
          </div>

          {/* Tab 1: Executive Summary */}
          {activeTab === 'summary' && (
            <div className="space-y-4">
              <div className="prose prose-invert prose-xs sm:prose-sm max-w-none text-slate-200 leading-relaxed">
                <ReactMarkdown>{summaryData.summaryMarkdown}</ReactMarkdown>
              </div>

              {/* Key Bullet Takeaways */}
              {summaryData.keyPoints && summaryData.keyPoints.length > 0 && (
                <div className="p-4 rounded-2xl bg-white/[0.03] border border-white/10 space-y-2">
                  <div className="text-xs font-bold text-amber-300 uppercase tracking-wider">
                    High-Yield Exam Takeaways
                  </div>
                  <ul className="space-y-1.5">
                    {summaryData.keyPoints.map((pt, i) => (
                      <li key={i} className="text-xs text-slate-300 flex items-start gap-2">
                        <CheckCircle2 className="h-3.5 w-3.5 text-amber-400 shrink-0 mt-0.5" />
                        <span>{pt}</span>
                      </li>
                    ))}
                  </ul>
                </div>
              )}
            </div>
          )}

          {/* Tab 2: Flashcards Extracted */}
          {activeTab === 'flashcards' && (
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              {summaryData.flashcards.map((fc, idx) => (
                <div key={idx} className="p-4 rounded-2xl glass-card border border-white/10 space-y-2">
                  <div className="text-[10px] font-bold text-sky-400 uppercase">Flashcard #{idx + 1}</div>
                  <div className="text-xs font-bold text-white">{fc.front}</div>
                  <div className="text-xs text-slate-300 pt-2 border-t border-white/5">{fc.back}</div>
                  {fc.hint && <div className="text-[11px] text-amber-300">💡 Hint: {fc.hint}</div>}
                </div>
              ))}
            </div>
          )}

          {/* Tab 3: Mind Map Outline */}
          {activeTab === 'mindmap' && (
            <div className="space-y-3">
              {summaryData.mindMapOutline.map((node, idx) => (
                <div key={idx} className="p-4 rounded-2xl bg-white/[0.03] border border-white/10 space-y-2">
                  <div className="flex items-center gap-2 text-sm font-bold text-sky-300">
                    <GitBranch className="h-4 w-4 text-sky-400" />
                    <span>{node.heading}</span>
                  </div>
                  <div className="pl-6 space-y-1">
                    {node.subpoints.map((sub, sIdx) => (
                      <div key={sIdx} className="text-xs text-slate-300 flex items-center gap-1.5">
                        <span className="h-1.5 w-1.5 rounded-full bg-sky-400"></span>
                        <span>{sub}</span>
                      </div>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          )}

          {/* Tab 4: Practice Quiz */}
          {activeTab === 'quiz' && (
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs text-slate-300">
                  {summaryData.quizQuestions.length} Questions extracted from your notes
                </span>
                <button
                  onClick={() =>
                    onStartQuizWithQuestions(
                      `Document Quiz: ${selectedSubject}`,
                      selectedSubject,
                      summaryData.quizQuestions
                    )
                  }
                  className="px-4 py-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 text-slate-950 font-bold text-xs shadow-md cursor-pointer"
                >
                  Start Live Quiz with these Questions
                </button>
              </div>

              <div className="space-y-3">
                {summaryData.quizQuestions.map((q, idx) => (
                  <div key={idx} className="p-4 rounded-2xl bg-white/[0.03] border border-white/10 space-y-2">
                    <div className="text-xs font-bold text-white">
                      Q{idx + 1}. {q.questionText}
                    </div>
                    <div className="text-xs text-emerald-400 font-semibold">
                      Answer: {q.options[q.correctOptionIndex]}
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
