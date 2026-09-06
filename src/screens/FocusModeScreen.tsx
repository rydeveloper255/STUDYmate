import React, { useState, useEffect, useRef } from 'react';
import { UserProfile, ExamInfo, FocusSession, DistractionShieldSettings } from '../types';
import { askNovaAssistant } from '../services/api';
import { triggerHaptic } from '../lib/haptics';
import {
  checkDndPermissionStatus,
  requestDndAccessSettings,
  activateDistractionShield,
  deactivateDistractionShield,
  recoverDistractionShieldOnAppLaunch,
  setDndPermissionGranted,
  DEFAULT_BLOCKED_APPS,
} from '../lib/distractionShield';
import { DistractionShieldPermissionModal } from '../components/DistractionShieldPermissionModal';
import { DistractionShieldSettingsCard } from '../components/DistractionShieldSettingsCard';
import { BlockedAppsManagerCard } from '../components/BlockedAppsManagerCard';
import { FocusDeterrentOverlay } from '../components/FocusDeterrentOverlay';
import {
  Shield,
  Play,
  Pause,
  RotateCcw,
  Sparkles,
  Volume2,
  Clock,
  CheckCircle2,
  Lock,
  Flame,
  Coffee,
  HelpCircle,
  Award,
  Sliders,
  History,
  Calendar,
  Zap,
  Smartphone,
  Check,
} from 'lucide-react';

interface FocusModeScreenProps {
  user: UserProfile;
  activeExam: ExamInfo;
  initialMinutes?: number;
  initialSubject?: string;
  initialTopic?: string;
  onCompleteSession: (session: FocusSession) => void;
  onAskNova: (prompt: string) => void;
}

