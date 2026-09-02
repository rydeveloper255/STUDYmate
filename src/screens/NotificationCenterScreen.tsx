import React from 'react';
import { StudyNotification } from '../types';
import { Bell, CheckCheck, Trash2, Clock, Sparkles } from 'lucide-react';

interface NotificationCenterScreenProps {
  notifications: StudyNotification[];
  onMarkAllAsRead: () => void;
  onClearNotifications: () => void;
  onBack: () => void;
}

export const NotificationCenterScreen: React.FC<NotificationCenterScreenProps> = ({
  notifications,
  onMarkAllAsRead,
  onClearNotifications,
  onBack,
}) => {
  return (
    <div className="max-w-2xl mx-auto space-y-6 pb-24 animate-in fade-in duration-300">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-extrabold text-white tracking-tight flex items-center gap-2">
            <Bell className="h-6 w-6 text-sky-400" />
            Study Alerts & Notifications
          </h1>
          <p className="text-xs text-slate-400">
            Reminders, streak milestones, and live recruitment announcements
          </p>
        </div>
        <button
          onClick={onBack}
          className="px-3.5 py-1.5 rounded-xl bg-white/[0.08] hover:bg-white/[0.14] text-xs font-semibold text-slate-300 cursor-pointer"
        >
          Back
        </button>
      </div>

      <div className="flex items-center justify-between">
        <button
          onClick={onMarkAllAsRead}
          className="text-xs text-sky-400 hover:text-sky-300 font-semibold flex items-center gap-1 cursor-pointer"
        >
          <CheckCheck className="h-3.5 w-3.5" />
          Mark all as read
        </button>
        <button
          onClick={onClearNotifications}
          className="text-xs text-rose-400 hover:text-rose-300 font-semibold flex items-center gap-1 cursor-pointer"
        >
          <Trash2 className="h-3.5 w-3.5" />
          Clear all
        </button>
      </div>

      <div className="space-y-3">
        {notifications.length === 0 ? (
          <div className="p-8 text-center rounded-3xl glass-card border border-white/10 space-y-2">
            <Bell className="h-8 w-8 text-slate-500 mx-auto" />
            <div className="text-sm font-bold text-slate-300">All caught up!</div>
            <p className="text-xs text-slate-500">No new alerts at this moment.</p>
          </div>
        ) : (
          notifications.map((n) => (
            <div
              key={n.id}
              className={`p-4 rounded-2xl glass-card border transition-all flex items-start justify-between gap-3 ${
                n.isRead ? 'border-white/5 opacity-70' : 'border-sky-500/30 bg-sky-950/20'
              }`}
            >
              <div className="space-y-1">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-bold text-white">{n.title}</span>
                  {!n.isRead && (
                    <span className="h-2 w-2 rounded-full bg-sky-400"></span>
                  )}
                </div>
                <p className="text-xs text-slate-300 leading-relaxed">{n.message}</p>
                <div className="text-[11px] text-slate-500 flex items-center gap-1 pt-1">
                  <Clock className="h-3 w-3" />
                  <span>{n.timestamp}</span>
                </div>
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};
