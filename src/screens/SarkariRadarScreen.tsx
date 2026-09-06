import React, { useState, useEffect, useMemo } from 'react';
import {
  Briefcase,
  Calendar,
  ExternalLink,
  Search,
  Filter,
  CheckCircle2,
  Bookmark,
  Sparkles,
  RefreshCw,
  Clock,
  GraduationCap,
  FileText,
  Target,
  Bot,
  ChevronRight,
  X,
  AlertCircle,
  Award,
  Send,
  Download,
  Flame,
  Radio,
  Share2,
  Key,
  Bell,
  TrendingUp,
  MapPin,
  HelpCircle,
} from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import {
  SarkariJob,
  SarkariAdmitCard,
  SarkariResult,
  SarkariAnswerKey,
  SarkariNotification,
  SarkariLatestUpdate,
  SarkariRadarSyncStatus,
  SarkariTargetExamPlan,
  StudyPlanItem,
  UserProfile,
} from '../types';
import {
  SarkariRadarService,
  calculateDaysRemaining,
} from '../services/sarkariRadarService';

interface SarkariRadarScreenProps {
  user?: UserProfile;
  bookmarkedIds?: string[];
  appliedIds?: string[];
  onToggleBookmark?: (id: string) => void;
  onToggleApplied?: (id: string) => void;
  onSetTargetExam?: (examTitle: string) => void;
  onAddPlanItem?: (item: StudyPlanItem) => void;
  onStartFocusSprint?: (minutes: number, subject: string, topic: string) => void;
}

