import React, { useState, useEffect, useRef } from 'react';
import { UserProfile, ExamInfo, FocusSession } from '../types';
import { askNovaAssistant } from '../services/api';
import { triggerHaptic } from '../lib/haptics';
import {
  Shield,
  Play,
  Pause,
  RotateCcw,
  Sparkles,
  Volume2,
  VolumeX,
  Clock,
  CheckCircle2,
  Lock,
  Flame,
  Coffee,
  HelpCircle,
  Award
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
  const [selectedDuration, setSelectedDuration] = useState<number>(initialMinutes);
  const [subject, setSubject] = useState<string>(initialSubject || activeExam.subjects[0] || 'General Studies');
  const [topic, setTopic] = useState<string>(initialTopic || 'High-Yield Concept Revision');

  const [secondsRemaining, setSecondsRemaining] = useState<number>(initialMinutes * 60);
  const [isActive, setIsActive] = useState<boolean>(false);
  const [ambientSound, setAmbientSound] = useState<'none' | 'alpha' | 'rain' | 'whitenoise'>('none');
  const [strictBlocker, setStrictBlocker] = useState<boolean>(true);

  // Quick doubt in-focus
  const [quickDoubt, setQuickDoubt] = useState('');
  const [quickAnswer, setQuickAnswer] = useState<string | null>(null);
  const [isAskingDoubt, setIsAskingDoubt] = useState(false);

  // Web Audio Noise Generator
  const audioCtxRef = useRef<AudioContext | null>(null);
  const noiseNodeRef = useRef<AudioNode | null>(null);

  useEffect(() => {
    if (initialMinutes) {
      setSelectedDuration(initialMinutes);
      setSecondsRemaining(initialMinutes * 60);
    }
    if (initialSubject) setSubject(initialSubject);
    if (initialTopic) setTopic(initialTopic);
  }, [initialMinutes, initialSubject, initialTopic]);

  // Timer Tick
  useEffect(() => {
    let interval: any = null;
    if (isActive && secondsRemaining > 0) {
      interval = setInterval(() => {
        setSecondsRemaining((sec) => sec - 1);
      }, 1000);
    } else if (isActive && secondsRemaining === 0) {
      setIsActive(false);
      stopAmbient();
      triggerHaptic('success');
      const completedSess: FocusSession = {
        id: `sess_${Date.now()}`,
        startTime: Date.now() - selectedDuration * 60 * 1000,
        subject,
        topic,
        durationMinutes: selectedDuration,
      };
      onCompleteSession(completedSess);
    }
    return () => clearInterval(interval);
  }, [isActive, secondsRemaining]);

  // Handle Ambient Sound Synthesis (Clean Web Audio buffer)
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
        // Binaural / gentle 10Hz sine wave modulation
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
        // White / Pink filtered noise
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

  const handleStartTimer = () => {
    setIsActive(true);
  };

  const handlePauseTimer = () => {
    setIsActive(false);
  };

  const handleResetTimer = () => {
    setIsActive(false);
    setSecondsRemaining(selectedDuration * 60);
    stopAmbient();
    setAmbientSound('none');
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

  const minutes = Math.floor(secondsRemaining / 60);
  const seconds = secondsRemaining % 60;
  const progressPercent = Math.round(
    ((selectedDuration * 60 - secondsRemaining) / (selectedDuration * 60)) * 100
  );

  return (
    <div className="max-w-2xl mx-auto space-y-6 pb-24 animate-in fade-in duration-300">
      {/* Focus Shield Hero */}
      <div className="p-6 sm:p-8 rounded-3xl glass-panel border border-sky-500/30 text-center space-y-6 relative overflow-hidden shadow-2xl bg-gradient-to-b from-slate-900 via-sky-950/20 to-slate-900">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2 text-xs font-bold text-sky-400">
            <Shield className="h-4 w-4" />
            <span>Smart Focus Shield & Pomodoro</span>
          </div>

          <div className="flex items-center gap-2">
            <span className="text-[10px] px-2.5 py-1 rounded-full bg-amber-500/20 text-amber-300 font-bold flex items-center gap-1 border border-amber-500/30">
              <Flame className="h-3 w-3 fill-amber-400" />
              {user.streakDays} Day Streak
            </span>
          </div>
        </div>

        {/* Current Sprint Subject & Topic */}
        <div>
          <h2 className="text-xl sm:text-2xl font-extrabold text-white">{topic}</h2>
          <p className="text-xs text-slate-400 mt-0.5">{subject} • {activeExam.shortName}</p>
        </div>

        {/* Giant Timer Dial */}
        <div className="relative flex items-center justify-center py-4">
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
              <div className="text-xs text-slate-400 mt-1">
                {isActive ? '🔥 Deep Sprint in Progress' : 'Ready to begin'}
              </div>
            </div>
          </div>
        </div>

        {/* Duration Selectors */}
        {!isActive && (
          <div className="flex justify-center gap-2">
            {[15, 25, 45, 60].map((dur) => (
              <button
                key={dur}
                onClick={() => {
                  setSelectedDuration(dur);
                  setSecondsRemaining(dur * 60);
                }}
                className={`px-3.5 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
                  selectedDuration === dur
                    ? 'bg-sky-500/20 text-sky-300 border border-sky-400'
                    : 'bg-white/[0.05] text-slate-400 hover:text-white border border-white/10'
                }`}
              >
                {dur}m
              </button>
            ))}
          </div>
        )}

        {/* Timer Control Buttons */}
        <div className="flex items-center justify-center gap-3">
          {!isActive ? (
            <button
              onClick={handleStartTimer}
              className="px-8 py-3 rounded-2xl bg-gradient-to-r from-sky-400 to-sky-500 hover:from-sky-300 text-slate-950 font-extrabold text-sm shadow-xl shadow-sky-500/25 flex items-center gap-2 cursor-pointer"
            >
              <Play className="h-4 w-4 fill-slate-950" />
              Start Focus Sprint
            </button>
          ) : (
            <button
              onClick={handlePauseTimer}
              className="px-8 py-3 rounded-2xl bg-amber-500 hover:bg-amber-400 text-slate-950 font-extrabold text-sm shadow-xl shadow-amber-500/25 flex items-center gap-2 cursor-pointer"
            >
              <Pause className="h-4 w-4 fill-slate-950" />
              Pause Sprint
            </button>
          )}

          <button
            onClick={handleResetTimer}
            className="p-3 rounded-2xl bg-white/[0.08] hover:bg-white/[0.14] text-slate-300 hover:text-white border border-white/10 cursor-pointer"
            title="Reset Timer"
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
              { key: 'alpha', label: 'Alpha Waves (10Hz)' },
              { key: 'rain', label: 'Rain Storm' },
              { key: 'whitenoise', label: 'White Noise' },
            ] as const
          ).map((item) => (
            <button
              key={item.key}
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
        <div className="flex items-center gap-2">
          <Sparkles className="h-4 w-4 text-sky-400" />
          <span className="text-xs font-bold text-white">Ask Quick In-Focus Doubt (Zero Distraction)</span>
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
            className="px-4 py-2 rounded-xl bg-sky-500 hover:bg-sky-400 text-slate-950 font-bold text-xs cursor-pointer disabled:opacity-50"
          >
            {isAskingDoubt ? '...' : 'Ask'}
          </button>
        </form>

        {quickAnswer && (
          <div className="p-3 rounded-xl bg-sky-500/10 border border-sky-500/25 text-xs text-sky-200 leading-relaxed">
            {quickAnswer}
          </div>
        )}
      </div>
    </div>
  );
};
