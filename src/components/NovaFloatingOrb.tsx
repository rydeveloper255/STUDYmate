import React, { useState } from 'react';
import { Sparkles, Mic, MessageSquare, X } from 'lucide-react';

interface NovaFloatingOrbProps {
  onOpenNova: (initialPrompt?: string) => void;
  activeExamName: string;
}

export const NovaFloatingOrb: React.FC<NovaFloatingOrbProps> = ({
  onOpenNova,
  activeExamName,
}) => {
  const [isOpenMenu, setIsOpenMenu] = useState(false);

  const quickPrompts = [
    `Aaj kya padhna chahiye for ${activeExamName}?`,
    `25 minute ka focused session start karo.`,
    `High-yield concept samjhao.`,
    `Quick 5-question test lo.`,
  ];

  return (
    <div className="fixed bottom-20 right-4 z-40 flex flex-col items-end pointer-events-auto">
      {isOpenMenu && (
        <div className="mb-3 w-72 rounded-2xl glass-panel p-3 border border-sky-500/30 shadow-2xl animate-in fade-in slide-in-from-bottom-4 duration-200">
          <div className="flex items-center justify-between pb-2 border-b border-white/10 mb-2">
            <div className="flex items-center gap-2">
              <div className="h-6 w-6 rounded-lg bg-sky-500/20 flex items-center justify-center text-sky-400">
                <Sparkles className="h-3.5 w-3.5" />
              </div>
              <span className="text-xs font-bold text-sky-300">Nova 2.0 AI Coach</span>
            </div>
            <button
              onClick={() => setIsOpenMenu(false)}
              className="text-slate-400 hover:text-white p-1"
            >
              <X className="h-4 w-4" />
            </button>
          </div>

          <p className="text-[11px] text-slate-300 mb-2">
            Quick Ask Nova or launch your next high-impact sprint:
          </p>

          <div className="space-y-1.5">
            {quickPrompts.map((prompt, idx) => (
              <button
                key={idx}
                onClick={() => {
                  setIsOpenMenu(false);
                  onOpenNova(prompt);
                }}
                className="w-full text-left px-2.5 py-1.5 rounded-xl bg-white/[0.06] hover:bg-sky-500/20 hover:border-sky-500/30 border border-transparent text-xs text-slate-200 transition-all flex items-center gap-2 cursor-pointer"
              >
                <MessageSquare className="h-3 w-3 text-sky-400 shrink-0" />
                <span className="truncate">{prompt}</span>
              </button>
            ))}
          </div>

          <button
            onClick={() => {
              setIsOpenMenu(false);
              onOpenNova();
            }}
            className="mt-2.5 w-full py-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 hover:to-indigo-500 text-xs font-bold text-white shadow-lg shadow-sky-500/20 flex items-center justify-center gap-1.5 cursor-pointer"
          >
            <Sparkles className="h-3.5 w-3.5" />
            Open Full Nova Tutor
          </button>
        </div>
      )}

      {/* Floating Glowing Orb Button */}
      <button
        id="nova-floating-orb-btn"
        onClick={() => setIsOpenMenu(!isOpenMenu)}
        className="relative group h-12 w-12 sm:h-13 sm:w-13 rounded-2xl bg-gradient-to-tr from-sky-500 via-indigo-500 to-cyan-400 p-[1.5px] shadow-xl shadow-sky-500/30 hover:scale-105 active:scale-95 transition-all duration-200 cursor-pointer"
        title="Ask Nova 2.0 AI"
      >
        <div className="h-full w-full rounded-[14px] bg-slate-950/80 backdrop-blur-md flex items-center justify-center text-sky-300 group-hover:bg-slate-900/60 transition-colors">
          <Sparkles className="h-6 w-6 text-sky-300 animate-pulse" />
        </div>
        <span className="absolute -top-1 -right-1 flex h-3 w-3">
          <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-sky-400 opacity-75"></span>
          <span className="relative inline-flex rounded-full h-3 w-3 bg-sky-500"></span>
        </span>
      </button>
    </div>
  );
};