export const SarkariRadarScreen: React.FC<SarkariRadarScreenProps> = ({
  user,
  bookmarkedIds = [],
  appliedIds = [],
  onToggleBookmark,
  onToggleApplied,
  onSetTargetExam,
  onAddPlanItem,
  onStartFocusSprint,
}) => {
  // State for live radar data across all 6 tables
  const [jobs, setJobs] = useState<SarkariJob[]>([]);
  const [admitCards, setAdmitCards] = useState<SarkariAdmitCard[]>([]);
  const [results, setResults] = useState<SarkariResult[]>([]);
  const [answerKeys, setAnswerKeys] = useState<SarkariAnswerKey[]>([]);
  const [notifications, setNotifications] = useState<SarkariNotification[]>([]);
  const [latestUpdates, setLatestUpdates] = useState<SarkariLatestUpdate[]>([]);
  const [syncStatus, setSyncStatus] = useState<SarkariRadarSyncStatus>({
    connected: true,
    source: 'cached_local',
    sourceLabel: '🟢 Live Connected to StudyMate Sarkari Database',
    lastSyncTime: 'Just now',
    jobsCount: 0,
    admitCardsCount: 0,
    resultsCount: 0,
    answerKeysCount: 0,
    notificationsCount: 0,
    latestUpdatesCount: 0,
  });

  const [activeTab, setActiveTab] = useState<
    'jobs' | 'admit_cards' | 'answer_keys' | 'results' | 'notifications'
  >('jobs');
  const [isLoading, setIsLoading] = useState<boolean>(true);
  const [isRefreshing, setIsRefreshing] = useState<boolean>(false);
  const [activeFilter, setActiveFilter] = useState<string>('All');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedJob, setSelectedJob] = useState<SarkariJob | null>(null);

  // Modals state
  const [showPlannerModal, setShowPlannerModal] = useState<boolean>(false);
  const [planningJob, setPlanningJob] = useState<SarkariJob | null>(null);
  const [generatedPlan, setGeneratedPlan] = useState<SarkariTargetExamPlan | null>(null);
  const [isGeneratingPlan, setIsGeneratingPlan] = useState<boolean>(false);
  const [planSuccessNotice, setPlanSuccessNotice] = useState<string | null>(null);

  // Hinglish AI Eligibility Assistant
  const [showEligibilityModal, setShowEligibilityModal] = useState<boolean>(false);
  const [eligibilityPrompt, setEligibilityPrompt] = useState<string>('');
  const [eligibilityChat, setEligibilityChat] = useState<
    { sender: 'user' | 'assistant'; text: string; time: string }[]
  >([
    {
      sender: 'assistant',
      text:
        'Namaste! Main hoon aapka **Hinglish AI Sarkari Job & Eligibility Advisor**. Aap mujhse pooch sakte hain:\n\n' +
        '- *"Bhai meri 12th pass hai aur age 21 hai, mere liye kaunsi live vacancy hai?"*\n' +
        '- *"Graduation me 50% hai, kya main SSC CGL ya IBPS PO ke liye eligible hoon?"*\n' +
        '- *"Upcoming Police SI aur Constable me physical running aur height criteria kya hai?"*\n\n' +
        'Aap apni age, qualification aur category bataiye, main live database se match karke instant bataunga!',
      time: 'Just now',
    },
  ]);
  const [isAnsweringEligibility, setIsAnsweringEligibility] = useState<boolean>(false);

  // Collapsible toggle for quick preview strips
  const [showAdmitCardsStrip, setShowAdmitCardsStrip] = useState<boolean>(true);
  const [showAnswerKeysStrip, setShowAnswerKeysStrip] = useState<boolean>(false);
  const [showResultsStrip, setShowResultsStrip] = useState<boolean>(false);
  const [showNotificationsStrip, setShowNotificationsStrip] = useState<boolean>(false);

  // Load live data from Supabase / Render API on mount
  useEffect(() => {
    loadData(false);
  }, []);

  const loadData = async (manualRefresh: boolean = false) => {
    if (manualRefresh) {
      setIsRefreshing(true);
    } else {
      setIsLoading(true);
    }

    try {
      const data = await SarkariRadarService.fetchAllRadarData();
      setJobs(data.jobs);
      setAdmitCards(data.admitCards);
      setResults(data.results);
      setAnswerKeys(data.answerKeys);
      setNotifications(data.notifications);
      setLatestUpdates(data.latestUpdates);
      setSyncStatus(data.syncStatus);
    } catch (err) {
      console.error('Failed to load Sarkari Radar data:', err);
    } finally {
      setIsLoading(false);
      setIsRefreshing(false);
    }
  };

  // Filter chips definitions
  const filterChips = [
    { id: 'All', label: 'All Jobs' },
    { id: 'SSC', label: 'SSC' },
    { id: 'Railway', label: 'Railways' },
    { id: 'Police', label: 'Police & Defence' },
    { id: 'Banking', label: 'Banking' },
    { id: 'UPSC', label: 'UPSC' },
    { id: '10th', label: '10th Pass' },
    { id: '12th', label: '12th Pass' },
    { id: 'Graduate', label: 'Graduate' },
    { id: 'Bookmarked', label: 'Saved' },
    { id: 'Applied', label: 'Applied' },
  ];

  // Filter and search logic
  const filteredJobs = useMemo(() => {
    return jobs.filter((job) => {
      // 1. Filter match
      let matchesFilter = true;
      if (activeFilter === 'SSC') {
        matchesFilter = job.category.toUpperCase().includes('SSC');
      } else if (activeFilter === 'Railway') {
        matchesFilter = job.category.toUpperCase().includes('RAILWAY') || job.category.toUpperCase().includes('RRB');
      } else if (activeFilter === 'Police') {
        matchesFilter =
          job.category.toUpperCase().includes('POLICE') ||
          job.category.toUpperCase().includes('DEFENCE') ||
          job.title.toUpperCase().includes('CONSTABLE') ||
          job.title.toUpperCase().includes('SI');
      } else if (activeFilter === 'Banking') {
        matchesFilter =
          job.category.toUpperCase().includes('BANK') ||
          job.category.toUpperCase().includes('IBPS') ||
          job.category.toUpperCase().includes('SBI');
      } else if (activeFilter === 'UPSC') {
        matchesFilter = job.category.toUpperCase().includes('UPSC');
      } else if (activeFilter === '10th') {
        matchesFilter =
          job.qualification.toLowerCase().includes('10th') ||
          job.qualification.toLowerCase().includes('matric');
      } else if (activeFilter === '12th') {
        matchesFilter =
          job.qualification.toLowerCase().includes('12th') ||
          job.qualification.toLowerCase().includes('intermediate') ||
          job.qualification.toLowerCase().includes('10+2');
      } else if (activeFilter === 'Graduate') {
        matchesFilter =
          job.qualification.toLowerCase().includes('graduate') ||
          job.qualification.toLowerCase().includes('bachelor') ||
          job.qualification.toLowerCase().includes('degree');
      } else if (activeFilter === 'Bookmarked') {
        matchesFilter = bookmarkedIds.includes(job.id);
      } else if (activeFilter === 'Applied') {
        matchesFilter = appliedIds.includes(job.id);
      }

      if (!matchesFilter) return false;

      // 2. Search query match
      if (!searchQuery.trim()) return true;
      const q = searchQuery.toLowerCase();
      return (
        job.title.toLowerCase().includes(q) ||
        (job.organization_name && job.organization_name.toLowerCase().includes(q)) ||
        (job.department && job.department.toLowerCase().includes(q)) ||
        job.qualification.toLowerCase().includes(q) ||
        job.category.toLowerCase().includes(q)
      );
    });
  }, [jobs, activeFilter, searchQuery, bookmarkedIds, appliedIds]);

  const filteredAdmitCards = useMemo(() => {
    if (!searchQuery.trim()) return admitCards;
    const q = searchQuery.toLowerCase();
    return admitCards.filter(
      (ac) =>
        ac.title.toLowerCase().includes(q) ||
        (ac.exam_name && ac.exam_name.toLowerCase().includes(q)) ||
        (ac.organization_name && ac.organization_name.toLowerCase().includes(q))
    );
  }, [admitCards, searchQuery]);

  const filteredAnswerKeys = useMemo(() => {
    if (!searchQuery.trim()) return answerKeys;
    const q = searchQuery.toLowerCase();
    return answerKeys.filter(
      (ak) =>
        ak.title.toLowerCase().includes(q) ||
        (ak.exam_name && ak.exam_name.toLowerCase().includes(q)) ||
        (ak.organization_name && ak.organization_name.toLowerCase().includes(q))
    );
  }, [answerKeys, searchQuery]);

  const filteredResults = useMemo(() => {
    if (!searchQuery.trim()) return results;
    const q = searchQuery.toLowerCase();
    return results.filter(
      (res) =>
        res.title.toLowerCase().includes(q) ||
        (res.exam_name && res.exam_name.toLowerCase().includes(q)) ||
        (res.organization_name && res.organization_name.toLowerCase().includes(q))
    );
  }, [results, searchQuery]);

  const filteredNotifications = useMemo(() => {
    if (!searchQuery.trim()) return notifications;
    const q = searchQuery.toLowerCase();
    return notifications.filter(
      (notif) =>
        notif.title.toLowerCase().includes(q) ||
        (notif.organization_name && notif.organization_name.toLowerCase().includes(q)) ||
        (notif.notification_type && notif.notification_type.toLowerCase().includes(q)) ||
        (notif.description && notif.description.toLowerCase().includes(q))
    );
  }, [notifications, searchQuery]);

  // Handler: Open AI Target Exam Study Planner
  const handleOpenStudyPlanner = async (job: SarkariJob) => {
    setPlanningJob(job);
    setShowPlannerModal(true);
    setPlanSuccessNotice(null);
    setIsGeneratingPlan(true);

    try {
      const plan = await SarkariRadarService.generateTargetExamStudyPlan(job, 5);
      setGeneratedPlan(plan);
    } catch (err) {
      console.error('Failed to generate target exam plan:', err);
    } finally {
      setIsGeneratingPlan(false);
    }
  };

  // Handler: Apply generated plan to StudyMate Plan
  const handleApplyPlanToStudyMate = () => {
    if (!generatedPlan || !onAddPlanItem) return;

    // Add high-priority plan items for Week 1
    const firstWeekRoutines = generatedPlan.dailyRoutines.slice(0, 7);
    firstWeekRoutines.forEach((routine) => {
      onAddPlanItem({
        id: `plan_target_${Date.now()}_${routine.dayNumber}`,
        subject: routine.focusSubject,
        topic: routine.highYieldTopics[0] || `${generatedPlan.examTitle} Module`,
        durationMinutes: routine.recommendedHours * 60,
        priority: 'HIGH',
        isCompleted: false,
        scheduledTime: '09:00 AM',
        notes: `Target Exam: ${generatedPlan.examTitle} — ${routine.revisionAction}`,
      });
    });

    if (onSetTargetExam) {
      onSetTargetExam(generatedPlan.examTitle);
    }

    setPlanSuccessNotice('🎉 Week 1 routines added to your Study Hub plan! Exam set as active target.');
  };

  // Handler: Send question to Hinglish AI Eligibility Assistant
  const handleSendEligibilityQuestion = async (presetPrompt?: string) => {
    const questionText = presetPrompt || eligibilityPrompt;
    if (!questionText.trim() || isAnsweringEligibility) return;

    const userMsg = {
      sender: 'user' as const,
      text: questionText,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
    };

    setEligibilityChat((prev) => [...prev, userMsg]);
    if (!presetPrompt) setEligibilityPrompt('');
    setIsAnsweringEligibility(true);

    try {
      const reply = await SarkariRadarService.askEligibilityAssistant(questionText, jobs, eligibilityChat);
      const assistantMsg = {
        sender: 'assistant' as const,
        text: reply,
        time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      };
      setEligibilityChat((prev) => [...prev, assistantMsg]);
    } catch {
      setEligibilityChat((prev) => [
        ...prev,
        {
          sender: 'assistant',
          text:
            'Aapke qualification ke hisaab se live database me kai active vacancies hain! Kripya ek baar internet connection check karke dobara poochhein.',
          time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        },
      ]);
    } finally {
      setIsAnsweringEligibility(false);
    }
  };

  return (
    <div className="space-y-6 pb-28 animate-in fade-in duration-200">
      {/* ------------------------------------------------------------- */}
      {/* 1. HEADER & LIVE SYNC STATUS BANNER                          */}
      {/* ------------------------------------------------------------- */}
      <div className="glass-panel p-5 rounded-3xl border border-white/10 relative overflow-hidden bg-gradient-to-br from-slate-900/90 via-sky-950/40 to-slate-900/90 shadow-xl">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <div className="flex items-center gap-2 mb-1.5 flex-wrap">
              <span className="inline-flex items-center gap-1.5 text-[11px] font-semibold px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                <span className="h-2 w-2 rounded-full bg-emerald-400 animate-ping" />
                Live Radar
              </span>
              <span className="text-[11px] font-medium text-slate-300 flex items-center gap-1">
                {syncStatus.sourceLabel}
              </span>
            </div>
            <h1 className="text-xl sm:text-2xl font-bold text-white tracking-tight flex items-center gap-2">
              <Radio className="h-6 w-6 text-sky-400 shrink-0" />
              Sarkari Job Vacancy & Exam Radar
            </h1>
            <p className="text-xs sm:text-sm text-slate-300 mt-1 max-w-2xl">
              Continuous live feed from shared Supabase database & verified government portals.
              Explore active recruitments, admit cards, and launch instant AI study routines.
            </p>
          </div>

          <div className="flex items-center gap-2 self-start md:self-center shrink-0">
            <button
              id="radar-sync-button"
              onClick={() => loadData(true)}
              disabled={isRefreshing}
              className="px-3 py-2 rounded-xl bg-white/[0.08] hover:bg-white/[0.15] text-slate-200 text-xs font-semibold border border-white/10 flex items-center gap-1.5 cursor-pointer transition-all active:scale-95 disabled:opacity-50"
              title="Sync with Render & Supabase database"
            >
              <RefreshCw className={`h-3.5 w-3.5 text-sky-400 ${isRefreshing ? 'animate-spin' : ''}`} />
              <span>{isRefreshing ? 'Syncing...' : 'Sync Live Feed'}</span>
            </button>
          </div>
        </div>

        {/* Live Counters - 5 Dedicated Schema Entities */}
        <div className="grid grid-cols-2 sm:grid-cols-5 gap-2 sm:gap-3 mt-4 pt-4 border-t border-white/10 text-center">
          <button
            onClick={() => setActiveTab('jobs')}
            className={`p-2.5 rounded-2xl border transition-all text-center cursor-pointer ${
              activeTab === 'jobs'
                ? 'bg-sky-500/20 border-sky-400/50 shadow-md shadow-sky-500/20'
                : 'bg-white/[0.03] border-white/5 hover:bg-white/[0.08]'
            }`}
          >
            <div className="text-lg sm:text-xl font-black text-white">{jobs.length}</div>
            <div className="text-[10px] sm:text-xs text-slate-300 font-medium">Active Vacancies</div>
          </button>

          <button
            onClick={() => setActiveTab('admit_cards')}
            className={`p-2.5 rounded-2xl border transition-all text-center cursor-pointer ${
              activeTab === 'admit_cards'
                ? 'bg-amber-500/20 border-amber-400/50 shadow-md shadow-amber-500/20'
                : 'bg-white/[0.03] border-white/5 hover:bg-white/[0.08]'
            }`}
          >
            <div className="text-lg sm:text-xl font-black text-amber-300">{admitCards.length}</div>
            <div className="text-[10px] sm:text-xs text-slate-300 font-medium">Admit Cards & Slips</div>
          </button>

          <button
            onClick={() => setActiveTab('answer_keys')}
            className={`p-2.5 rounded-2xl border transition-all text-center cursor-pointer ${
              activeTab === 'answer_keys'
                ? 'bg-purple-500/20 border-purple-400/50 shadow-md shadow-purple-500/20'
                : 'bg-white/[0.03] border-white/5 hover:bg-white/[0.08]'
            }`}
          >
            <div className="text-lg sm:text-xl font-black text-purple-300">{answerKeys.length}</div>
            <div className="text-[10px] sm:text-xs text-slate-300 font-medium">Answer Keys</div>
          </button>

          <button
            onClick={() => setActiveTab('results')}
            className={`p-2.5 rounded-2xl border transition-all text-center cursor-pointer ${
              activeTab === 'results'
                ? 'bg-emerald-500/20 border-emerald-400/50 shadow-md shadow-emerald-500/20'
                : 'bg-white/[0.03] border-white/5 hover:bg-white/[0.08]'
            }`}
          >
            <div className="text-lg sm:text-xl font-black text-emerald-300">{results.length}</div>
            <div className="text-[10px] sm:text-xs text-slate-300 font-medium">Results Declared</div>
          </button>

          <button
            onClick={() => setActiveTab('notifications')}
            className={`p-2.5 rounded-2xl border transition-all text-center cursor-pointer col-span-2 sm:col-span-1 ${
              activeTab === 'notifications'
                ? 'bg-rose-500/20 border-rose-400/50 shadow-md shadow-rose-500/20'
                : 'bg-white/[0.03] border-white/5 hover:bg-white/[0.08]'
            }`}
          >
            <div className="text-lg sm:text-xl font-black text-rose-300">{notifications.length}</div>
            <div className="text-[10px] sm:text-xs text-slate-300 font-medium">Official Notices</div>
          </button>
        </div>
      </div>

      {/* ------------------------------------------------------------- */}
      {/* 1.5 FLASH UPDATES TICKER (From active_latest_updates)         */}
      {/* ------------------------------------------------------------- */}
      {latestUpdates.length > 0 && (
        <div className="glass-panel p-3 rounded-2xl border border-sky-500/20 bg-sky-950/30 flex items-center gap-3 overflow-hidden shadow-md">
          <div className="flex items-center gap-1.5 px-2.5 py-1 rounded-xl bg-amber-500/20 text-amber-300 text-[11px] font-black tracking-wide shrink-0 border border-amber-500/30 animate-pulse">
            <Flame className="h-3.5 w-3.5 text-amber-400" />
            <span>FLASH UPDATES</span>
          </div>

          <div className="flex items-center gap-2 overflow-x-auto scrollbar-none py-0.5 text-xs text-slate-200">
            {latestUpdates.map((update) => (
              <a
                key={update.id}
                href={update.source_url || '#'}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-white/[0.06] hover:bg-white/[0.12] border border-white/10 shrink-0 text-slate-200 hover:text-white transition-all group"
              >
                <span className="text-[10px] font-bold px-1.5 py-0.5 rounded bg-sky-500/20 text-sky-300">
                  {update.category}
                </span>
                <span className="font-semibold text-xs truncate max-w-[280px]">
                  {update.title}
                </span>
                {update.badge && (
                  <span className="text-[9px] font-black uppercase px-1.5 py-0.2 rounded bg-amber-500/20 text-amber-300 border border-amber-500/30">
                    {update.badge}
                  </span>
                )}
                <ExternalLink className="h-3 w-3 text-slate-400 group-hover:text-sky-300 shrink-0" />
              </a>
            ))}
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* 2. GEMINI AI ACTION ENGINES: STUDY PLANNER & ELIGIBILITY      */}
      {/* ------------------------------------------------------------- */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
        {/* Card A: Target Exam AI Planner */}
        <div className="glass-panel p-4 rounded-3xl border border-sky-500/25 bg-gradient-to-br from-sky-950/40 to-slate-900/60 relative overflow-hidden flex flex-col justify-between gap-3 group hover:border-sky-400/40 transition-all">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <span className="p-2 rounded-xl bg-sky-500/20 text-sky-300 border border-sky-500/30">
                <Target className="h-4 w-4" />
              </span>
              <span className="text-[11px] font-bold uppercase tracking-wider text-sky-400">
                AI Target Exam Planner
              </span>
            </div>
            <h3 className="text-sm font-bold text-white leading-snug">
              Instant 30-Day Daily Routine & Focus Schedule
            </h3>
            <p className="text-xs text-slate-300">
              Select any live government job below and let Gemini AI curate a science-backed daily routine, weekly milestones, and active recall sprints.
            </p>
          </div>
          <button
            onClick={() => {
              if (jobs.length > 0) {
                handleOpenStudyPlanner(jobs[0]);
              }
            }}
            className="w-full py-2.5 px-3 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-white font-bold text-xs flex items-center justify-center gap-1.5 shadow-lg shadow-sky-500/20 transition-all cursor-pointer"
          >
            <Sparkles className="h-3.5 w-3.5" />
            <span>Launch AI Planner on Featured Vacancy</span>
          </button>
        </div>

        {/* Card B: Hinglish AI Eligibility Assistant */}
        <div className="glass-panel p-4 rounded-3xl border border-amber-500/25 bg-gradient-to-br from-amber-950/40 to-slate-900/60 relative overflow-hidden flex flex-col justify-between gap-3 group hover:border-amber-400/40 transition-all">
          <div className="space-y-1.5">
            <div className="flex items-center gap-2">
              <span className="p-2 rounded-xl bg-amber-500/20 text-amber-300 border border-amber-500/30">
                <Bot className="h-4 w-4" />
              </span>
              <span className="text-[11px] font-bold uppercase tracking-wider text-amber-400">
                Hinglish AI Eligibility Advisor
              </span>
            </div>
            <h3 className="text-sm font-bold text-white leading-snug">
              "Bhai meri 12th pass hai aur age 21 hai..."
            </h3>
            <p className="text-xs text-slate-300">
              Ask any qualification, age limit, category relaxation, or exam pattern question and get instant grounded answers from our live jobs database.
            </p>
          </div>
          <button
            onClick={() => setShowEligibilityModal(true)}
            className="w-full py-2.5 px-3 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 text-slate-950 font-bold text-xs flex items-center justify-center gap-1.5 shadow-lg shadow-amber-500/20 transition-all cursor-pointer"
          >
            <Bot className="h-3.5 w-3.5" />
            <span>Open Hinglish AI Eligibility Chat</span>
          </button>
        </div>
      </div>

      {/* ------------------------------------------------------------- */}
      {/* 3. PRIMARY MODULE TABS BAR                                    */}
      {/* ------------------------------------------------------------- */}
      <div className="flex items-center gap-1.5 p-1.5 rounded-2xl bg-white/[0.04] border border-white/10 overflow-x-auto scrollbar-none">
        <button
          onClick={() => setActiveTab('jobs')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer shrink-0 ${
            activeTab === 'jobs'
              ? 'bg-sky-500 text-slate-950 shadow-md shadow-sky-500/20'
              : 'text-slate-300 hover:bg-white/[0.06]'
          }`}
        >
          <Briefcase className="h-4 w-4" />
          <span>Live Vacancies</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-black/20 font-black">
            {jobs.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('admit_cards')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer shrink-0 ${
            activeTab === 'admit_cards'
              ? 'bg-amber-500 text-slate-950 shadow-md shadow-amber-500/20'
              : 'text-slate-300 hover:bg-white/[0.06]'
          }`}
        >
          <FileText className="h-4 w-4" />
          <span>Admit Cards & Slips</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-black/20 font-black">
            {admitCards.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('answer_keys')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer shrink-0 ${
            activeTab === 'answer_keys'
              ? 'bg-purple-500 text-white shadow-md shadow-purple-500/20'
              : 'text-slate-300 hover:bg-white/[0.06]'
          }`}
        >
          <Key className="h-4 w-4" />
          <span>Answer Keys</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-black/20 font-black">
            {answerKeys.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('results')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer shrink-0 ${
            activeTab === 'results'
              ? 'bg-emerald-500 text-slate-950 shadow-md shadow-emerald-500/20'
              : 'text-slate-300 hover:bg-white/[0.06]'
          }`}
        >
          <Award className="h-4 w-4" />
          <span>Declared Results</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-black/20 font-black">
            {results.length}
          </span>
        </button>

        <button
          onClick={() => setActiveTab('notifications')}
          className={`flex items-center gap-2 px-4 py-2 rounded-xl text-xs font-bold transition-all cursor-pointer shrink-0 ${
            activeTab === 'notifications'
              ? 'bg-rose-500 text-white shadow-md shadow-rose-500/20'
              : 'text-slate-300 hover:bg-white/[0.06]'
          }`}
        >
          <Bell className="h-4 w-4" />
          <span>Official Notices</span>
          <span className="px-1.5 py-0.2 rounded-full text-[10px] bg-black/20 font-black">
            {notifications.length}
          </span>
        </button>
      </div>

      {/* ------------------------------------------------------------- */}
      {/* 4. SEARCH & FILTER CONTROLS                                   */}
      {/* ------------------------------------------------------------- */}
      <div className="space-y-3">
        {/* Search bar */}
        <div className="relative">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 h-4 w-4 text-slate-400" />
          <input
            id="sarkari-search-input"
            type="text"
            placeholder={
              activeTab === 'jobs'
                ? 'Search by exam name, department, post, or qualification (e.g. SSC, Railway, 12th Pass)...'
                : activeTab === 'admit_cards'
                ? 'Search admit cards & hall tickets by exam or organization...'
                : activeTab === 'answer_keys'
                ? 'Search answer keys by exam name or organization...'
                : activeTab === 'results'
                ? 'Search declared results by exam or department...'
                : 'Search official notices and circulars...'
            }
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-white/[0.05] border border-white/10 text-white placeholder-slate-400 text-xs sm:text-sm focus:outline-none focus:border-sky-400 focus:ring-1 focus:ring-sky-400/50 transition-all"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3.5 top-1/2 -translate-y-1/2 text-slate-400 hover:text-white cursor-pointer"
            >
              <X className="h-4 w-4" />
            </button>
          )}
        </div>

        {/* Category filter chips (Only for Live Vacancies tab) */}
        {activeTab === 'jobs' && (
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none">
            {filterChips.map((chip) => {
              const isActive = activeFilter === chip.id;
              return (
                <button
                  key={chip.id}
                  onClick={() => setActiveFilter(chip.id)}
                  className={`px-3 py-1.5 rounded-xl text-xs font-semibold whitespace-nowrap transition-all cursor-pointer ${
                    isActive
                      ? 'bg-sky-500 text-slate-950 shadow-md shadow-sky-500/20 font-bold'
                      : 'bg-white/[0.05] text-slate-300 hover:bg-white/[0.1] border border-white/5'
                  }`}
                >
                  {chip.label}
                </button>
              );
            })}
          </div>
        )}
      </div>

      {/* ------------------------------------------------------------- */}
      {/* 5. TAB 1: LIVE VACANCIES FEED                                 */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'jobs' && (
        <div className="space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-300 px-1">
            <div className="flex items-center gap-2">
              <span className="font-bold text-white text-sm">Live Vacancies</span>
              <span className="px-2 py-0.5 rounded-full bg-white/10 text-slate-300 font-semibold text-[11px]">
                {filteredJobs.length} Available
              </span>
            </div>
            {syncStatus.lastSyncTime && (
              <span className="text-[11px] text-slate-400">
                Last Synced: {syncStatus.lastSyncTime}
              </span>
            )}
          </div>

          {isLoading ? (
            <div className="glass-panel p-12 rounded-3xl border border-white/10 text-center space-y-3">
              <RefreshCw className="h-8 w-8 text-sky-400 animate-spin mx-auto" />
              <div className="text-sm font-bold text-white">Fetching Verified Sarkari Vacancies...</div>
              <p className="text-xs text-slate-400 max-w-sm mx-auto">
                Connecting directly to StudyMate Sarkari database on Supabase & Render live scraper feed...
              </p>
            </div>
          ) : filteredJobs.length === 0 ? (
            <div className="glass-panel p-10 rounded-3xl border border-white/10 text-center space-y-3">
              <AlertCircle className="h-8 w-8 text-amber-400 mx-auto" />
              <div className="text-sm font-bold text-white">No Vacancies Matched Your Filter</div>
              <p className="text-xs text-slate-400">
                Try switching the category filter or clearing your search term.
              </p>
              <button
                onClick={() => {
                  setActiveFilter('All');
                  setSearchQuery('');
                }}
                className="px-4 py-2 rounded-xl bg-white/10 hover:bg-white/15 text-xs font-semibold text-white cursor-pointer"
              >
                Reset Filters
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {filteredJobs.map((job) => {
                const daysInfo = calculateDaysRemaining(job.last_date);
                const isBookmarked = bookmarkedIds.includes(job.id);
                const isApplied = appliedIds.includes(job.id);

                return (
                  <div
                    key={job.id}
                    className="glass-panel p-5 rounded-3xl border border-white/10 flex flex-col justify-between gap-4 hover:border-sky-500/35 transition-all relative overflow-hidden group shadow-lg"
                  >
                    <div>
                      {/* Top Row: Category tag, Scope tag, Last Date badge, Bookmark */}
                      <div className="flex items-start justify-between gap-2 mb-2">
                        <div className="flex items-center gap-1.5 flex-wrap">
                          <span className="text-[10px] font-bold px-2.5 py-0.5 rounded-full bg-sky-500/20 text-sky-300 border border-sky-500/30">
                            {job.category}
                          </span>
                          {job.scope && (
                            <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                              {job.scope === 'STATE' && job.state_name ? `STATE: ${job.state_name}` : job.scope}
                            </span>
                          )}
                          <span
                            className={`text-[10px] font-bold px-2.5 py-0.5 rounded-full border ${daysInfo.badgeClass}`}
                          >
                            {daysInfo.label}
                          </span>
                        </div>

                        <div className="flex items-center gap-1">
                          {onToggleBookmark && (
                            <button
                              onClick={() => onToggleBookmark(job.id)}
                              className={`p-1.5 rounded-xl border transition-colors cursor-pointer ${
                                isBookmarked
                                  ? 'bg-amber-500/20 text-amber-300 border-amber-500/30'
                                  : 'bg-white/[0.05] text-slate-400 border-white/5 hover:text-white'
                              }`}
                              title={isBookmarked ? 'Bookmarked' : 'Save to bookmarks'}
                            >
                              <Bookmark className={`h-3.5 w-3.5 ${isBookmarked ? 'fill-amber-400 text-amber-400' : ''}`} />
                            </button>
                          )}
                        </div>
                      </div>

                      {/* Job Title & Organization */}
                      <h3 className="text-sm sm:text-base font-bold text-white leading-snug line-clamp-2">
                        {job.title}
                      </h3>
                      <div className="flex items-center justify-between mt-1 text-xs">
                        <p className="text-sky-300 font-medium">
                          {job.organization_name || job.department || 'Govt Department'}
                        </p>
                        {job.advertisement_no && (
                          <span className="text-[10px] text-slate-400 font-mono">
                            Advt: {job.advertisement_no}
                          </span>
                        )}
                      </div>

                      {/* Snapshot Metadata Grid */}
                      <div className="grid grid-cols-2 gap-2 text-xs text-slate-300 pt-3 mt-3 border-t border-white/5">
                        <div className="flex items-center gap-1.5">
                          <Briefcase className="h-3.5 w-3.5 text-amber-400 shrink-0" />
                          <span className="font-semibold text-white truncate">
                            {job.total_vacancies}
                          </span>
                        </div>
                        <div className="flex items-center gap-1.5">
                          <Calendar className="h-3.5 w-3.5 text-sky-400 shrink-0" />
                          <span className="truncate">Last: {job.last_date || 'Check Notice'}</span>
                        </div>
                        <div className="flex items-center gap-1.5 col-span-2">
                          <GraduationCap className="h-3.5 w-3.5 text-emerald-400 shrink-0" />
                          <span className="truncate text-slate-300">{job.qualification}</span>
                        </div>
                        {job.salary && (
                          <div className="flex items-center gap-1.5 col-span-2 text-[11px] text-slate-400">
                            <Award className="h-3 w-3 text-amber-400 shrink-0" />
                            <span className="truncate">{job.salary}</span>
                          </div>
                        )}
                      </div>
                    </div>

                    {/* Action Bar */}
                    <div className="pt-2 border-t border-white/5 space-y-2">
                      <div className="flex items-center gap-2">
                        {/* 1-Tap Direct Apply */}
                        <a
                          href={job.apply_url}
                          target="_blank"
                          rel="noreferrer"
                          className="flex-1 py-2 px-3 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 hover:to-teal-500 text-slate-950 font-bold text-xs flex items-center justify-center gap-1.5 shadow-md shadow-emerald-500/20 transition-all cursor-pointer"
                        >
                          <ExternalLink className="h-3.5 w-3.5" />
                          <span>Direct Apply ↗</span>
                        </a>

                        {/* AI Target Exam Planner */}
                        <button
                          onClick={() => handleOpenStudyPlanner(job)}
                          className="py-2 px-3 rounded-xl bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 font-bold text-xs border border-sky-400/30 flex items-center justify-center gap-1.5 transition-all cursor-pointer"
                          title="Generate instant 30-day AI study routine"
                        >
                          <Target className="h-3.5 w-3.5 text-sky-400" />
                          <span>AI Plan</span>
                        </button>

                        {/* Details button */}
                        <button
                          onClick={() => setSelectedJob(job)}
                          className="py-2 px-2.5 rounded-xl bg-white/[0.06] hover:bg-white/[0.12] text-slate-200 text-xs font-semibold border border-white/10 cursor-pointer"
                          title="View complete eligibility and details"
                        >
                          Details
                        </button>
                      </div>

                      {/* Mark Applied & Official PDF row */}
                      <div className="flex items-center justify-between gap-2 text-xs">
                        {job.notification_pdf_url ? (
                          <a
                            href={job.notification_pdf_url}
                            target="_blank"
                            rel="noreferrer"
                            className="text-[11px] text-slate-400 hover:text-sky-300 flex items-center gap-1 underline underline-offset-2"
                          >
                            <FileText className="h-3 w-3" />
                            <span>Official Notice PDF ↗</span>
                          </a>
                        ) : (
                          <span className="text-[11px] text-slate-400">Verified Notification</span>
                        )}

                        {onToggleApplied && (
                          <button
                            onClick={() => onToggleApplied(job.id)}
                            className={`text-[11px] font-bold flex items-center gap-1 px-2 py-0.5 rounded-lg transition-all cursor-pointer ${
                              isApplied
                                ? 'text-emerald-300 bg-emerald-500/15 border border-emerald-500/30'
                                : 'text-slate-400 hover:text-white'
                            }`}
                          >
                            <CheckCircle2 className="h-3 w-3" />
                            <span>{isApplied ? 'Applied' : 'Mark as Applied'}</span>
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* 5. TAB 2: ADMIT CARDS & EXAM DATES                            */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'admit_cards' && (
        <div className="space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-300 px-1">
            <div className="flex items-center gap-2">
              <span className="font-bold text-white text-sm">Admit Cards & Hall Tickets</span>
              <span className="px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 font-semibold text-[11px] border border-amber-500/30">
                {filteredAdmitCards.length} Active
              </span>
            </div>
          </div>

          {filteredAdmitCards.length === 0 ? (
            <div className="glass-panel p-10 rounded-3xl border border-white/10 text-center space-y-3">
              <AlertCircle className="h-8 w-8 text-amber-400 mx-auto" />
              <div className="text-sm font-bold text-white">No Admit Cards Match Your Search</div>
              <p className="text-xs text-slate-400">Check back shortly or clear search terms.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
              {filteredAdmitCards.map((ac) => (
                <div
                  key={ac.id}
                  className="glass-panel p-4 rounded-3xl border border-white/10 hover:border-amber-500/35 transition-all flex flex-col justify-between gap-3 shadow-lg"
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-1 text-[11px] text-amber-300 font-semibold">
                      <span className="truncate">{ac.organization_name || 'Staff Selection / Board'}</span>
                      <span className="px-2 py-0.5 rounded-full bg-amber-500/20 border border-amber-500/30 text-[10px]">
                        {ac.release_date || 'Active'}
                      </span>
                    </div>

                    <h4 className="text-sm font-bold text-white leading-snug line-clamp-2">
                      {ac.title}
                    </h4>

                    {ac.advertisement_no && (
                      <div className="text-[10px] text-slate-400 font-mono">
                        Advt: {ac.advertisement_no}
                      </div>
                    )}

                    <div className="p-2.5 rounded-xl bg-white/[0.03] border border-white/5 space-y-1 text-xs">
                      <div className="flex items-center gap-1.5 text-slate-300">
                        <Calendar className="h-3.5 w-3.5 text-sky-400 shrink-0" />
                        <span>Exam Date: <strong className="text-white">{ac.exam_date || 'Upcoming'}</strong></span>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-2 pt-2 border-t border-white/5">
                    <a
                      href={ac.download_url}
                      target="_blank"
                      rel="noreferrer"
                      className="w-full py-2 rounded-xl bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 text-xs font-bold border border-amber-500/40 flex items-center justify-center gap-1.5 transition-all shadow-md"
                    >
                      <Download className="h-3.5 w-3.5" />
                      <span>Download Admit Card / Hall Ticket ↗</span>
                    </a>

                    {ac.city_slip_url && (
                      <a
                        href={ac.city_slip_url}
                        target="_blank"
                        rel="noreferrer"
                        className="w-full py-1.5 rounded-xl bg-white/[0.05] hover:bg-white/[0.1] text-slate-300 text-xs font-semibold flex items-center justify-center gap-1 transition-all"
                      >
                        <MapPin className="h-3 w-3 text-sky-400" />
                        <span>Check Exam City Intimation Slip ↗</span>
                      </a>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* 5. TAB 3: ANSWER KEYS & OBJECTION TRACKER                     */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'answer_keys' && (
        <div className="space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-300 px-1">
            <div className="flex items-center gap-2">
              <span className="font-bold text-white text-sm">Official Answer Keys & Objections</span>
              <span className="px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 font-semibold text-[11px] border border-purple-500/30">
                {filteredAnswerKeys.length} Available
              </span>
            </div>
          </div>

          {filteredAnswerKeys.length === 0 ? (
            <div className="glass-panel p-10 rounded-3xl border border-white/10 text-center space-y-3">
              <AlertCircle className="h-8 w-8 text-purple-400 mx-auto" />
              <div className="text-sm font-bold text-white">No Answer Keys Found</div>
              <p className="text-xs text-slate-400">All live answer keys from database will appear here.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
              {filteredAnswerKeys.map((ak) => (
                <div
                  key={ak.id}
                  className="glass-panel p-4 rounded-3xl border border-white/10 hover:border-purple-500/35 transition-all flex flex-col justify-between gap-3 shadow-lg"
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-1 text-[11px] text-purple-300 font-semibold">
                      <span className="truncate">{ak.organization_name || 'Staff Selection / Board'}</span>
                      <span className="px-2 py-0.5 rounded-full bg-purple-500/20 border border-purple-500/30 text-[10px]">
                        {ak.release_date || 'Released'}
                      </span>
                    </div>

                    <h4 className="text-sm font-bold text-white leading-snug line-clamp-2">
                      {ak.title}
                    </h4>

                    {ak.advertisement_no && (
                      <div className="text-[10px] text-slate-400 font-mono">
                        Advt: {ak.advertisement_no}
                      </div>
                    )}

                    <div className="p-2.5 rounded-xl bg-white/[0.03] border border-white/5 space-y-1 text-xs">
                      <div className="flex items-center gap-1.5 text-slate-300">
                        <Clock className="h-3.5 w-3.5 text-amber-400 shrink-0" />
                        <span>Objection Last Date: <strong className="text-amber-300">{ak.objection_last_date || 'Check Notice'}</strong></span>
                      </div>
                    </div>
                  </div>

                  <div className="space-y-2 pt-2 border-t border-white/5">
                    <a
                      href={ak.answer_key_url}
                      target="_blank"
                      rel="noreferrer"
                      className="w-full py-2 rounded-xl bg-purple-500/20 hover:bg-purple-500/30 text-purple-300 text-xs font-bold border border-purple-500/40 flex items-center justify-center gap-1.5 transition-all shadow-md"
                    >
                      <Download className="h-3.5 w-3.5" />
                      <span>Download Official Answer Key PDF ↗</span>
                    </a>

                    {ak.objection_url && (
                      <a
                        href={ak.objection_url}
                        target="_blank"
                        rel="noreferrer"
                        className="w-full py-1.5 rounded-xl bg-white/[0.05] hover:bg-white/[0.1] text-slate-300 text-xs font-semibold flex items-center justify-center gap-1 transition-all"
                      >
                        <HelpCircle className="h-3 w-3 text-amber-400" />
                        <span>Submit Answer Key Objection ↗</span>
                      </a>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* 5. TAB 4: DECLARED RESULTS & CUTOFFS                          */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'results' && (
        <div className="space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-300 px-1">
            <div className="flex items-center gap-2">
              <span className="font-bold text-white text-sm">Official Declared Results & Merit Lists</span>
              <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-semibold text-[11px] border border-emerald-500/30">
                {filteredResults.length} Available
              </span>
            </div>
          </div>

          {filteredResults.length === 0 ? (
            <div className="glass-panel p-10 rounded-3xl border border-white/10 text-center space-y-3">
              <AlertCircle className="h-8 w-8 text-emerald-400 mx-auto" />
              <div className="text-sm font-bold text-white">No Results Found</div>
              <p className="text-xs text-slate-400">Newly declared results will synchronize here automatically.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
              {filteredResults.map((res) => (
                <div
                  key={res.id}
                  className="glass-panel p-4 rounded-3xl border border-white/10 hover:border-emerald-500/35 transition-all flex flex-col justify-between gap-3 shadow-lg"
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-1 text-[11px] text-emerald-300 font-semibold">
                      <span className="truncate">{res.organization_name || 'Examination Board'}</span>
                      <span className="px-2 py-0.5 rounded-full bg-emerald-500/20 border border-emerald-500/30 text-[10px]">
                        {res.declared_date || 'Declared'}
                      </span>
                    </div>

                    <h4 className="text-sm font-bold text-white leading-snug line-clamp-2">
                      {res.title}
                    </h4>

                    {res.cutoff_details && (
                      <div className="p-2.5 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-xs text-emerald-200">
                        <strong className="block text-[10px] text-emerald-400 font-bold uppercase tracking-wider mb-0.5">
                          Cutoff Details:
                        </strong>
                        {res.cutoff_details}
                      </div>
                    )}
                  </div>

                  <div className="space-y-2 pt-2 border-t border-white/5">
                    <a
                      href={res.result_url}
                      target="_blank"
                      rel="noreferrer"
                      className="w-full py-2 rounded-xl bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 text-xs font-bold border border-emerald-500/40 flex items-center justify-center gap-1.5 transition-all shadow-md"
                    >
                      <ExternalLink className="h-3.5 w-3.5" />
                      <span>Check Official Result / Scorecard ↗</span>
                    </a>

                    {res.merit_list_url && (
                      <a
                        href={res.merit_list_url}
                        target="_blank"
                        rel="noreferrer"
                        className="w-full py-1.5 rounded-xl bg-white/[0.05] hover:bg-white/[0.1] text-slate-300 text-xs font-semibold flex items-center justify-center gap-1 transition-all"
                      >
                        <FileText className="h-3 w-3 text-emerald-400" />
                        <span>Download Merit List PDF ↗</span>
                      </a>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* 5. TAB 5: OFFICIAL NOTIFICATIONS & CIRCULARS                  */}
      {/* ------------------------------------------------------------- */}
      {activeTab === 'notifications' && (
        <div className="space-y-3">
          <div className="flex items-center justify-between text-xs text-slate-300 px-1">
            <div className="flex items-center gap-2">
              <span className="font-bold text-white text-sm">Official Notifications & Notices</span>
              <span className="px-2 py-0.5 rounded-full bg-rose-500/20 text-rose-300 font-semibold text-[11px] border border-rose-500/30">
                {filteredNotifications.length} Published
              </span>
            </div>
          </div>

          {filteredNotifications.length === 0 ? (
            <div className="glass-panel p-10 rounded-3xl border border-white/10 text-center space-y-3">
              <AlertCircle className="h-8 w-8 text-rose-400 mx-auto" />
              <div className="text-sm font-bold text-white">No Notices Match Your Search</div>
              <p className="text-xs text-slate-400">Government gazettes and public circulars will show here.</p>
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3.5">
              {filteredNotifications.map((notif) => (
                <div
                  key={notif.id}
                  className="glass-panel p-4 rounded-3xl border border-white/10 hover:border-rose-500/35 transition-all flex flex-col justify-between gap-3 shadow-lg"
                >
                  <div className="space-y-2">
                    <div className="flex items-center justify-between gap-1 text-[11px] text-rose-300 font-semibold">
                      <span className="truncate">{notif.organization_name || 'Govt Department'}</span>
                      <span className="px-2 py-0.5 rounded-full bg-rose-500/20 border border-rose-500/30 text-[10px]">
                        {notif.notification_type || 'NOTICE'}
                      </span>
                    </div>

                    <h4 className="text-sm font-bold text-white leading-snug line-clamp-2">
                      {notif.title}
                    </h4>

                    {notif.notification_no && (
                      <div className="text-[10px] text-slate-400 font-mono">
                        Ref No: {notif.notification_no}
                      </div>
                    )}

                    {notif.description && (
                      <p className="text-xs text-slate-300 line-clamp-2">
                        {notif.description}
                      </p>
                    )}

                    <div className="text-[11px] text-slate-400 flex items-center gap-1">
                      <Calendar className="h-3 w-3 text-sky-400" />
                      <span>Date: {notif.notification_date || 'Recent'}</span>
                    </div>
                  </div>

                  <div className="pt-2 border-t border-white/5">
                    <a
                      href={notif.pdf_url || notif.official_url}
                      target="_blank"
                      rel="noreferrer"
                      className="w-full py-2 rounded-xl bg-rose-500/20 hover:bg-rose-500/30 text-rose-300 text-xs font-bold border border-rose-500/40 flex items-center justify-center gap-1.5 transition-all shadow-md"
                    >
                      <Download className="h-3.5 w-3.5" />
                      <span>Download Official Notice PDF ↗</span>
                    </a>
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* 6. MODAL: AI TARGET EXAM 30-DAY STUDY PLANNER                 */}
      {/* ------------------------------------------------------------- */}
      {showPlannerModal && planningJob && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="w-full max-w-2xl max-h-[90vh] overflow-y-auto glass-panel p-6 rounded-3xl border border-sky-400/30 shadow-2xl space-y-4">
            <div className="flex items-start justify-between pb-3 border-b border-white/10">
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-sky-500/20 text-sky-300 border border-sky-500/30">
                    Gemini AI Strategy
                  </span>
                  <span className="text-xs text-slate-400">{planningJob.category}</span>
                </div>
                <h3 className="text-lg font-bold text-white leading-snug">
                  {planningJob.title} — 30-Day Daily Study Routine
                </h3>
                <p className="text-xs text-sky-300">{planningJob.organization_name}</p>
              </div>
              <button
                onClick={() => setShowPlannerModal(false)}
                className="text-slate-400 hover:text-white p-1 rounded-xl"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {isGeneratingPlan ? (
              <div className="py-12 text-center space-y-3">
                <RefreshCw className="h-8 w-8 text-sky-400 animate-spin mx-auto" />
                <div className="text-sm font-bold text-white">Synthesizing 30-Day Daily Study Routine...</div>
                <p className="text-xs text-slate-400 max-w-sm mx-auto">
                  Aligning syllabus modules, active recall drills, and high-yield PYQ patterns for {planningJob.title}...
                </p>
              </div>
            ) : generatedPlan ? (
              <div className="space-y-4 text-xs">
                {/* Motivational Quote */}
                <div className="p-3.5 rounded-2xl bg-sky-500/10 border border-sky-500/20 flex items-center gap-3">
                  <Flame className="h-5 w-5 text-amber-400 shrink-0" />
                  <p className="text-xs font-semibold text-sky-200 italic">
                    "{generatedPlan.summaryQuote}"
                  </p>
                </div>

                {/* Plan success notice */}
                {planSuccessNotice && (
                  <div className="p-3 rounded-2xl bg-emerald-500/20 border border-emerald-500/40 text-emerald-300 font-bold flex items-center gap-2 animate-in fade-in">
                    <CheckCircle2 className="h-4 w-4 shrink-0" />
                    <span>{planSuccessNotice}</span>
                  </div>
                )}

                {/* 4 Weekly Milestones */}
                <div className="space-y-2">
                  <div className="font-bold text-white text-xs flex items-center gap-1.5">
                    <Award className="h-4 w-4 text-amber-400" />
                    <span>4-Week Progress Milestones</span>
                  </div>
                  <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
                    {generatedPlan.weeklyMilestones.map((m) => (
                      <div
                        key={m.week}
                        className="p-2.5 rounded-xl bg-white/[0.04] border border-white/5 space-y-1"
                      >
                        <div className="text-[10px] font-bold text-sky-400">Week {m.week}</div>
                        <div className="text-[11px] font-semibold text-white line-clamp-2">{m.focus}</div>
                        <div className="text-[10px] text-amber-300 font-medium">Goal: {m.targetMockScore}</div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* General Strategy */}
                <div className="p-3 rounded-2xl bg-white/[0.04] border border-white/5 space-y-1">
                  <div className="font-bold text-white text-xs">🎯 High-Yield Exam Hall Strategy</div>
                  <p className="text-slate-300 leading-relaxed">{generatedPlan.generalStrategy}</p>
                </div>

                {/* Day-by-Day Routines Scroll */}
                <div className="space-y-2">
                  <div className="font-bold text-white text-xs flex items-center justify-between">
                    <span>Day-by-Day Focus Schedule (Day 1 to 30)</span>
                    <span className="text-[11px] text-slate-400">5-6 Hours / Day</span>
                  </div>
                  <div className="space-y-2 max-h-64 overflow-y-auto pr-1">
                    {generatedPlan.dailyRoutines.slice(0, 15).map((routine) => (
                      <div
                        key={routine.dayNumber}
                        className="p-2.5 rounded-xl bg-white/[0.03] border border-white/5 hover:border-sky-500/20 transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-2"
                      >
                        <div className="space-y-0.5">
                          <div className="flex items-center gap-2">
                            <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-sky-500/20 text-sky-300">
                              {routine.dayLabel}
                            </span>
                            <span className="font-bold text-white text-xs">{routine.focusSubject}</span>
                          </div>
                          <p className="text-[11px] text-slate-300">
                            {routine.highYieldTopics.join(' • ')}
                          </p>
                          <p className="text-[10px] text-amber-300">{routine.revisionAction}</p>
                        </div>

                        <div className="flex items-center gap-2 self-start sm:self-center shrink-0">
                          {onStartFocusSprint && (
                            <button
                              onClick={() => {
                                onStartFocusSprint(
                                  25,
                                  routine.focusSubject,
                                  routine.highYieldTopics[0] || 'Target Revision'
                                );
                                setShowPlannerModal(false);
                              }}
                              className="px-2.5 py-1 rounded-lg bg-emerald-500/20 hover:bg-emerald-500/30 text-emerald-300 font-bold text-[10px] border border-emerald-500/30 flex items-center gap-1 cursor-pointer"
                            >
                              <Flame className="h-3 w-3" />
                              <span>25m Sprint</span>
                            </button>
                          )}
                        </div>
                      </div>
                    ))}
                  </div>
                </div>

                {/* Modal Actions */}
                <div className="pt-2 flex flex-col sm:flex-row gap-2">
                  <button
                    onClick={handleApplyPlanToStudyMate}
                    className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 text-white font-bold text-xs flex items-center justify-center gap-1.5 shadow-lg shadow-sky-500/20 cursor-pointer"
                  >
                    <Target className="h-3.5 w-3.5" />
                    <span>Add to My Study Plan & Set as Active Exam</span>
                  </button>

                  <button
                    onClick={() => setShowPlannerModal(false)}
                    className="py-2.5 px-4 rounded-xl bg-white/10 hover:bg-white/15 text-slate-200 font-semibold text-xs cursor-pointer"
                  >
                    Done
                  </button>
                </div>
              </div>
            ) : null}
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* 7. MODAL: HINGLISH AI ELIGIBILITY ASSISTANT                    */}
      {/* ------------------------------------------------------------- */}
      {showEligibilityModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="w-full max-w-2xl h-[85vh] glass-panel p-5 rounded-3xl border border-amber-400/30 shadow-2xl flex flex-col justify-between">
            {/* Modal Header */}
            <div className="flex items-center justify-between pb-3 border-b border-white/10">
              <div className="flex items-center gap-2">
                <span className="p-2 rounded-xl bg-amber-500/20 text-amber-300 border border-amber-500/30">
                  <Bot className="h-4 w-4" />
                </span>
                <div>
                  <h3 className="text-base font-bold text-white">Hinglish AI Eligibility Advisor</h3>
                  <p className="text-[11px] text-slate-400">
                    Live Grounded in {jobs.length} Active Database Vacancies
                  </p>
                </div>
              </div>
              <button
                onClick={() => setShowEligibilityModal(false)}
                className="text-slate-400 hover:text-white p-1 rounded-xl"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            {/* Chat Messages */}
            <div className="flex-1 overflow-y-auto space-y-3 py-3 pr-1">
              {eligibilityChat.map((msg, idx) => (
                <div
                  key={idx}
                  className={`flex flex-col ${
                    msg.sender === 'user' ? 'items-end' : 'items-start'
                  }`}
                >
                  <div
                    className={`max-w-[88%] p-3.5 rounded-2xl text-xs leading-relaxed ${
                      msg.sender === 'user'
                        ? 'bg-sky-600 text-white rounded-br-none'
                        : 'glass-panel bg-slate-900/80 text-slate-200 border border-white/10 rounded-bl-none'
                    }`}
                  >
                    {msg.sender === 'assistant' ? (
                      <div className="prose prose-invert prose-xs max-w-none">
                        <ReactMarkdown>{msg.text}</ReactMarkdown>
                      </div>
                    ) : (
                      <p>{msg.text}</p>
                    )}
                  </div>
                  <span className="text-[10px] text-slate-400 mt-1 px-1">{msg.time}</span>
                </div>
              ))}

              {isAnsweringEligibility && (
                <div className="flex items-center gap-2 p-3 rounded-2xl bg-white/[0.04] text-xs text-amber-300 w-fit">
                  <RefreshCw className="h-3.5 w-3.5 animate-spin" />
                  <span>Checking live database & eligibility criteria...</span>
                </div>
              )}
            </div>

            {/* Quick Prompt Suggestions */}
            <div className="pt-2 border-t border-white/10 space-y-2">
              <div className="flex items-center gap-1.5 overflow-x-auto pb-1 scrollbar-none text-[11px]">
                <button
                  onClick={() =>
                    handleSendEligibilityQuestion(
                      'Bhai meri 12th pass hai aur age 21 hai, mere liye kaunsi live vacancy hai?'
                    )
                  }
                  className="px-2.5 py-1 rounded-lg bg-amber-500/15 hover:bg-amber-500/25 text-amber-300 border border-amber-500/30 whitespace-nowrap cursor-pointer"
                >
                  "Meri 12th pass + 21 yrs hai?"
                </button>
                <button
                  onClick={() =>
                    handleSendEligibilityQuestion(
                      'Graduate candidate hun with 50% marks, kya main Banking PO aur SSC CGL dono de sakta hun?'
                    )
                  }
                  className="px-2.5 py-1 rounded-lg bg-sky-500/15 hover:bg-sky-500/25 text-sky-300 border border-sky-500/30 whitespace-nowrap cursor-pointer"
                >
                  "Graduate with 50% marks?"
                </button>
                <button
                  onClick={() =>
                    handleSendEligibilityQuestion(
                      'Police aur Defence vacancies ke physical eligibility (Height & Running) rules samjha do.'
                    )
                  }
                  className="px-2.5 py-1 rounded-lg bg-emerald-500/15 hover:bg-emerald-500/25 text-emerald-300 border border-emerald-500/30 whitespace-nowrap cursor-pointer"
                >
                  "Police Physical Criteria?"
                </button>
              </div>

              {/* Chat Input */}
              <div className="flex items-center gap-2">
                <input
                  type="text"
                  placeholder="Apni qualification, age ya category poochhein (e.g. 10th pass, OBC, 22 yrs)..."
                  value={eligibilityPrompt}
                  onChange={(e) => setEligibilityPrompt(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter') handleSendEligibilityQuestion();
                  }}
                  className="flex-1 px-4 py-2.5 rounded-xl bg-white/[0.05] border border-white/10 text-white placeholder-slate-400 text-xs focus:outline-none focus:border-amber-400"
                />
                <button
                  onClick={() => handleSendEligibilityQuestion()}
                  disabled={!eligibilityPrompt.trim() || isAnsweringEligibility}
                  className="p-2.5 rounded-xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-bold cursor-pointer disabled:opacity-40 transition-all"
                >
                  <Send className="h-4 w-4" />
                </button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* 8. MODAL: DETAILED VACANCY VIEW                               */}
      {/* ------------------------------------------------------------- */}
      {selectedJob && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="w-full max-w-lg rounded-3xl glass-panel p-6 border border-white/15 shadow-2xl space-y-4">
            <div className="flex items-start justify-between pb-3 border-b border-white/10">
              <div>
                <span className="text-[10px] px-2.5 py-0.5 rounded-full bg-sky-500/20 text-sky-300 font-bold">
                  {selectedJob.category}
                </span>
                <h3 className="text-base font-bold text-white mt-1 leading-snug">
                  {selectedJob.title}
                </h3>
                <p className="text-xs text-sky-400 font-medium">
                  {selectedJob.organization_name || selectedJob.department}
                </p>
              </div>
              <button
                onClick={() => setSelectedJob(null)}
                className="text-slate-400 hover:text-white p-1"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="p-3 rounded-xl bg-white/[0.04] border border-white/5 space-y-1">
                <div className="text-slate-400">Total Vacancies</div>
                <div className="text-sm font-bold text-white">{selectedJob.total_vacancies}</div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="p-3 rounded-xl bg-white/[0.04] border border-white/5 space-y-1">
                  <div className="text-slate-400">Age Limit</div>
                  <div className="font-semibold text-white">{selectedJob.age_limit || '18 to 30/32 Years'}</div>
                </div>
                <div className="p-3 rounded-xl bg-white/[0.04] border border-white/5 space-y-1">
                  <div className="text-slate-400">Pay Scale</div>
                  <div className="font-semibold text-white">{selectedJob.salary || 'As per 7th CPC'}</div>
                </div>
              </div>

              <div className="p-3 rounded-xl bg-white/[0.04] border border-white/5 space-y-1">
                <div className="text-slate-400">Educational Qualification</div>
                <div className="font-semibold text-white">{selectedJob.qualification}</div>
              </div>

              <div className="p-3 rounded-xl bg-white/[0.04] border border-white/5 space-y-1">
                <div className="text-slate-400">Application Timeline</div>
                <div className="font-semibold text-white">
                  Last Date to Apply: {selectedJob.last_date}
                </div>
                {selectedJob.exam_date && (
                  <div className="font-semibold text-sky-300">
                    Tentative Exam Date: {selectedJob.exam_date}
                  </div>
                )}
              </div>

              {selectedJob.description && (
                <div className="p-3 rounded-xl bg-white/[0.04] border border-white/5 space-y-1">
                  <div className="text-slate-400">Overview</div>
                  <p className="text-slate-300 leading-relaxed">{selectedJob.description}</p>
                </div>
              )}
            </div>

            <div className="pt-2 flex gap-2">
              <a
                href={selectedJob.apply_url}
                target="_blank"
                rel="noreferrer"
                className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-400 text-slate-950 font-bold text-xs flex items-center justify-center gap-1.5 shadow-lg shadow-emerald-500/20"
              >
                <ExternalLink className="h-3.5 w-3.5" />
                <span>Visit Official Portal / Apply ↗</span>
              </a>

              <button
                onClick={() => {
                  setSelectedJob(null);
                  handleOpenStudyPlanner(selectedJob);
                }}
                className="py-2.5 px-4 rounded-xl bg-sky-500/20 hover:bg-sky-500/30 text-sky-300 font-bold text-xs border border-sky-400/30 flex items-center gap-1.5 cursor-pointer"
              >
                <Target className="h-3.5 w-3.5" />
                <span>AI 30-Day Plan</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
