import React from 'react';
import { Home, Bot, BookOpen, Target, Shield, BellRing } from 'lucide-react';

export type TabKey = 'home' | 'nova' | 'study' | 'practice' | 'focus' | 'updates';

interface FloatingGlassNavBarProps {
  activeTab: TabKey;
  onChangeTab: (tab: TabKey) => void;
  isFocusActive?: boolean;
}

export const FloatingGlassNavBar: React.FC<FloatingGlassNavBarProps> = ({
  activeTab,
  onChangeTab,
  isFocusActive = false,
}) => {
  const tabs: { key: TabKey; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
    { key: 'home', label: 'Home', icon: Home },
    { key: 'nova', label: 'Nova AI', icon: Bot },
    { key: 'study', label: 'Study Hub', icon: BookOpen },
    { key: 'practice', label: 'Practice', icon: Target },
    { key: 'focus', label: 'Focus Shield', icon: Shield },
    { key: 'updates', label: 'Updates', icon: BellRing },
  ];

  return (
    <nav className="fixed bottom-3 left-0 right-0 z-40 px-3 flex justify-center pointer-events-none">
      <div className="pointer-events-auto max-w-xl w-full glass-panel rounded-2xl p-1.5 border border-white/15 shadow-2xl shadow-black/80 flex items-center justify-between gap-1 backdrop-blur-2xl">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = activeTab === tab.key;
          const isFocus = tab.key === 'focus' && isFocusActive;

          return (
            <button
              key={tab.key}
              id={`nav-tab-${tab.key}`}
              onClick={() => onChangeTab(tab.key)}
              className={`flex-1 flex flex-col items-center justify-center py-2 px-1 rounded-xl transition-all duration-200 relative cursor-pointer ${
                isActive
                  ? 'bg-gradient-to-b from-sky-500/20 to-indigo-600/20 text-sky-300 font-semibold border border-sky-400/30 shadow-lg shadow-sky-500/10'
                  : 'text-slate-400 hover:text-slate-200 hover:bg-white/[0.05]'
              }`}
            >
              {isFocus && (
                <span className="absolute -top-1 -right-1 h-2.5 w-2.5 rounded-full bg-emerald-400 animate-ping"></span>
              )}
              <Icon
                className={`h-4.5 w-4.5 mb-1 transition-transform ${
                  isActive ? 'scale-110 text-sky-400' : 'text-slate-400'
                }`}
              />
              <span className="text-[10px] sm:text-[11px] leading-tight tracking-tight whitespace-nowrap">
                {tab.label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
};
