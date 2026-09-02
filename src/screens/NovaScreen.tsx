import React, { useState, useRef, useEffect } from 'react';
import { UserProfile, ExamInfo, NovaChatMessage, NovaMemoryItem, NovaSettings } from '../types';
import { askNovaAssistant } from '../services/api';
import { triggerHaptic } from '../lib/haptics';
import ReactMarkdown from 'react-markdown';
import {
  Sparkles,
  Send,
  Image as ImageIcon,
  Camera,
  Mic,
  MicOff,
  Bot,
  User,
  Trash2,
  Settings,
  Brain,
  Play,
  HelpCircle,
  Clock,
  Layers,
  Check,
  Bookmark,
  Volume2,
  VolumeX,
  X
} from 'lucide-react';

interface NovaScreenProps {
  user: UserProfile;
  activeExam: ExamInfo;
  memories: NovaMemoryItem[];
  onSaveMemory: (item: NovaMemoryItem) => void;
  onDeleteMemory: (id: string) => void;
  onStartFocusSprint: (minutes: number, subject: string, topic: string) => void;
  onStartQuiz: (subject: string, topic: string) => void;
  initialPrompt?: string;
}

export const NovaScreen: React.FC<NovaScreenProps> = ({
  user,
  activeExam,
  memories,
  onSaveMemory,
  onDeleteMemory,
  onStartFocusSprint,
  onStartQuiz,
  initialPrompt = '',
}) => {
  const [messages, setMessages] = useState<NovaChatMessage[]>([
    {
      id: 'init_1',
      sender: 'assistant',
      text: `Hello ${user.name}! 🌟 I'm **NOVA 2.0**, your personalized AI Study Coach for **${activeExam.name}**.\n\nI have your real-time syllabus data loaded (${activeExam.daysRemaining} days left). Aap mujhse kisi bhi subject ke doubts pooch sakte ho, handwritten question photo scan kar sakte ho, ya targeted practice sprint start kar sakte ho!\n\n*How can I help you today, Boss?*`,
      timestamp: 'Just now',
    },
  ]);

  const [inputPrompt, setInputPrompt] = useState(initialPrompt);
  const [isLoading, setIsLoading] = useState(false);
  const [activeSubTab, setActiveSubTab] = useState<'chat' | 'memory' | 'settings'>('chat');
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [isRecording, setIsRecording] = useState(false);
  const [speakingMessageId, setSpeakingMessageId] = useState<string | null>(null);

  const [settings, setSettings] = useState<NovaSettings>({
    useBossGreeting: true,
    voiceEnabled: true,
    memoryEnabled: true,
    thinkingMode: false,
    selectedPersona: 'Empathetic Socratic Tutor',
  });

  const messagesEndRef = useRef<HTMLDivElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const cameraInputRef = useRef<HTMLInputElement>(null);
  const recognitionRef = useRef<any>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, isLoading]);

  useEffect(() => {
    if (initialPrompt && initialPrompt.trim()) {
      handleSendMessage(initialPrompt);
    }
  }, []);

  // Voice speech-to-text listener
  useEffect(() => {
    if (typeof window !== 'undefined' && ('SpeechRecognition' in window || 'webkitSpeechRecognition' in window)) {
      const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
      recognitionRef.current = new SpeechRecognition();
      recognitionRef.current.continuous = false;
      recognitionRef.current.interimResults = false;
      recognitionRef.current.lang = user.preferredLanguage === 'Hindi' ? 'hi-IN' : 'en-IN';

      recognitionRef.current.onresult = (event: any) => {
        const transcript = event.results[0][0].transcript;
        setInputPrompt((prev) => (prev ? `${prev} ${transcript}` : transcript));
        setIsRecording(false);
        triggerHaptic('success');
      };

      recognitionRef.current.onerror = () => {
        setIsRecording(false);
      };

      recognitionRef.current.onend = () => {
        setIsRecording(false);
      };
    }
  }, [user.preferredLanguage]);

  const toggleVoiceInput = () => {
    if (!recognitionRef.current) {
      alert('Voice recognition is not supported in this browser. Please type your query.');
      return;
    }

    if (isRecording) {
      recognitionRef.current.stop();
      setIsRecording(false);
      triggerHaptic('light');
    } else {
      try {
        recognitionRef.current.start();
        setIsRecording(true);
        triggerHaptic('medium');
      } catch (e) {
        setIsRecording(false);
      }
    }
  };

  const handleSendMessage = async (textToSend?: string) => {
    const prompt = (textToSend || inputPrompt).trim();
    if (!prompt && !selectedImage) return;

    triggerHaptic('light');

    const userMessage: NovaChatMessage = {
      id: `msg_user_${Date.now()}`,
      sender: 'user',
      text: prompt || 'Analyze this question image and explain the core principle and solution:',
      timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      imageUri: selectedImage || undefined,
    };

    setMessages((prev) => [...prev, userMessage]);
    setInputPrompt('');
    const curImg = selectedImage;
    setSelectedImage(null);
    setIsLoading(true);

    try {
      const history = messages.map((m) => ({
        role: m.sender === 'user' ? 'user' : 'model',
        text: m.text,
      }));

      const studyContext = {
        studentName: user.name,
        targetExam: activeExam.name,
        examDaysRemaining: activeExam.daysRemaining,
        subjects: activeExam.subjects,
        weakTopics: ['Fundamental Rights Article 14 vs 21', 'Headline Inflation CPI Basket'],
        strongTopics: ['Modern History Freedom Struggle', 'Judicial Writs'],
        dailyTargetMinutes: user.studyGoalMinutesPerDay,
        todayFocusMinutes: user.todayFocusedMinutes,
        currentStreak: user.streakDays,
        preferredLanguage: user.preferredLanguage,
        preferredStudyDurationMins: 25,
        memories: memories.map((m) => ({
          category: { name: m.category, displayName: m.category },
          key: m.key,
          value: m.value,
        })),
      };

      const res = await askNovaAssistant(
        prompt || 'Analyze this question photo step-by-step',
        history,
        studyContext,
        settings,
        settings.thinkingMode,
        curImg
      );

      triggerHaptic('success');

      const assistantMsg: NovaChatMessage = {
        id: `msg_asst_${Date.now()}`,
        sender: 'assistant',
        text: res.replyMarkdown,
        timestamp: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
        actionType: res.actionType,
        actionPayload: res.actionPayload || undefined,
      };

      if (res.memoryToSave) {
        onSaveMemory({
          id: `mem_${Date.now()}`,
          category: res.memoryToSave.category || 'STUDY_PREFERENCES',
          key: res.memoryToSave.key || 'Learned Habit',
          value: res.memoryToSave.value || '',
          timestamp: 'Just now',
        });
      }

      setMessages((prev) => [...prev, assistantMsg]);
    } catch (err: any) {
      triggerHaptic('error');
      setMessages((prev) => [
        ...prev,
        {
          id: `msg_err_${Date.now()}`,
          sender: 'assistant',
          text: `Main connected hoon! Aapke ${activeExam.shortName} target ke liye chalo 25-minute ka high-yield sprint start karte hain.`,
          timestamp: 'Just now',
        },
      ]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleImageUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      triggerHaptic('medium');
      const reader = new FileReader();
      reader.onloadend = () => {
        setSelectedImage(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleExecuteAction = (actionType: string, payloadStr?: string) => {
    triggerHaptic('success');
    try {
      const payload = payloadStr ? JSON.parse(payloadStr) : {};
      if (actionType === 'START_FOCUS') {
        onStartFocusSprint(payload.minutes || 25, payload.subject || 'Polity & Governance', payload.topic || 'High-Yield Concepts');
      } else if (actionType === 'START_QUIZ') {
        onStartQuiz(payload.subject || activeExam.subjects[0] || 'General Studies', payload.topic || 'Concept Review');
      } else {
        onStartFocusSprint(25, activeExam.subjects[0] || 'Polity', 'Comprehensive Revision');
      }
    } catch (e) {
      onStartFocusSprint(25, activeExam.subjects[0] || 'Polity', 'Comprehensive Revision');
    }
  };

  const toggleSpeech = (id: string, text: string) => {
    triggerHaptic('light');
    if ('speechSynthesis' in window) {
      if (speakingMessageId === id) {
        window.speechSynthesis.cancel();
        setSpeakingMessageId(null);
      } else {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text.replace(/[*#_`]/g, ''));
        utterance.rate = 1.05;
        utterance.onend = () => setSpeakingMessageId(null);
        utterance.onerror = () => setSpeakingMessageId(null);
        setSpeakingMessageId(id);
        window.speechSynthesis.speak(utterance);
      }
    }
  };

  const quickChips = [
    '🎯 Aaj ka optimal study plan kya hai?',
    '⏱️ 25-minute ka focus sprint shuru karo',
    '🧠 Article 14 vs 21 landmark verdicts samjhao',
    '📝 5 questions ka quick quiz lo',
  ];

  return (
    <div className="h-[calc(100vh-140px)] flex flex-col rounded-3xl glass-panel border border-white/10 overflow-hidden shadow-2xl relative">
      {/* Top Nova Header with Sub-Tabs */}
      <div className="px-4 py-3 border-b border-white/10 bg-slate-900/60 backdrop-blur-md flex items-center justify-between gap-2">
        <div className="flex items-center gap-3">
          <div className="relative">
            <div className="h-10 w-10 rounded-2xl bg-gradient-to-tr from-sky-400 via-indigo-500 to-cyan-400 p-[1.5px] shadow-lg shadow-sky-500/20">
              <div className="h-full w-full rounded-[14px] bg-slate-950 flex items-center justify-center text-sky-400">
                <Sparkles className="h-5 w-5 animate-pulse" />
              </div>
            </div>
            <span className="absolute bottom-0 right-0 h-3 w-3 rounded-full bg-emerald-400 ring-2 ring-slate-900"></span>
          </div>

          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-sm font-bold text-white flex items-center gap-1.5">
                NOVA 2.0 AI Tutor
              </h2>
              {settings.thinkingMode && (
                <span className="text-[10px] px-1.5 py-0.2 rounded-md bg-purple-500/20 text-purple-300 font-bold border border-purple-500/30 flex items-center gap-1">
                  <Brain className="h-2.5 w-2.5" /> Deep Thinking
                </span>
              )}
            </div>
            <p className="text-[11px] text-slate-400">
              Personalized for {activeExam.shortName} • Multi-Lingual AI Tutor
            </p>
          </div>
        </div>

        {/* Sub Tab Switcher */}
        <div className="flex items-center gap-1 bg-white/[0.05] p-1 rounded-xl border border-white/10">
          <button
            onClick={() => {
              triggerHaptic('light');
              setActiveSubTab('chat');
            }}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
              activeSubTab === 'chat'
                ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Chat
          </button>
          <button
            onClick={() => {
              triggerHaptic('light');
              setActiveSubTab('memory');
            }}
            className={`px-3 py-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer flex items-center gap-1 ${
              activeSubTab === 'memory'
                ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
          >
            <Bookmark className="h-3 w-3" />
            Memory ({memories.length})
          </button>
          <button
            onClick={() => {
              triggerHaptic('light');
              setActiveSubTab('settings');
            }}
            className={`p-1.5 rounded-lg text-xs font-semibold transition-all cursor-pointer ${
              activeSubTab === 'settings'
                ? 'bg-sky-500/20 text-sky-300 border border-sky-500/30'
                : 'text-slate-400 hover:text-white'
            }`}
            title="Persona & Model Settings"
          >
            <Settings className="h-3.5 w-3.5" />
          </button>
        </div>
      </div>

      {/* Main Content Area */}
      {activeSubTab === 'chat' && (
        <div className="flex-1 flex flex-col justify-between overflow-hidden">
          {/* Messages Scroll Area */}
          <div className="flex-1 overflow-y-auto p-4 space-y-4">
            {messages.map((msg) => (
              <div
                key={msg.id}
                className={`flex gap-3 max-w-2xl ${
                  msg.sender === 'user' ? 'ml-auto flex-row-reverse' : 'mr-auto'
                }`}
              >
                {/* Avatar */}
                <div
                  className={`h-8 w-8 rounded-xl shrink-0 flex items-center justify-center text-xs font-bold ${
                    msg.sender === 'user'
                      ? 'bg-gradient-to-tr from-sky-400 to-indigo-600 text-slate-950'
                      : 'bg-slate-800 text-sky-400 border border-white/10'
                  }`}
                >
                  {msg.sender === 'user' ? <User className="h-4 w-4" /> : <Bot className="h-4 w-4" />}
                </div>

                {/* Message Content Bubble */}
                <div
                  className={`rounded-2xl p-4 text-xs sm:text-sm leading-relaxed ${
                    msg.sender === 'user'
                      ? 'bg-gradient-to-r from-sky-600 to-indigo-600 text-white rounded-tr-none'
                      : 'glass-card border border-white/10 text-slate-100 rounded-tl-none space-y-3'
                  }`}
                >
                  {msg.imageUri && (
                    <img
                      src={msg.imageUri}
                      alt="Uploaded Doubt"
                      className="max-h-48 rounded-xl object-contain mb-2 border border-white/20"
                    />
                  )}

                  <div className="prose prose-invert prose-xs sm:prose-sm max-w-none text-slate-100">
                    <ReactMarkdown>{msg.text}</ReactMarkdown>
                  </div>

                  {/* If assistant proposed an actionable tool */}
                  {msg.actionType && msg.actionType !== 'NONE' && (
                    <div className="pt-2 border-t border-white/10 flex items-center justify-between gap-2">
                      <span className="text-[11px] font-semibold text-sky-300 flex items-center gap-1">
                        <Sparkles className="h-3 w-3" />
                        Proposed Action: {msg.actionType.replace('_', ' ')}
                      </span>
                      <button
                        onClick={() => handleExecuteAction(msg.actionType!, msg.actionPayload)}
                        className="px-3 py-1.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-slate-950 font-bold text-xs flex items-center gap-1.5 shadow-md cursor-pointer"
                      >
                        <Play className="h-3 w-3 fill-slate-950" />
                        Execute Action
                      </button>
                    </div>
                  )}

                  {/* Message footer with timestamp & Speech synthesizer */}
                  <div className="flex items-center justify-between text-[10px] text-slate-400 pt-1">
                    <span>{msg.timestamp}</span>
                    {msg.sender === 'assistant' && (
                      <button
                        onClick={() => toggleSpeech(msg.id, msg.text)}
                        className="p-1 hover:text-sky-300 transition-colors cursor-pointer"
                        title={speakingMessageId === msg.id ? 'Stop Speech' : 'Listen via TTS'}
                      >
                        {speakingMessageId === msg.id ? (
                          <VolumeX className="h-3.5 w-3.5 text-rose-400 animate-pulse" />
                        ) : (
                          <Volume2 className="h-3.5 w-3.5 text-slate-400 hover:text-sky-400" />
                        )}
                      </button>
                    )}
                  </div>
                </div>
              </div>
            ))}

            {isLoading && (
              <div className="flex gap-3 max-w-lg mr-auto">
                <div className="h-8 w-8 rounded-xl bg-slate-800 text-sky-400 flex items-center justify-center border border-white/10">
                  <Bot className="h-4 w-4 animate-spin" />
                </div>
                <div className="glass-card p-4 rounded-2xl rounded-tl-none border border-white/10 flex items-center gap-2 text-xs text-sky-300">
                  <span className="h-2 w-2 rounded-full bg-sky-400 animate-ping"></span>
                  Nova is synthesizing high-yield response...
                </div>
              </div>
            )}
            <div ref={messagesEndRef} />
          </div>

          {/* Quick Suggestion Chips */}
          <div className="px-4 py-2 flex items-center gap-2 overflow-x-auto no-scrollbar border-t border-white/5 bg-slate-950/40">
            {quickChips.map((chip, idx) => (
              <button
                key={idx}
                onClick={() => handleSendMessage(chip)}
                className="px-3 py-1.5 rounded-full bg-white/[0.05] hover:bg-sky-500/20 hover:border-sky-500/30 border border-white/10 text-xs text-slate-300 whitespace-nowrap transition-all cursor-pointer"
              >
                {chip}
              </button>
            ))}
          </div>

          {/* Input Bar */}
          <div className="p-3 sm:p-4 bg-slate-900/90 border-t border-white/10">
            {selectedImage && (
              <div className="mb-2 flex items-center gap-2 bg-slate-800/80 p-2 rounded-xl border border-sky-500/30 w-fit">
                <img src={selectedImage} alt="Selected" className="h-10 w-10 rounded-lg object-cover" />
                <span className="text-xs text-slate-300">Question image attached</span>
                <button
                  onClick={() => setSelectedImage(null)}
                  className="p-1 hover:text-rose-400 text-slate-400 cursor-pointer"
                >
                  <X className="h-3.5 w-3.5" />
                </button>
              </div>
            )}

            <form
              onSubmit={(e) => {
                e.preventDefault();
                handleSendMessage();
              }}
              className="flex items-center gap-2"
            >
              {/* Gallery Image Picker */}
              <input
                type="file"
                ref={fileInputRef}
                onChange={handleImageUpload}
                accept="image/*"
                className="hidden"
              />

              {/* Mobile Camera Snapshot */}
              <input
                type="file"
                ref={cameraInputRef}
                onChange={handleImageUpload}
                accept="image/*"
                capture="environment"
                className="hidden"
              />

              <button
                type="button"
                onClick={() => {
                  triggerHaptic('light');
                  fileInputRef.current?.click();
                }}
                className="p-2.5 rounded-xl bg-white/[0.06] hover:bg-white/[0.12] border border-white/10 text-slate-300 hover:text-sky-300 transition-colors cursor-pointer"
                title="Attach question image from gallery"
              >
                <ImageIcon className="h-4 w-4" />
              </button>

              <button
                type="button"
                onClick={() => {
                  triggerHaptic('light');
                  cameraInputRef.current?.click();
                }}
                className="p-2.5 rounded-xl bg-white/[0.06] hover:bg-white/[0.12] border border-white/10 text-slate-300 hover:text-sky-300 transition-colors cursor-pointer"
                title="Snap question with camera"
              >
                <Camera className="h-4 w-4" />
              </button>

              <div className="flex-1 relative">
                <input
                  type="text"
                  value={inputPrompt}
                  onChange={(e) => setInputPrompt(e.target.value)}
                  placeholder={isRecording ? 'Listening to your voice doubt...' : `Ask Nova anything in English, Hindi, or Hinglish...`}
                  className={`w-full px-4 py-2.5 rounded-xl bg-slate-950/80 border text-xs sm:text-sm text-white placeholder:text-slate-500 outline-none transition-all ${
                    isRecording ? 'border-rose-500 ring-1 ring-rose-500 animate-pulse' : 'border-white/15 focus:border-sky-400 focus:ring-1 focus:ring-sky-400'
                  }`}
                />
              </div>

              {/* Mic Voice Button */}
              <button
                type="button"
                onClick={toggleVoiceInput}
                className={`p-2.5 rounded-xl border transition-all cursor-pointer ${
                  isRecording
                    ? 'bg-rose-500 text-white border-rose-400 animate-pulse shadow-lg shadow-rose-500/30'
                    : 'bg-white/[0.06] hover:bg-white/[0.12] border-white/10 text-slate-300 hover:text-sky-300'
                }`}
                title={isRecording ? 'Stop Recording' : 'Speak your doubt'}
              >
                {isRecording ? <MicOff className="h-4 w-4" /> : <Mic className="h-4 w-4" />}
              </button>

              <button
                type="submit"
                disabled={isLoading || (!inputPrompt.trim() && !selectedImage)}
                className="p-2.5 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 disabled:opacity-40 text-slate-950 font-bold transition-all shadow-lg shadow-sky-500/20 cursor-pointer"
              >
                <Send className="h-4 w-4 fill-slate-950" />
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Memory Vault Manager Sub-Tab */}
      {activeSubTab === 'memory' && (
        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          <div className="flex items-center justify-between">
            <div>
              <h3 className="text-sm font-bold text-white flex items-center gap-2">
                <Bookmark className="h-4 w-4 text-sky-400" />
                Nova Long-Term Memory Vault
              </h3>
              <p className="text-xs text-slate-400">
                Nova dynamically saves your learning preferences, weak topics, and exam milestones.
              </p>
            </div>
          </div>

          <div className="space-y-2">
            {memories.map((mem) => (
              <div
                key={mem.id}
                className="p-3.5 rounded-2xl glass-card border border-white/10 flex items-center justify-between gap-3"
              >
                <div>
                  <div className="flex items-center gap-2 mb-1">
                    <span className="text-[10px] px-2 py-0.5 rounded-full bg-sky-500/20 text-sky-300 font-bold uppercase">
                      {mem.category}
                    </span>
                    <span className="text-xs font-semibold text-white">{mem.key}</span>
                  </div>
                  <p className="text-xs text-slate-300">{mem.value}</p>
                </div>
                <button
                  onClick={() => {
                    triggerHaptic('warning');
                    onDeleteMemory(mem.id);
                  }}
                  className="p-2 rounded-xl text-slate-400 hover:text-rose-400 hover:bg-rose-500/10 transition-colors cursor-pointer"
                  title="Forget this memory"
                >
                  <Trash2 className="h-4 w-4" />
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Settings Sub-Tab */}
      {activeSubTab === 'settings' && (
        <div className="flex-1 overflow-y-auto p-5 space-y-5">
          <div>
            <h3 className="text-sm font-bold text-white flex items-center gap-2 mb-1">
              <Settings className="h-4 w-4 text-sky-400" />
              AI Assistant Persona & Model Config
            </h3>
            <p className="text-xs text-slate-400">
              Customize how Nova interacts, thinks, and coaches you.
            </p>
          </div>

          {/* Persona Selection */}
          <div className="space-y-2">
            <label className="text-xs font-bold text-slate-300 uppercase tracking-wider">
              Tutor Persona Mode
            </label>
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-2">
              {(['Empathetic Socratic Tutor', 'Strict Exam Strategist', 'ELI5 Friendly Mentor'] as const).map(
                (persona) => (
                  <button
                    key={persona}
                    onClick={() => {
                      triggerHaptic('light');
                      setSettings((s) => ({ ...s, selectedPersona: persona }));
                    }}
                    className={`p-3 rounded-2xl text-left border transition-all cursor-pointer ${
                      settings.selectedPersona === persona
                        ? 'bg-sky-500/20 border-sky-400 text-sky-300 font-bold'
                        : 'glass-card border-white/10 text-slate-300 hover:bg-white/[0.06]'
                    }`}
                  >
                    <div className="text-xs font-bold">{persona}</div>
                  </button>
                )
              )}
            </div>
          </div>

          {/* Thinking Mode Switch */}
          <div className="p-4 rounded-2xl glass-card border border-white/10 flex items-center justify-between">
            <div>
              <div className="text-xs font-bold text-white flex items-center gap-1.5">
                <Brain className="h-4 w-4 text-purple-400" />
                Deep Thinking & Reasoning Mode
              </div>
              <div className="text-[11px] text-slate-400 mt-0.5">
                Utilizes Gemini 3.7 Flash for multi-step math, physics formulas, and complex law derivations.
              </div>
            </div>
            <button
              onClick={() => {
                triggerHaptic('medium');
                setSettings((s) => ({ ...s, thinkingMode: !s.thinkingMode }));
              }}
              className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                settings.thinkingMode ? 'bg-purple-600' : 'bg-slate-800'
              }`}
            >
              <div
                className={`w-5 h-5 rounded-full bg-white absolute top-0.5 transition-transform ${
                  settings.thinkingMode ? 'right-0.5' : 'left-0.5'
                }`}
              />
            </button>
          </div>

          {/* Boss Greeting Switch */}
          <div className="p-4 rounded-2xl glass-card border border-white/10 flex items-center justify-between">
            <div>
              <div className="text-xs font-bold text-white">
                Casual "Boss" Salutation
              </div>
              <div className="text-[11px] text-slate-400 mt-0.5">
                Address user warmly as "Boss" during study coaching and focus sprints.
              </div>
            </div>
            <button
              onClick={() => {
                triggerHaptic('medium');
                setSettings((s) => ({ ...s, useBossGreeting: !s.useBossGreeting }));
              }}
              className={`w-12 h-6 rounded-full transition-colors relative cursor-pointer ${
                settings.useBossGreeting ? 'bg-sky-500' : 'bg-slate-800'
              }`}
            >
              <div
                className={`w-5 h-5 rounded-full bg-white absolute top-0.5 transition-transform ${
                  settings.useBossGreeting ? 'right-0.5' : 'left-0.5'
                }`}
              />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
