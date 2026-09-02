import React, { useState, useEffect } from 'react';
import { WifiOff, Wifi } from 'lucide-react';
import { triggerHaptic } from '../lib/haptics';

export const OfflineIndicator: React.FC = () => {
  const [isOnline, setIsOnline] = useState(typeof navigator !== 'undefined' ? navigator.onLine : true);
  const [showReconnected, setShowReconnected] = useState(false);

  useEffect(() => {
    const handleOnline = () => {
      setIsOnline(true);
      setShowReconnected(true);
      triggerHaptic('success');
      const timer = setTimeout(() => setShowReconnected(false), 3000);
      return () => clearTimeout(timer);
    };

    const handleOffline = () => {
      setIsOnline(false);
      triggerHaptic('warning');
    };

    window.addEventListener('online', handleOnline);
    window.addEventListener('offline', handleOffline);

    return () => {
      window.removeEventListener('online', handleOnline);
      window.removeEventListener('offline', handleOffline);
    };
  }, []);

  if (isOnline && !showReconnected) return null;

  return (
    <div className="fixed top-16 left-1/2 -translate-x-1/2 z-40 max-w-sm w-[90%] px-4 py-2 rounded-2xl backdrop-blur-lg border shadow-xl flex items-center justify-between text-xs font-semibold animate-in slide-in-from-top duration-300 pointer-events-none">
      {!isOnline ? (
        <div className="flex items-center gap-2 text-amber-300 bg-amber-950/80 border border-amber-500/30 w-full p-2.5 rounded-xl">
          <WifiOff className="w-4 h-4 text-amber-400 shrink-0" />
          <span>Offline mode active — local PYQs & flashcards ready.</span>
        </div>
      ) : (
        <div className="flex items-center gap-2 text-emerald-300 bg-emerald-950/80 border border-emerald-500/30 w-full p-2.5 rounded-xl">
          <Wifi className="w-4 h-4 text-emerald-400 shrink-0" />
          <span>Back online! Live AI Tutor & sync connected.</span>
        </div>
      )}
    </div>
  );
};