export const FocusModeScreen: React.FC<FocusModeScreenProps> = ({
  user,
  activeExam,
  initialMinutes = 25,
  initialSubject,
  initialTopic,
  onCompleteSession,
  onAskNova,
}) => {
  // Navigation tab inside Focus Mode
  const [activeSubTab, setActiveSubTab] = useState<'timer' | 'settings' | 'history'>('timer');

  // Core Timer Configuration
  const [selectedDuration, setSelectedDuration] = useState<number>(initialMinutes);
  const [subject, setSubject] = useState<string>(initialSubject || activeExam.subjects[0] || 'General Studies');
  const [topic, setTopic] = useState<string>(initialTopic || 'High-Yield Concept Revision');
  const [customMinutesInput, setCustomMinutesInput] = useState<string>('');

  // Running Timer State
  const [secondsRemaining, setSecondsRemaining] = useState<number>(initialMinutes * 60);
  const [isActive, setIsActive] = useState<boolean>(false);
  const [ambientSound, setAmbientSound] = useState<'none' | 'alpha' | 'rain' | 'whitenoise'>('none');

  // Distraction Shield Configuration (Default: OFF per specification)
  const [shieldSettings, setShieldSettings] = useState<DistractionShieldSettings>(() => {
    return (
      user.distractionShield || {
        enabled: false,
        permissionGranted: false,
        autoRestore: true,
        allowPriorityAlarms: true,
        blockApps: true,
        blockedApps: DEFAULT_BLOCKED_APPS.filter((a) => a.isBlocked).map((a) => a.packageName),
        strictMode: false,
        autoFocus: false,
        autoFocusTime: '09:00',
      }
    );
  });

  const [hasDndPermission, setHasDndPermission] = useState<boolean>(() => checkDndPermissionStatus());
  const [showPermissionModal, setShowPermissionModal] = useState<boolean>(false);
  const [showDeterrentOverlay, setShowDeterrentOverlay] = useState<boolean>(false);
  const [distractionAttempts, setDistractionAttempts] = useState<number>(0);

  // Session Completed Summary Modal State
  const [completedSessionSummary, setCompletedSessionSummary] = useState<FocusSession | null>(null);

  // Recent focus history in local state
  const [focusHistory, setFocusHistory] = useState<FocusSession[]>(() => {
    const saved = localStorage.getItem('studymate_focus_history');
    return saved
      ? JSON.parse(saved)
      : [
          {
            id: 'hist_1',
            startTime: Date.now() - 3600000 * 2,
            durationMinutes: 45,
            subject: 'General Studies',
            topic: 'Constitutional Articles & Amendments',
            distractionAttemptsCount: 0,
            distractionShieldActive: true,
            dndRestored: true,
          },
          {
            id: 'hist_2',
            startTime: Date.now() - 3600000 * 24,
            durationMinutes: 25,
            subject: 'Quantitative Aptitude',
            topic: 'Speed, Time & Distance Shortcuts',
            distractionAttemptsCount: 1,
            distractionShieldActive: true,
            dndRestored: true,
          },
        ];
  });

  // Quick doubt in-focus
  const [quickDoubt, setQuickDoubt] = useState('');
  const [quickAnswer, setQuickAnswer] = useState<string | null>(null);
  const [isAskingDoubt, setIsAskingDoubt] = useState(false);

  // Web Audio Noise Generator Refs
  const audioCtxRef = useRef<AudioContext | null>(null);
  const noiseNodeRef = useRef<AudioNode | null>(null);
  const activeSessionIdRef = useRef<string | null>(null);

  // Recover any lingering DND state on mount
  useEffect(() => {
    recoverDistractionShieldOnAppLaunch();
    setHasDndPermission(checkDndPermissionStatus());
  }, []);

  // Update initial props if changed from navigation
  useEffect(() => {
    if (initialMinutes && !isActive) {
      setSelectedDuration(initialMinutes);
      setSecondsRemaining(initialMinutes * 60);
    }
    if (initialSubject) setSubject(initialSubject);
    if (initialTopic) setTopic(initialTopic);
  }, [initialMinutes, initialSubject, initialTopic, isActive]);

  // Save history to localStorage
  useEffect(() => {
    localStorage.setItem('studymate_focus_history', JSON.stringify(focusHistory));
  }, [focusHistory]);

  // Window blur / tab switch detector during active sprint
  useEffect(() => {
    const handleVisibilityChange = () => {
      if (document.hidden && isActive && shieldSettings.blockApps) {
        setDistractionAttempts((prev) => prev + 1);
        triggerHaptic('warning');
        setShowDeterrentOverlay(true);
      }
    };

    const handleWindowBlur = () => {
      if (isActive && shieldSettings.blockApps) {
        setDistractionAttempts((prev) => prev + 1);
        triggerHaptic('warning');
        setShowDeterrentOverlay(true);
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('blur', handleWindowBlur);

    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('blur', handleWindowBlur);
    };
  }, [isActive, shieldSettings.blockApps]);

  // Timer Tick and Natural Completion
  useEffect(() => {
    let interval: any = null;
    if (isActive && secondsRemaining > 0) {
      interval = setInterval(() => {
        setSecondsRemaining((sec) => sec - 1);
      }, 1000);
    } else if (isActive && secondsRemaining === 0) {
      handleCompleteTimer();
    }
    return () => clearInterval(interval);
  }, [isActive, secondsRemaining]);

  // Natural Session Completion
  const handleCompleteTimer = async () => {
    setIsActive(false);
    stopAmbient();
    triggerHaptic('success');

    // Deactivate Distraction Shield and restore DND state
    if (shieldSettings.enabled && shieldSettings.autoRestore) {
      await deactivateDistractionShield('COMPLETED');
    }

    const completedSess: FocusSession = {
      id: `sess_${Date.now()}`,
      startTime: Date.now() - selectedDuration * 60 * 1000,
      subject,
      topic,
      durationMinutes: selectedDuration,
      distractionAttemptsCount: distractionAttempts,
      distractionShieldActive: shieldSettings.enabled,
      dndRestored: shieldSettings.enabled && shieldSettings.autoRestore,
    };

    setFocusHistory((prev) => [completedSess, ...prev]);
    setCompletedSessionSummary(completedSess);
    onCompleteSession(completedSess);
    activeSessionIdRef.current = null;
  };

  // Start Timer logic with Distraction Shield Guard
  const handleStartTimer = async () => {
    // If Distraction Shield is enabled but permission is missing, prompt user
    if (shieldSettings.enabled && !hasDndPermission) {
      setShowPermissionModal(true);
      return;
    }

    await startSprintExecution();
  };

  const startSprintExecution = async () => {
    setShowPermissionModal(false);
    const sessionId = `sprint_${Date.now()}`;
    activeSessionIdRef.current = sessionId;
    setDistractionAttempts(0);

    // If Distraction Shield is enabled, activate Android DND Priority Filter
    if (shieldSettings.enabled) {
      await activateDistractionShield(sessionId, {
        allowPriorityAlarms: shieldSettings.allowPriorityAlarms,
      });
    }

    setIsActive(true);
    triggerHaptic('medium');
  };

  const handlePauseTimer = () => {
    if (shieldSettings.strictMode && isActive) {
      triggerHaptic('warning');
      const confirmed = window.confirm(
        'Strict Mode is active! Pausing interrupts your deep focus momentum. Are you sure?'
      );
      if (!confirmed) return;
    }
    setIsActive(false);
  };

  const handleResetTimer = async () => {
    if (shieldSettings.strictMode && isActive) {
      triggerHaptic('warning');
      const confirmed = window.confirm(
        'Strict Mode is active! Resetting will abort your ongoing study sprint. Confirm reset?'
      );
      if (!confirmed) return;
    }

    setIsActive(false);
    setSecondsRemaining(selectedDuration * 60);
    stopAmbient();
    setAmbientSound('none');

    // Restore previous DND state on reset/manual stop
    if (shieldSettings.enabled) {
      await deactivateDistractionShield('MANUAL_STOP');
    }
    activeSessionIdRef.current = null;
  };

  // Permission Flow Handlers
  const handleGrantPermission = async () => {
    const res = await requestDndAccessSettings();
    setHasDndPermission(res.granted);
    setDndPermissionGranted(res.granted);
    setShowPermissionModal(false);
    await startSprintExecution();
  };

  const handleContinueWithoutShield = async () => {
    setShowPermissionModal(false);
    await startSprintExecution();
  };

  // Ambient Audio Synthesizer
  const toggleAmbientSound = (type: 'none' | 'alpha' | 'rain' | 'whitenoise') => {
    if (ambientSound === type || type === 'none') {
      stopAmbient();
      setAmbientSound('none');
      return;
    }

    stopAmbient();
    setAmbientSound(type);

    try {
      const AudioCtx = window.AudioContext || (window as any).webkitAudioContext;
      if (!AudioCtx) return;
      const ctx = new AudioCtx();
      audioCtxRef.current = ctx;

      const bufferSize = ctx.sampleRate * 2;
      const buffer = ctx.createBuffer(1, bufferSize, ctx.sampleRate);
      const data = buffer.getChannelData(0);

      if (type === 'alpha') {
        const osc = ctx.createOscillator();
        const gain = ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(200, ctx.currentTime);
        gain.gain.setValueAtTime(0.08, ctx.currentTime);
        osc.connect(gain);
        gain.connect(ctx.destination);
        osc.start();
        noiseNodeRef.current = osc;
      } else {
        for (let i = 0; i < bufferSize; i++) {
          data[i] = Math.random() * 2 - 1;
        }
        const noiseSource = ctx.createBufferSource();
        noiseSource.buffer = buffer;
        noiseSource.loop = true;

        const filter = ctx.createBiquadFilter();
        filter.type = type === 'rain' ? 'lowpass' : 'bandpass';
        filter.frequency.setValueAtTime(type === 'rain' ? 800 : 1200, ctx.currentTime);

        const gainNode = ctx.createGain();
        gainNode.gain.setValueAtTime(0.05, ctx.currentTime);

        noiseSource.connect(filter);
        filter.connect(gainNode);
        gainNode.connect(ctx.destination);
        noiseSource.start();
        noiseNodeRef.current = noiseSource;
      }
    } catch (e) {
      console.warn('AudioContext not permitted in this context', e);
    }
  };

  const stopAmbient = () => {
    if (audioCtxRef.current) {
      try {
        audioCtxRef.current.close();
      } catch (e) {}
      audioCtxRef.current = null;
    }
  };

  const handleQuickDoubtSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!quickDoubt.trim()) return;
    setIsAskingDoubt(true);
    setQuickAnswer(null);

    try {
      const res = await askNovaAssistant(
        `[IN-FOCUS DOUBT]: Please provide a 2-sentence crisp answer to: ${quickDoubt}`,
        [],
        { studentName: user.name, targetExam: activeExam.name, subject, topic },
        {
          useBossGreeting: false,
          voiceEnabled: false,
          memoryEnabled: false,
          thinkingMode: false,
          selectedPersona: 'Empathetic Socratic Tutor',
        }
      );
      setQuickAnswer(res.replyMarkdown);
    } catch (e) {
      setQuickAnswer('Focus session active. Remember core principles and keep momentum high!');
    } finally {
      setIsAskingDoubt(false);
    }
  };

  const handleApplyCustomDuration = () => {
    const mins = parseInt(customMinutesInput, 10);
    if (!isNaN(mins) && mins > 0 && mins <= 180) {
      setSelectedDuration(mins);
      setSecondsRemaining(mins * 60);
      setCustomMinutesInput('');
      triggerHaptic('light');
    }
  };

  const minutes = Math.floor(secondsRemaining / 60);
  const seconds = secondsRemaining % 60;
  const progressPercent = Math.round(
    ((selectedDuration * 60 - secondsRemaining) / (selectedDuration * 60)) * 100
  );

  return (
    <div className="max-w-3xl mx-auto space-y-6 pb-28 animate-in fade-in duration-300">
      {/* Sub Tab Navigation */}
      <div className="flex items-center justify-between gap-2 p-1.5 rounded-2xl bg-slate-900/60 border border-white/10 backdrop-blur-md">
        <button
          id="tab-focus-timer"
          type="button"
          onClick={() => {
            triggerHaptic('light');
            setActiveSubTab('timer');
          }}
          className={`flex-1 py-2 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 cursor-pointer ${
            activeSubTab === 'timer'
              ? 'bg-sky-500 text-slate-950 shadow-md shadow-sky-500/20'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Clock className="h-3.5 w-3.5" />
          <span>Sprint Timer</span>
        </button>

        <button
          id="tab-focus-settings"
          type="button"
          onClick={() => {
            triggerHaptic('light');
            setActiveSubTab('settings');
          }}
          className={`flex-1 py-2 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 cursor-pointer ${
            activeSubTab === 'settings'
              ? 'bg-sky-500 text-slate-950 shadow-md shadow-sky-500/20'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <Shield className="h-3.5 w-3.5" />
          <span>Shield & Blocker</span>
          {shieldSettings.enabled && (
            <span className="h-1.5 w-1.5 rounded-full bg-emerald-400" />
          )}
        </button>

        <button
          id="tab-focus-history"
          type="button"
          onClick={() => {
            triggerHaptic('light');
            setActiveSubTab('history');
          }}
          className={`flex-1 py-2 px-3 rounded-xl text-xs font-bold transition-all flex items-center justify-center gap-2 cursor-pointer ${
            activeSubTab === 'history'
              ? 'bg-sky-500 text-slate-950 shadow-md shadow-sky-500/20'
              : 'text-slate-400 hover:text-slate-200'
          }`}
        >
          <History className="h-3.5 w-3.5" />
          <span>History ({focusHistory.length})</span>
        </button>
      </div>

      {/* ------------------------------------------------------------- */}
      {/* TAB 1: SPRINT TIMER VIEW */}
      {/* ------------------------------------------------------------- */}
      {activeSubTab === 'timer' && (
        <div className="space-y-6">
          {/* Main Focus Shield Dial Card */}
          <div className="p-6 sm:p-8 rounded-3xl glass-panel border border-sky-500/30 text-center space-y-6 relative overflow-hidden shadow-2xl bg-gradient-to-b from-slate-900 via-sky-950/20 to-slate-900">
            {/* Top Bar inside Dial Card */}
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-xs font-bold text-sky-400">
                <Shield className="h-4 w-4" />
                <span>Focus Shield & Pomodoro</span>
              </div>

              <div className="flex items-center gap-2">
                {shieldSettings.enabled && (
                  <span className="text-[10px] px-2.5 py-1 rounded-full bg-emerald-500/20 text-emerald-300 font-bold flex items-center gap-1 border border-emerald-500/30">
                    <Shield className="h-3 w-3" />
                    DND Shield ON
                  </span>
                )}
                <span className="text-[10px] px-2.5 py-1 rounded-full bg-amber-500/20 text-amber-300 font-bold flex items-center gap-1 border border-amber-500/30">
                  <Flame className="h-3 w-3 fill-amber-400" />
                  {user.streakDays} Day Streak
                </span>
              </div>
            </div>

            {/* Current Sprint Subject & Topic Selector */}
            {!isActive ? (
              <div className="space-y-2 max-w-md mx-auto">
                <div className="grid grid-cols-2 gap-2 text-left">
                  <div>
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                      Subject
                    </label>
                    <select
                      value={subject}
                      onChange={(e) => setSubject(e.target.value)}
                      className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-white/10 text-xs text-white outline-none"
                    >
                      {activeExam.subjects.map((sub) => (
                        <option key={sub} value={sub}>
                          {sub}
                        </option>
                      ))}
                    </select>
                  </div>
                  <div>
                    <label className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block mb-1">
                      Target Topic
                    </label>
                    <input
                      type="text"
                      value={topic}
                      onChange={(e) => setTopic(e.target.value)}
                      placeholder="e.g. Concept Revision"
                      className="w-full px-3 py-2 rounded-xl bg-slate-950 border border-white/10 text-xs text-white outline-none"
                    />
                  </div>
                </div>
              </div>
            ) : (
              <div>
                <h2 className="text-xl sm:text-2xl font-extrabold text-white">{topic}</h2>
                <p className="text-xs text-slate-400 mt-0.5">
                  {subject} • {activeExam.shortName}
                </p>
              </div>
            )}

            {/* Giant Timer Dial */}
            <div className="relative flex items-center justify-center py-2">
              <div className="relative h-56 w-56 sm:h-64 sm:w-64 rounded-full border-4 border-slate-800 flex items-center justify-center">
                {/* Animated Ring */}
                <svg className="absolute inset-0 h-full w-full -rotate-90">
                  <circle
                    cx="50%"
                    cy="50%"
                    r="45%"
                    className="stroke-sky-500 fill-none stroke-[6px] transition-all duration-1000"
                    strokeDasharray="628"
                    strokeDashoffset={628 - (628 * progressPercent) / 100}
                    strokeLinecap="round"
                  />
                </svg>

                <div className="text-center">
                  <div className="text-4xl sm:text-5xl font-extrabold text-white font-mono tracking-tight">
                    {String(minutes).padStart(2, '0')}:{String(seconds).padStart(2, '0')}
                  </div>
                  <div className="text-xs text-slate-400 mt-1 flex items-center justify-center gap-1.5">
                    {isActive ? (
                      <>
                        <span className="h-2 w-2 rounded-full bg-emerald-400 animate-ping" />
                        <span>Deep Sprint in Progress</span>
                      </>
                    ) : (
                      <span>Ready to begin</span>
                    )}
                  </div>
                  {shieldSettings.strictMode && isActive && (
                    <div className="text-[10px] text-rose-300 font-bold flex items-center justify-center gap-1 mt-1">
                      <Lock className="h-3 w-3" />
                      <span>Strict Lock Active</span>
                    </div>
                  )}
                </div>
              </div>
            </div>

            {/* Duration Selectors */}
            {!isActive && (
              <div className="space-y-3">
                <div className="flex justify-center flex-wrap gap-2">
                  {[15, 25, 45, 60].map((dur) => (
                    <button
                      key={dur}
                      onClick={() => {
                        setSelectedDuration(dur);
                        setSecondsRemaining(dur * 60);
                      }}
                      className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                        selectedDuration === dur
                          ? 'bg-sky-500/20 text-sky-300 border border-sky-400 shadow-sm'
                          : 'bg-white/[0.05] text-slate-400 hover:text-white border border-white/10'
                      }`}
                    >
                      {dur}m
                    </button>
                  ))}
                </div>

                {/* Custom Minutes Input */}
                <div className="flex items-center justify-center gap-2 max-w-xs mx-auto">
                  <input
                    type="number"
                    min="1"
                    max="180"
                    value={customMinutesInput}
                    onChange={(e) => setCustomMinutesInput(e.target.value)}
                    placeholder="Custom mins"
                    className="w-28 px-3 py-1 rounded-xl bg-slate-950 border border-white/10 text-xs text-white text-center outline-none"
                  />
                  <button
                    type="button"
                    onClick={handleApplyCustomDuration}
                    className="px-3 py-1 rounded-xl bg-white/10 hover:bg-white/20 text-xs text-white font-semibold cursor-pointer"
                  >
                    Set
                  </button>
                </div>
              </div>
            )}

            {/* Timer Control Buttons */}
            <div className="flex items-center justify-center gap-3 pt-2">
              {!isActive ? (
                <button
                  id="btn-start-focus-sprint"
                  onClick={handleStartTimer}
                  className="px-8 py-3.5 rounded-2xl bg-gradient-to-r from-sky-400 to-sky-500 hover:from-sky-300 text-slate-950 font-extrabold text-sm shadow-xl shadow-sky-500/25 flex items-center gap-2 cursor-pointer transition-all hover:scale-[1.02]"
                >
                  <Play className="h-4 w-4 fill-slate-950" />
                  <span>Start Focus Sprint</span>
                </button>
              ) : (
                <button
                  id="btn-pause-focus-sprint"
                  onClick={handlePauseTimer}
                  className="px-8 py-3.5 rounded-2xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-extrabold text-sm shadow-xl shadow-amber-500/25 flex items-center gap-2 cursor-pointer transition-all"
                >
                  <Pause className="h-4 w-4 fill-slate-950" />
                  <span>Pause Sprint</span>
                </button>
              )}

              <button
                id="btn-reset-focus-timer"
                onClick={handleResetTimer}
                className="p-3.5 rounded-2xl bg-white/[0.08] hover:bg-white/[0.14] text-slate-300 hover:text-white border border-white/10 cursor-pointer transition-all"
                title="Reset / Abort Sprint"
              >
                <RotateCcw className="h-4 w-4" />
              </button>
            </div>
          </div>

          {/* Ambient Audio Synthesizer */}
          <div className="p-4 sm:p-5 rounded-2xl glass-card border border-white/10 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Volume2 className="h-4 w-4 text-sky-400" />
                <span className="text-xs font-bold text-white">Ambient Focus Soundscape</span>
              </div>
              {ambientSound !== 'none' && (
                <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-semibold animate-pulse">
                  Playing {ambientSound}
                </span>
              )}
            </div>

            <div className="grid grid-cols-4 gap-2">
              {(
                [
                  { key: 'none', label: 'Mute' },
                  { key: 'alpha', label: 'Alpha 10Hz' },
                  { key: 'rain', label: 'Rain Storm' },
                  { key: 'whitenoise', label: 'White Noise' },
                ] as const
              ).map((item) => (
                <button
                  key={item.key}
                  type="button"
                  onClick={() => toggleAmbientSound(item.key)}
                  className={`p-2.5 rounded-xl text-center text-xs font-semibold transition-all border cursor-pointer ${
                    ambientSound === item.key
                      ? 'bg-sky-500/20 text-sky-300 border-sky-400 font-bold'
                      : 'bg-white/[0.04] text-slate-400 border-white/5 hover:text-white'
                  }`}
                >
                  {item.label}
                </button>
              ))}
            </div>
          </div>

          {/* In-Focus Instant Doubt Solver */}
          <div className="p-5 rounded-2xl glass-card border border-sky-500/20 space-y-3">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Sparkles className="h-4 w-4 text-sky-400" />
                <span className="text-xs font-bold text-white">Ask Quick In-Focus Doubt (Zero Distraction)</span>
              </div>
              <button
                type="button"
                onClick={() => onAskNova(`Focus session on ${topic} (${subject}) me help chahiye`)}
                className="text-[10px] text-sky-400 hover:text-sky-300 font-semibold cursor-pointer"
              >
                Open Full Nova →
              </button>
            </div>

            <form onSubmit={handleQuickDoubtSubmit} className="flex gap-2">
              <input
                type="text"
                value={quickDoubt}
                onChange={(e) => setQuickDoubt(e.target.value)}
                placeholder="e.g., What is Article 32 vs 226 scope?"
                className="flex-1 px-3.5 py-2 rounded-xl bg-slate-950 border border-white/10 text-xs text-white placeholder:text-slate-500 outline-none"
              />
              <button
                type="submit"
                disabled={isAskingDoubt || !quickDoubt.trim()}
                className="px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-400 text-slate-950 font-bold text-xs cursor-pointer disabled:opacity-50 transition-all"
              >
                {isAskingDoubt ? '...' : 'Ask'}
              </button>
            </form>

            {quickAnswer && (
              <div className="p-3.5 rounded-xl bg-sky-500/10 border border-sky-500/25 text-xs text-sky-200 leading-relaxed animate-in fade-in">
                {quickAnswer}
              </div>
            )}
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* TAB 2: DISTRACTION SHIELD & BLOCKER SETTINGS */}
      {/* ------------------------------------------------------------- */}
      {activeSubTab === 'settings' && (
        <div className="space-y-5 animate-in fade-in duration-200">
          {/* 1. Distraction Shield DND Card */}
          <DistractionShieldSettingsCard
            settings={shieldSettings}
            hasPermission={hasDndPermission}
            isActiveSprint={isActive}
            onToggleShield={(enabled) => {
              setShieldSettings((prev) => ({ ...prev, enabled }));
            }}
            onRequestPermission={async () => {
              const res = await requestDndAccessSettings();
              setHasDndPermission(res.granted);
              setDndPermissionGranted(res.granted);
            }}
            onUpdateSettings={(updated) => {
              setShieldSettings((prev) => ({ ...prev, ...updated }));
            }}
          />

          {/* 2. Block Distracting Apps Card */}
          <BlockedAppsManagerCard
            blockAppsEnabled={shieldSettings.blockApps}
            blockedAppList={shieldSettings.blockedApps}
            onToggleBlockApps={(enabled) => {
              setShieldSettings((prev) => ({ ...prev, blockApps: enabled }));
            }}
            onToggleAppBlocked={(pkg) => {
              setShieldSettings((prev) => {
                const isBlocked = prev.blockedApps.includes(pkg);
                return {
                  ...prev,
                  blockedApps: isBlocked
                    ? prev.blockedApps.filter((p) => p !== pkg)
                    : [...prev.blockedApps, pkg],
                };
              });
            }}
            onSelectAll={() => {
              setShieldSettings((prev) => ({
                ...prev,
                blockedApps: DEFAULT_BLOCKED_APPS.map((a) => a.packageName),
              }));
            }}
            onDeselectAll={() => {
              setShieldSettings((prev) => ({ ...prev, blockedApps: [] }));
            }}
          />

          {/* 3. Strict Mode & Auto Focus Card */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            {/* Strict Mode */}
            <div className="p-5 rounded-3xl bg-slate-900/50 border border-white/10 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 rounded-xl bg-amber-500/20 text-amber-400 border border-amber-500/30">
                    <Lock className="h-4 w-4" />
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-white">Strict Mode</h4>
                    <p className="text-[10px] text-slate-400">Prevents premature quit</p>
                  </div>
                </div>

                <input
                  type="checkbox"
                  checked={shieldSettings.strictMode}
                  onChange={(e) => {
                    triggerHaptic('light');
                    setShieldSettings((prev) => ({ ...prev, strictMode: e.target.checked }));
                  }}
                  className="rounded border-white/20 text-amber-500 focus:ring-amber-500 h-4 w-4 cursor-pointer"
                />
              </div>
              <p className="text-[11px] text-slate-400 leading-relaxed">
                Locks down timer exit buttons and gives haptic warnings if you try to abort your sprint.
              </p>
            </div>

            {/* Auto Focus Mode */}
            <div className="p-5 rounded-3xl bg-slate-900/50 border border-white/10 space-y-3">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2.5">
                  <div className="p-2 rounded-xl bg-sky-500/20 text-sky-400 border border-sky-500/30">
                    <Zap className="h-4 w-4" />
                  </div>
                  <div>
                    <h4 className="text-xs font-bold text-white">Auto Focus Mode</h4>
                    <p className="text-[10px] text-slate-400">Scheduled study time</p>
                  </div>
                </div>

                <input
                  type="checkbox"
                  checked={shieldSettings.autoFocus}
                  onChange={(e) => {
                    triggerHaptic('light');
                    setShieldSettings((prev) => ({ ...prev, autoFocus: e.target.checked }));
                  }}
                  className="rounded border-white/20 text-sky-500 focus:ring-sky-500 h-4 w-4 cursor-pointer"
                />
              </div>
              {shieldSettings.autoFocus && (
                <div className="flex items-center justify-between pt-1">
                  <span className="text-[11px] text-slate-300">Daily Study Sprint Time:</span>
                  <input
                    type="time"
                    value={shieldSettings.autoFocusTime}
                    onChange={(e) => {
                      setShieldSettings((prev) => ({ ...prev, autoFocusTime: e.target.value }));
                    }}
                    className="px-2 py-1 rounded-lg bg-slate-950 border border-white/10 text-xs text-white outline-none"
                  />
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------- */}
      {/* TAB 3: SPRINT HISTORY & STATS */}
      {/* ------------------------------------------------------------- */}
      {activeSubTab === 'history' && (
        <div className="space-y-4 animate-in fade-in duration-200">
          <div className="grid grid-cols-3 gap-3">
            <div className="p-4 rounded-2xl bg-slate-900/60 border border-white/10 text-center">
              <div className="text-xl font-extrabold text-white">
                {focusHistory.reduce((acc, h) => acc + h.durationMinutes, 0)}m
              </div>
              <div className="text-[10px] text-slate-400 mt-0.5">Total Focus</div>
            </div>
            <div className="p-4 rounded-2xl bg-slate-900/60 border border-white/10 text-center">
              <div className="text-xl font-extrabold text-sky-400">{focusHistory.length}</div>
              <div className="text-[10px] text-slate-400 mt-0.5">Completed Sprints</div>
            </div>
            <div className="p-4 rounded-2xl bg-slate-900/60 border border-white/10 text-center">
              <div className="text-xl font-extrabold text-emerald-400">
                {focusHistory.filter((h) => (h.distractionAttemptsCount || 0) === 0).length}
              </div>
              <div className="text-[10px] text-slate-400 mt-0.5">Zero-Distraction Sprints</div>
            </div>
          </div>

          <div className="p-5 rounded-3xl bg-slate-900/50 border border-white/10 space-y-3">
            <h4 className="text-xs font-bold text-white uppercase tracking-wider">Recent Sprints</h4>

            {focusHistory.length === 0 ? (
              <div className="text-center py-8 text-xs text-slate-500">
                No focus sprints logged yet. Start your first sprint today!
              </div>
            ) : (
              <div className="space-y-2">
                {focusHistory.map((item) => (
                  <div
                    key={item.id}
                    className="p-3.5 rounded-2xl bg-slate-950/60 border border-white/5 flex items-center justify-between text-xs"
                  >
                    <div>
                      <div className="font-bold text-white">{item.topic}</div>
                      <div className="text-[11px] text-slate-400 mt-0.5">
                        {item.subject} • {item.durationMinutes} minutes
                      </div>
                    </div>
                    <div className="text-right space-y-1">
                      {item.distractionShieldActive && (
                        <span className="text-[10px] px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 font-bold border border-emerald-500/30 inline-flex items-center gap-1">
                          <Shield className="h-2.5 w-2.5" />
                          Shield Protected
                        </span>
                      )}
                      <div className="text-[10px] text-slate-500">
                        {item.startTime ? new Date(item.startTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : 'Recent'}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Permission Modal */}
      <DistractionShieldPermissionModal
        isOpen={showPermissionModal}
        onGrantPermission={handleGrantPermission}
        onContinueWithoutShield={handleContinueWithoutShield}
        onClose={() => setShowPermissionModal(false)}
      />

      {/* Distraction Deterrent Blur Overlay */}
      <FocusDeterrentOverlay
        isOpen={showDeterrentOverlay}
        topic={topic}
        subject={subject}
        streakDays={user.streakDays}
        distractionAttempts={distractionAttempts}
        onReturnToStudy={() => setShowDeterrentOverlay(false)}
        onGiveUp={() => {
          setShowDeterrentOverlay(false);
          handleResetTimer();
        }}
        isStrictMode={shieldSettings.strictMode}
      />

      {/* Session Completed Summary Banner Modal */}
      {completedSessionSummary && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/80 backdrop-blur-md animate-in fade-in duration-200">
          <div className="w-full max-w-md p-6 rounded-3xl glass-panel border border-emerald-500/40 shadow-2xl bg-gradient-to-b from-slate-900 via-emerald-950/20 to-slate-950 text-center space-y-5">
            <div className="mx-auto w-14 h-14 rounded-3xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shadow-lg shadow-emerald-500/20">
              <Award className="h-7 w-7" />
            </div>

            <div>
              <span className="text-[10px] px-3 py-1 rounded-full bg-emerald-500/20 text-emerald-300 font-bold border border-emerald-500/30 uppercase tracking-wider">
                Sprint Accomplished!
              </span>
              <h3 className="text-xl font-black text-white mt-3">{completedSessionSummary.durationMinutes} Minutes Focused</h3>
              <p className="text-xs text-slate-400 mt-1">
                {completedSessionSummary.topic} ({completedSessionSummary.subject})
              </p>
            </div>

            <div className="p-4 rounded-2xl bg-slate-950/80 border border-white/5 space-y-2 text-left text-xs">
              <div className="flex items-center justify-between text-slate-300">
                <span>Distraction Attempts Prevented:</span>
                <strong className="text-white">{completedSessionSummary.distractionAttemptsCount || 0}</strong>
              </div>
              {completedSessionSummary.distractionShieldActive && (
                <div className="flex items-center gap-2 text-emerald-400 text-[11px] pt-1 border-t border-white/5">
                  <CheckCircle2 className="h-4 w-4 shrink-0" />
                  <span>Android DND restored to previous settings safely.</span>
                </div>
              )}
            </div>

            <button
              onClick={() => {
                triggerHaptic('light');
                setCompletedSessionSummary(null);
              }}
              className="w-full py-3.5 px-4 rounded-2xl bg-gradient-to-r from-emerald-400 to-emerald-500 hover:from-emerald-300 text-slate-950 font-black text-xs shadow-lg shadow-emerald-500/25 cursor-pointer transition-all"
            >
              Continue to Study Hub
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
