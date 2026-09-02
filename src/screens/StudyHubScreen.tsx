import React, { useState } from 'react';
import { SubjectItem, StudyPlanItem, Flashcard, ExamInfo } from '../types';
import {
  BookOpen,
  Calendar,
  Layers,
  Sparkles,
  CheckCircle2,
  Circle,
  Plus,
  Play,
  RotateCcw,
  Clock,
  HelpCircle,
  ChevronDown,
  ChevronRight,
  Flame,
  FileText
} from 'lucide-react';

interface StudyHubScreenProps {
  activeExam: ExamInfo;
  subjects: SubjectItem[];
  planItems: StudyPlanItem[];
  flashcards: Flashcard[];
  onToggleTopic: (subjectId: string, chapterId: string, topicId: string) => void;
  onTogglePlanItem: (planId: string) => void;
  onAddPlanItem: (item: StudyPlanItem) => void;
  onStartFocusSprint: (minutes: number, subject: string, topic: string) => void;
  onOpenSummarizer: () => void;
  onReviewFlashcard: (cardId: string, difficulty: 'EASY' | 'MEDIUM' | 'HARD') => void;
  onAskNovaAboutTopic: (topicName: string, subjectName: string) => void;
}

export const StudyHubScreen: React.FC<StudyHubScreenProps> = ({
  activeExam,
  subjects,
  planItems,
  flashcards,
  onToggleTopic,
  onTogglePlanItem,
  onAddPlanItem,
  onStartFocusSprint,
  onOpenSummarizer,
  onReviewFlashcard,
  onAskNovaAboutTopic,
}) => {
  const [activeTab, setActiveTab] = useState<'syllabus' | 'plan' | 'flashcards'>('syllabus');
  const [selectedSubjectId, setSelectedSubjectId] = useState<string>(subjects[0]?.id || '');
  const [expandedChapters, setExpandedChapters] = useState<{ [key: string]: boolean }>({
    [subjects[0]?.chapters[0]?.id || '']: true,
  });

  // Flashcard review state
  const [reviewIndex, setReviewIndex] = useState(0);
  const [isFlipped, setIsFlipped] = useState(false);
  const [showHint, setShowHint] = useState(false);

  // New task modal state
  const [showAddTaskModal, setShowAddTaskModal] = useState(false);
  const [newTaskSubject, setNewTaskSubject] = useState(activeExam.subjects[0] || 'General Studies');
  const [newTaskTopic, setNewTaskTopic] = useState('');
  const [newTaskMinutes, setNewTaskMinutes] = useState(30);
  const [newTaskPriority, setNewTaskPriority] = useState<'HIGH' | 'MEDIUM' | 'LOW'>('HIGH');

  const activeSubject = subjects.find((s) => s.id === selectedSubjectId) || subjects[0];

  const handleCreateTask = (e: React.FormEvent) => {
    e.preventDefault();
    if (!newTaskTopic.trim()) return;
    onAddPlanItem({
      id: `plan_${Date.now()}`,
      subject: newTaskSubject,
      topic: newTaskTopic,
      durationMinutes: newTaskMinutes,
      priority: newTaskPriority,
      isCompleted: false,
      scheduledTime: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    });
    setNewTaskTopic('');
    setShowAddTaskModal(false);
  };

  const toggleChapter = (chapId: string) => {
    setExpandedChapters((prev) => ({ ...prev, [chapId]: !prev[chapId] }));
  };

  const currentCard = flashcards[reviewIndex] || flashcards[0];

  return (
    <div className="space-y-6 pb-24 animate-in fade-in duration-300">
      {/* Top Header & Section Switcher */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-extrabold text-white tracking-tight flex items-center gap-2">
            <BookOpen className="h-6 w-6 text-sky-400" />
            Study & Learn Hub
          </h1>
          <p className="text-xs text-slate-400">
            Official curriculum breakdown, adaptive timetable, and spaced repetition engine
          </p>
        </div>

        {/* Tab Controls */}
        <div className="flex items-center gap-1.5 p-1 bg-white/[0.05] rounded-2xl border border-white/10 shrink-0">
          <button
            id="study-tab-syllabus"
            onClick={() => setActiveTab('syllabus')}
            className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'syllabus'
                ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30 shadow-lg shadow-sky-500/10'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <BookOpen className="h-3.5 w-3.5" />
            Syllabus
          </button>
          <button
            id="study-tab-plan"
            onClick={() => setActiveTab('plan')}
            className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'plan'
                ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30 shadow-lg shadow-sky-500/10'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Calendar className="h-3.5 w-3.5" />
            Daily Plan
          </button>
          <button
            id="study-tab-flashcards"
            onClick={() => setActiveTab('flashcards')}
            className={`px-3.5 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer flex items-center gap-1.5 ${
              activeTab === 'flashcards'
                ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30 shadow-lg shadow-sky-500/10'
                : 'text-slate-400 hover:text-slate-200'
            }`}
          >
            <Layers className="h-3.5 w-3.5" />
            Flashcards ({flashcards.length})
          </button>
        </div>
      </div>

      {/* 1. SYLLABUS NAVIGATOR */}
      {activeTab === 'syllabus' && (
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-5">
          {/* Left: Subject Selector List */}
          <div className="lg:col-span-1 space-y-2">
            <div className="text-xs font-bold text-slate-400 uppercase tracking-wider px-1">
              Subjects ({subjects.length})
            </div>
            {subjects.map((sub) => {
              const isSelected = sub.id === (activeSubject?.id || '');
              return (
                <button
                  key={sub.id}
                  onClick={() => setSelectedSubjectId(sub.id)}
                  className={`w-full text-left p-3 rounded-2xl transition-all border cursor-pointer ${
                    isSelected
                      ? 'bg-sky-500/15 border-sky-500/40 text-white shadow-lg shadow-sky-500/10'
                      : 'glass-card border-white/5 text-slate-300 hover:bg-white/[0.06]'
                  }`}
                >
                  <div className="flex items-center justify-between text-xs font-bold mb-1">
                    <span className="truncate mr-2">{sub.name}</span>
                    <span className="text-sky-300 font-extrabold">{sub.masteryPercentage}%</span>
                  </div>
                  <div className="text-[11px] text-slate-400 mb-2">
                    {sub.completedTopicsCount} of {sub.totalTopicsCount} topics completed
                  </div>
                  <div className="w-full h-1.5 rounded-full bg-slate-800 overflow-hidden">
                    <div
                      className="h-full bg-gradient-to-r from-sky-400 to-indigo-500 rounded-full"
                      style={{ width: `${sub.masteryPercentage}%` }}
                    />
                  </div>
                </button>
              );
            })}

            {/* Quick Notes Summarizer Banner */}
            <div className="mt-4 p-4 rounded-2xl glass-card border border-amber-500/20 bg-amber-500/5 space-y-2">
              <div className="flex items-center gap-2 text-xs font-bold text-amber-300">
                <FileText className="h-4 w-4" />
                Lecture / Notes Summarizer
              </div>
              <p className="text-[11px] text-slate-300">
                Paste syllabus material or textbook chapters to generate instant flashcards & mind maps.
              </p>
              <button
                onClick={onOpenSummarizer}
                className="w-full py-1.5 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/30 text-xs font-bold transition-colors cursor-pointer"
              >
                Launch Summarizer
              </button>
            </div>
          </div>

          {/* Right: Chapters and Topic Checklists */}
          <div className="lg:col-span-3 space-y-4">
            {activeSubject && (
              <>
                <div className="p-4 rounded-2xl glass-panel border border-white/10 flex items-center justify-between">
                  <div>
                    <h2 className="text-base font-bold text-white">{activeSubject.name}</h2>
                    <p className="text-xs text-slate-400">
                      {activeSubject.chaptersCount} Chapters • {activeSubject.totalTopicsCount} High-Yield Topics
                    </p>
                  </div>
                  <button
                    onClick={() => onAskNovaAboutTopic('All Weak Areas', activeSubject.name)}
                    className="px-3 py-1.5 rounded-xl bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 text-xs font-semibold border border-sky-500/30 flex items-center gap-1.5 cursor-pointer"
                  >
                    <Sparkles className="h-3.5 w-3.5" />
                    AI Subject Diagnostic
                  </button>
                </div>

                <div className="space-y-3">
                  {activeSubject.chapters.map((chap) => {
                    const isExpanded = expandedChapters[chap.id] ?? true;
                    return (
                      <div
                        key={chap.id}
                        className="rounded-2xl glass-card border border-white/10 overflow-hidden"
                      >
                        <button
                          onClick={() => toggleChapter(chap.id)}
                          className="w-full p-3.5 flex items-center justify-between hover:bg-white/[0.03] transition-colors cursor-pointer"
                        >
                          <div className="flex items-center gap-2">
                            <BookOpen className="h-4 w-4 text-sky-400" />
                            <span className="text-sm font-bold text-white text-left">
                              {chap.name}
                            </span>
                            <span className="text-[11px] text-slate-400">
                              ({chap.topics.filter((t) => t.isCompleted).length}/{chap.topics.length})
                            </span>
                          </div>
                          {isExpanded ? (
                            <ChevronDown className="h-4 w-4 text-slate-400" />
                          ) : (
                            <ChevronRight className="h-4 w-4 text-slate-400" />
                          )}
                        </button>

                        {isExpanded && (
                          <div className="p-3 pt-0 space-y-2 border-t border-white/5">
                            {chap.topics.map((topic) => (
                              <div
                                key={topic.id}
                                className={`p-3 rounded-xl border transition-all flex items-center justify-between gap-3 ${
                                  topic.isCompleted
                                    ? 'bg-white/[0.02] border-white/5 text-slate-400'
                                    : 'bg-white/[0.04] border-white/10 text-white'
                                }`}
                              >
                                <div className="flex items-center gap-3 min-w-0 flex-1">
                                  <button
                                    onClick={() => onToggleTopic(activeSubject.id, chap.id, topic.id)}
                                    className="text-slate-400 hover:text-sky-400 shrink-0 cursor-pointer"
                                  >
                                    {topic.isCompleted ? (
                                      <CheckCircle2 className="h-5 w-5 text-emerald-400" />
                                    ) : (
                                      <Circle className="h-5 w-5" />
                                    )}
                                  </button>
                                  <div className="min-w-0">
                                    <div className="flex items-center gap-2 flex-wrap">
                                      <span
                                        className={`text-xs font-semibold ${
                                          topic.isCompleted ? 'line-through text-slate-500' : 'text-slate-100'
                                        }`}
                                      >
                                        {topic.name}
                                      </span>
                                      {topic.isWeakArea && (
                                        <span className="text-[10px] px-1.5 py-0.2 rounded bg-rose-500/20 text-rose-300 font-bold">
                                          Weak Area
                                        </span>
                                      )}
                                      <span className="text-[10px] px-1.5 py-0.2 rounded bg-white/10 text-slate-300">
                                        {topic.difficulty}
                                      </span>
                                    </div>
                                    <div className="text-[11px] text-slate-400 mt-0.5">
                                      Est: {topic.estimatedMinutes}m • Revisions: {topic.revisionsCount}
                                    </div>
                                  </div>
                                </div>

                                <div className="flex items-center gap-1.5 shrink-0">
                                  <button
                                    onClick={() =>
                                      onStartFocusSprint(
                                        topic.estimatedMinutes || 25,
                                        activeSubject.name,
                                        topic.name
                                      )
                                    }
                                    className="p-2 rounded-lg bg-sky-500/15 hover:bg-sky-500/25 text-sky-300 text-xs font-semibold border border-sky-500/25 flex items-center gap-1 cursor-pointer"
                                    title="Start Focus Sprint on this topic"
                                  >
                                    <Play className="h-3 w-3 fill-sky-400 text-sky-400" />
                                    <span className="hidden sm:inline">Sprint</span>
                                  </button>
                                  <button
                                    onClick={() => onAskNovaAboutTopic(topic.name, activeSubject.name)}
                                    className="p-2 rounded-lg bg-white/[0.06] hover:bg-white/[0.12] text-slate-300 hover:text-white text-xs cursor-pointer"
                                    title="Ask Nova to explain this topic"
                                  >
                                    <Sparkles className="h-3.5 w-3.5" />
                                  </button>
                                </div>
                              </div>
                            ))}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </>
            )}
          </div>
        </div>
      )}

      {/* 2. ADAPTIVE DAILY PLANNER */}
      {activeTab === 'plan' && (
        <div className="space-y-4">
          <div className="p-4 rounded-2xl glass-panel border border-white/10 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
            <div>
              <h2 className="text-base font-bold text-white flex items-center gap-2">
                <Calendar className="h-4 w-4 text-sky-400" />
                Adaptive Daily Study Schedule
              </h2>
              <p className="text-xs text-slate-400">
                AI dynamically allocates time slots based on your weakest exam areas.
              </p>
            </div>
            <button
              id="study-add-task-btn"
              onClick={() => setShowAddTaskModal(true)}
              className="px-3.5 py-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-slate-950 font-bold text-xs flex items-center gap-1.5 shadow-lg shadow-sky-500/20 cursor-pointer"
            >
              <Plus className="h-4 w-4" />
              Add Custom Study Task
            </button>
          </div>

          {/* Plan Tasks List */}
          <div className="space-y-2.5">
            {planItems.map((task) => (
              <div
                key={task.id}
                className={`p-4 rounded-2xl glass-card border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
                  task.isCompleted ? 'border-white/5 opacity-60' : 'border-white/10 hover:border-sky-500/30'
                }`}
              >
                <div className="flex items-start gap-3 flex-1 min-w-0">
                  <button
                    onClick={() => onTogglePlanItem(task.id)}
                    className="mt-0.5 text-slate-400 hover:text-sky-400 shrink-0 cursor-pointer"
                  >
                    {task.isCompleted ? (
                      <CheckCircle2 className="h-5 w-5 text-emerald-400" />
                    ) : (
                      <Circle className="h-5 w-5" />
                    )}
                  </button>

                  <div className="min-w-0">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span
                        className={`text-sm font-bold ${
                          task.isCompleted ? 'line-through text-slate-400' : 'text-white'
                        }`}
                      >
                        {task.topic}
                      </span>
                      <span
                        className={`text-[10px] px-2 py-0.5 rounded-full font-bold uppercase ${
                          task.priority === 'HIGH'
                            ? 'bg-rose-500/20 text-rose-300 border border-rose-500/30'
                            : task.priority === 'MEDIUM'
                            ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                            : 'bg-slate-700/40 text-slate-300'
                        }`}
                      >
                        {task.priority} Priority
                      </span>
                    </div>

                    <div className="flex items-center gap-2 text-xs text-slate-400 mt-1">
                      <span className="text-sky-300 font-semibold">{task.subject}</span>
                      <span>•</span>
                      <span className="flex items-center gap-1">
                        <Clock className="h-3 w-3" />
                        {task.durationMinutes} mins
                      </span>
                      {task.scheduledTime && (
                        <>
                          <span>•</span>
                          <span>Slot: {task.scheduledTime}</span>
                        </>
                      )}
                    </div>

                    {task.notes && (
                      <p className="text-[11px] text-slate-400 mt-1.5 italic">Tip: {task.notes}</p>
                    )}
                  </div>
                </div>

                {!task.isCompleted && (
                  <button
                    onClick={() => onStartFocusSprint(task.durationMinutes, task.subject, task.topic)}
                    className="px-3.5 py-2 rounded-xl bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 text-xs font-bold border border-sky-500/30 flex items-center justify-center gap-1.5 shrink-0 cursor-pointer"
                  >
                    <Play className="h-3 w-3 fill-sky-400 text-sky-400" />
                    Start {task.durationMinutes}m Sprint
                  </button>
                )}
              </div>
            ))}
          </div>

          {/* Add Task Modal */}
          {showAddTaskModal && (
            <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-150">
              <div className="w-full max-w-md rounded-3xl glass-panel p-5 sm:p-6 border border-white/15 shadow-2xl space-y-4">
                <div className="flex items-center justify-between pb-3 border-b border-white/10">
                  <h3 className="text-base font-bold text-white">Add Study Plan Task</h3>
                  <button
                    onClick={() => setShowAddTaskModal(false)}
                    className="text-slate-400 hover:text-white"
                  >
                    ✕
                  </button>
                </div>

                <form onSubmit={handleCreateTask} className="space-y-3.5">
                  <div>
                    <label className="text-xs font-bold text-slate-300 block mb-1">Subject</label>
                    <select
                      value={newTaskSubject}
                      onChange={(e) => setNewTaskSubject(e.target.value)}
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
                    <label className="text-xs font-bold text-slate-300 block mb-1">Topic / Goal</label>
                    <input
                      type="text"
                      value={newTaskTopic}
                      onChange={(e) => setNewTaskTopic(e.target.value)}
                      placeholder="e.g., Insolvency Code or Formula Flashcards"
                      className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-white/15 text-xs text-white placeholder:text-slate-500 outline-none"
                      required
                    />
                  </div>

                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-xs font-bold text-slate-300 block mb-1">
                        Duration (Minutes)
                      </label>
                      <input
                        type="number"
                        min={10}
                        max={180}
                        step={5}
                        value={newTaskMinutes}
                        onChange={(e) => setNewTaskMinutes(Number(e.target.value))}
                        className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-white/15 text-xs text-white outline-none"
                      />
                    </div>

                    <div>
                      <label className="text-xs font-bold text-slate-300 block mb-1">Priority</label>
                      <select
                        value={newTaskPriority}
                        onChange={(e) =>
                          setNewTaskPriority(e.target.value as 'HIGH' | 'MEDIUM' | 'LOW')
                        }
                        className="w-full px-3.5 py-2.5 rounded-xl bg-slate-950 border border-white/15 text-xs text-white outline-none"
                      >
                        <option value="HIGH">High Priority</option>
                        <option value="MEDIUM">Medium</option>
                        <option value="LOW">Low</option>
                      </select>
                    </div>
                  </div>

                  <div className="pt-2 flex gap-2">
                    <button
                      type="button"
                      onClick={() => setShowAddTaskModal(false)}
                      className="flex-1 py-2.5 rounded-xl bg-white/[0.08] hover:bg-white/[0.12] text-xs font-bold text-slate-300 cursor-pointer"
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 text-xs font-bold text-slate-950 cursor-pointer"
                    >
                      Add Task
                    </button>
                  </div>
                </form>
              </div>
            </div>
          )}
        </div>
      )}

      {/* 3. SPACED REPETITION FLASHCARDS REVIEW */}
      {activeTab === 'flashcards' && (
        <div className="max-w-xl mx-auto space-y-5">
          {/* Card Review Header */}
          <div className="flex items-center justify-between text-xs text-slate-400">
            <span>
              Card {reviewIndex + 1} of {flashcards.length}
            </span>
            <span className="text-sky-400 font-semibold">{currentCard.subject}</span>
          </div>

          {/* 3D Interactive Flashcard */}
          <div
            onClick={() => setIsFlipped(!isFlipped)}
            className="w-full min-h-[260px] rounded-3xl glass-panel border border-sky-500/30 p-6 flex flex-col justify-between shadow-2xl relative cursor-pointer hover:border-sky-400/50 transition-all duration-200"
          >
            <div className="flex items-center justify-between">
              <span className="text-[10px] px-2.5 py-1 rounded-full bg-sky-500/20 text-sky-300 font-bold uppercase tracking-wider">
                {isFlipped ? 'Answer / Solution' : 'Question / Concept'}
              </span>
              <span className="text-xs text-slate-400 flex items-center gap-1">
                <RotateCcw className="h-3 w-3" /> Tap to flip
              </span>
            </div>

            <div className="my-6 text-center">
              <p className="text-base sm:text-lg font-bold text-white leading-relaxed">
                {isFlipped ? currentCard.back : currentCard.front}
              </p>

              {showHint && !isFlipped && currentCard.hint && (
                <div className="mt-3 p-2.5 rounded-xl bg-amber-500/10 border border-amber-500/20 text-xs text-amber-300">
                  💡 Hint: {currentCard.hint}
                </div>
              )}
            </div>

            <div className="flex items-center justify-between pt-2 border-t border-white/10 text-xs text-slate-400">
              <span className="font-semibold text-slate-300">Topic: {currentCard.topic}</span>
              {currentCard.hint && !isFlipped && (
                <button
                  onClick={(e) => {
                    e.stopPropagation();
                    setShowHint(!showHint);
                  }}
                  className="text-amber-400 hover:text-amber-300 text-xs font-semibold cursor-pointer"
                >
                  {showHint ? 'Hide Hint' : 'Show Hint'}
                </button>
              )}
            </div>
          </div>

          {/* SM-2 Spaced Ease Buttons */}
          <div className="space-y-2">
            <div className="text-center text-xs font-semibold text-slate-400">
              Rate your recall difficulty to optimize next review interval:
            </div>
            <div className="grid grid-cols-3 gap-2">
              <button
                onClick={() => {
                  onReviewFlashcard(currentCard.id, 'HARD');
                  setIsFlipped(false);
                  setShowHint(false);
                  setReviewIndex((prev) => (prev + 1) % flashcards.length);
                }}
                className="py-3 rounded-2xl bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 font-bold text-xs border border-rose-500/30 transition-all cursor-pointer"
              >
                Hard (+1 Day)
              </button>
              <button
                onClick={() => {
                  onReviewFlashcard(currentCard.id, 'MEDIUM');
                  setIsFlipped(false);
                  setShowHint(false);
                  setReviewIndex((prev) => (prev + 1) % flashcards.length);
                }}
                className="py-3 rounded-2xl bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 font-bold text-xs border border-amber-500/30 transition-all cursor-pointer"
              >
                Good (+4 Days)
              </button>
              <button
                onClick={() => {
                  onReviewFlashcard(currentCard.id, 'EASY');
                  setIsFlipped(false);
                  setShowHint(false);
                  setReviewIndex((prev) => (prev + 1) % flashcards.length);
                }}
                className="py-3 rounded-2xl bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 font-bold text-xs border border-emerald-500/30 transition-all cursor-pointer"
              >
                Easy (+7 Days)
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
