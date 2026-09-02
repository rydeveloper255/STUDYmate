import React from 'react';
import { ShieldCheck, Lock, Check, X, FileText, Smartphone, Award } from 'lucide-react';
import { triggerHaptic } from '../lib/haptics';

interface PrivacyTermsModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const PrivacyTermsModal: React.FC<PrivacyTermsModalProps> = ({ isOpen, onClose }) => {
  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md animate-in fade-in duration-200">
      <div className="relative w-full max-w-2xl max-h-[85vh] bg-[#0A0F24] border border-sky-500/30 rounded-3xl p-6 sm:p-8 flex flex-col shadow-2xl shadow-sky-500/10 overflow-hidden text-slate-200">
        {/* Header */}
        <div className="flex items-center justify-between pb-4 border-b border-white/10">
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-sky-500/20 text-sky-400 border border-sky-500/30">
              <ShieldCheck className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-lg sm:text-xl font-extrabold text-white">Privacy & Play Store Compliance</h2>
              <p className="text-xs text-slate-400">StudyMate AI Aspirant Data Protection & Terms</p>
            </div>
          </div>
          <button
            onClick={() => {
              triggerHaptic('light');
              onClose();
            }}
            className="p-2 rounded-xl bg-white/5 hover:bg-white/10 text-slate-400 hover:text-white transition-all cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content Body */}
        <div className="flex-1 overflow-y-auto pr-2 py-4 space-y-5 text-xs sm:text-sm text-slate-300 leading-relaxed custom-scrollbar">
          <section className="space-y-2">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Lock className="w-4 h-4 text-emerald-400" />
              1. Aspirant Data Privacy & Security
            </h3>
            <p>
              StudyMate AI is committed to student confidentiality. Your mock test scores, study schedules, notes, and uploaded question snapshots are processed securely and encrypted. We do not sell or monetize personal academic records to third-party ad networks.
            </p>
          </section>

          <section className="space-y-2">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Award className="w-4 h-4 text-sky-400" />
              2. AI Socratic Tutoring (Gemini Powered)
            </h3>
            <p>
              Our personal AI Study Coach (Nova 2.0) assists with syllabus breakdowns, PYQ analysis, formula mnemonics, and doubt clearing. All AI responses are engineered for academic rigor according to official UPSC, SSC, and Banking syllabus standards.
            </p>
          </section>

          <section className="space-y-2">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <Smartphone className="w-4 h-4 text-indigo-400" />
              3. Device Permissions & Camera Access
            </h3>
            <p>
              Camera and storage permissions are strictly used to scan handwritten doubts, textbook questions, or lecture notes. Audio recording permissions are used exclusively for voice-based questions and speech synthesis during Focus sessions.
            </p>
          </section>

          <section className="space-y-2">
            <h3 className="text-sm font-bold text-white flex items-center gap-2">
              <FileText className="w-4 h-4 text-amber-400" />
              4. Local Offline Storage & Sync
            </h3>
            <p>
              Your streaks, completed chapters, flashcard repetition intervals, and bookmarks are cached locally on your device for instant zero-lag offline access, even when studying in areas with weak cellular networks.
            </p>
          </section>
        </div>

        {/* Footer */}
        <div className="pt-4 border-t border-white/10 flex items-center justify-between">
          <span className="text-[11px] text-slate-400">Play Store Verified & Compliant • v2.4.0</span>
          <button
            onClick={() => {
              triggerHaptic('success');
              onClose();
            }}
            className="px-5 py-2 rounded-xl bg-gradient-to-r from-sky-500 to-indigo-600 hover:from-sky-400 text-slate-950 font-bold text-xs shadow-lg shadow-sky-500/20 cursor-pointer"
          >
            I Understand
          </button>
        </div>
      </div>
    </div>
  );
};
