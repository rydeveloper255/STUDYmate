import React, { useState } from 'react';
import { VacancyItem } from '../types';
import {
  BellRing,
  Search,
  Bookmark,
  ExternalLink,
  Calendar,
  Briefcase,
  GraduationCap,
  DollarSign,
  CheckCircle2,
  Clock,
  Filter,
  X
} from 'lucide-react';

interface UpdatesHubScreenProps {
  vacancies: VacancyItem[];
  bookmarkedIds: string[];
  appliedIds: string[];
  onToggleBookmark: (id: string) => void;
  onToggleApplied: (id: string) => void;
}

export const UpdatesHubScreen: React.FC<UpdatesHubScreenProps> = ({
  vacancies,
  bookmarkedIds,
  appliedIds,
  onToggleBookmark,
  onToggleApplied,
}) => {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  const [selectedVacancy, setSelectedVacancy] = useState<VacancyItem | null>(null);

  const categories = ['All', 'Vacancy', 'Admit Card', 'Result', 'Answer Key'];

  const filteredVacancies = vacancies.filter((v) => {
    const matchesSearch =
      v.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      v.organization.toLowerCase().includes(searchQuery.toLowerCase()) ||
      v.qualification.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCat = selectedCategory === 'All' || v.category === selectedCategory;
    return matchesSearch && matchesCat;
  });

  return (
    <div className="space-y-6 pb-24 animate-in fade-in duration-300">
      {/* Top Banner */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl sm:text-2xl font-extrabold text-white tracking-tight flex items-center gap-2">
            <BellRing className="h-6 w-6 text-amber-400" />
            Recruitment & Exam Updates Hub
          </h1>
          <p className="text-xs text-slate-400">
            Real-time government job notifications, admit cards, results & deadline trackers
          </p>
        </div>
      </div>

      {/* Search & Category Filter */}
      <div className="flex flex-col sm:flex-row gap-3">
        <div className="flex-1 relative">
          <Search className="h-4 w-4 absolute left-3.5 top-3 text-slate-400" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search exam, commission (UPSC, SSC, Banking, Railways), or post..."
            className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-slate-900/80 border border-white/10 text-xs sm:text-sm text-white placeholder:text-slate-500 outline-none focus:border-amber-400/50"
          />
        </div>

        <div className="flex items-center gap-1 overflow-x-auto pb-1">
          {categories.map((cat) => (
            <button
              key={cat}
              onClick={() => setSelectedCategory(cat)}
              className={`px-3.5 py-2 rounded-xl text-xs font-bold whitespace-nowrap transition-all cursor-pointer ${
                selectedCategory === cat
                  ? 'bg-amber-500/20 text-amber-300 border border-amber-500/30'
                  : 'bg-white/[0.04] text-slate-400 hover:text-white border border-white/5'
              }`}
            >
              {cat}
            </button>
          ))}
        </div>
      </div>

      {/* Vacancy Cards Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
        {filteredVacancies.map((item) => {
          const isBookmarked = bookmarkedIds.includes(item.id);
          const isApplied = appliedIds.includes(item.id);

          return (
            <div
              key={item.id}
              className="p-5 rounded-3xl glass-panel border border-white/10 flex flex-col justify-between gap-4 hover:border-amber-500/30 transition-all"
            >
              <div>
                <div className="flex items-start justify-between gap-2 mb-2">
                  <span className="text-[10px] px-2.5 py-0.5 rounded-full bg-amber-500/20 text-amber-300 font-bold border border-amber-500/30">
                    {item.category}
                  </span>
                  <div className="flex items-center gap-1">
                    <button
                      onClick={() => onToggleBookmark(item.id)}
                      className={`p-1.5 rounded-xl border transition-colors cursor-pointer ${
                        isBookmarked
                          ? 'bg-amber-500/20 text-amber-300 border-amber-500/30'
                          : 'bg-white/[0.05] text-slate-400 border-white/5 hover:text-white'
                      }`}
                      title={isBookmarked ? 'Bookmarked' : 'Bookmark'}
                    >
                      <Bookmark className={`h-3.5 w-3.5 ${isBookmarked ? 'fill-amber-400' : ''}`} />
                    </button>
                  </div>
                </div>

                <h3 className="text-sm sm:text-base font-bold text-white leading-snug">
                  {item.title}
                </h3>
                <p className="text-xs text-sky-300 font-medium mt-0.5">{item.organization}</p>
              </div>

              {/* Snapshot Info */}
              <div className="grid grid-cols-2 gap-2 text-xs text-slate-300 pt-2 border-t border-white/5">
                <div className="flex items-center gap-1.5">
                  <Briefcase className="h-3.5 w-3.5 text-amber-400 shrink-0" />
                  <span className="truncate">{item.totalPosts} Posts</span>
                </div>
                <div className="flex items-center gap-1.5">
                  <Calendar className="h-3.5 w-3.5 text-amber-400 shrink-0" />
                  <span className="truncate">Last: {item.lastDateToApply}</span>
                </div>
                <div className="flex items-center gap-1.5 col-span-2">
                  <GraduationCap className="h-3.5 w-3.5 text-amber-400 shrink-0" />
                  <span className="truncate">{item.qualification}</span>
                </div>
              </div>

              {/* Action Buttons */}
              <div className="flex items-center gap-2 pt-2">
                <button
                  onClick={() => setSelectedVacancy(item)}
                  className="flex-1 py-2 rounded-xl bg-white/[0.06] hover:bg-white/[0.12] text-slate-200 text-xs font-semibold border border-white/10 cursor-pointer"
                >
                  View Eligibility & Details
                </button>
                <button
                  onClick={() => onToggleApplied(item.id)}
                  className={`px-3 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 cursor-pointer ${
                    isApplied
                      ? 'bg-emerald-500/20 text-emerald-300 border border-emerald-500/30'
                      : 'bg-amber-500/20 hover:bg-amber-500/30 text-amber-300 border border-amber-500/30'
                  }`}
                >
                  <CheckCircle2 className="h-3.5 w-3.5" />
                  <span>{isApplied ? 'Applied' : 'Mark Applied'}</span>
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Detailed Modal View */}
      {selectedVacancy && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-150">
          <div className="w-full max-w-lg rounded-3xl glass-panel p-6 border border-white/15 shadow-2xl space-y-4">
            <div className="flex items-start justify-between pb-3 border-b border-white/10">
              <div>
                <span className="text-[10px] px-2.5 py-0.5 rounded-full bg-amber-500/20 text-amber-300 font-bold">
                  {selectedVacancy.category}
                </span>
                <h3 className="text-base font-bold text-white mt-1">{selectedVacancy.title}</h3>
                <p className="text-xs text-sky-400 font-medium">{selectedVacancy.organization}</p>
              </div>
              <button
                onClick={() => setSelectedVacancy(null)}
                className="text-slate-400 hover:text-white p-1"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <div className="p-3 rounded-xl bg-white/[0.04] border border-white/5 space-y-1">
                <div className="text-slate-400">Total Vacancies</div>
                <div className="text-sm font-bold text-white">{selectedVacancy.totalPosts}</div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="p-3 rounded-xl bg-white/[0.04] border border-white/5 space-y-1">
                  <div className="text-slate-400">Age Limit</div>
                  <div className="font-semibold text-white">{selectedVacancy.ageLimit}</div>
                </div>
                <div className="p-3 rounded-xl bg-white/[0.04] border border-white/5 space-y-1">
                  <div className="text-slate-400">Pay Scale</div>
                  <div className="font-semibold text-white">{selectedVacancy.salaryPayScale}</div>
                </div>
              </div>

              <div className="p-3 rounded-xl bg-white/[0.04] border border-white/5 space-y-1">
                <div className="text-slate-400">Educational Qualification</div>
                <div className="font-semibold text-white">{selectedVacancy.qualification}</div>
              </div>

              <div className="p-3 rounded-xl bg-white/[0.04] border border-white/5 space-y-1">
                <div className="text-slate-400">Important Dates</div>
                <div className="font-semibold text-white">
                  Application Deadline: {selectedVacancy.lastDateToApply}
                </div>
                <div className="font-semibold text-white">
                  Exam Date: {selectedVacancy.examDate}
                </div>
              </div>
            </div>

            <div className="pt-2 flex gap-2">
              <a
                href={selectedVacancy.officialUrl}
                target="_blank"
                rel="noreferrer"
                className="flex-1 py-2.5 rounded-xl bg-gradient-to-r from-amber-500 to-amber-600 hover:from-amber-400 text-slate-950 font-bold text-xs flex items-center justify-center gap-1.5 shadow-lg shadow-amber-500/20"
              >
                <ExternalLink className="h-3.5 w-3.5" />
                Visit Official Portal / Apply
              </a>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
