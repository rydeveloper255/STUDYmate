import React, { useState, useEffect, lazy, Suspense } from 'react';
import {
  UserProfile,
  ExamInfo,
  SubjectItem,
  StudyPlanItem,
  Flashcard,
  Question,
  VacancyItem,
  FocusSession,
  MockTestAttempt,
  NovaMemoryItem,
  StudyNotification,
} from './types';
import {
  initialUserProfile,
  initialExams,
  initialSubjects,
  initialStudyPlan,
  initialFlashcards,
  initialPyqQuestions,
  initialVacancies,
  initialMemories,
  initialNotifications,
} from './lib/mockData';
import { triggerHaptic } from './lib/haptics';

import { HeaderBar } from './components/HeaderBar';
import { FloatingGlassNavBar, TabKey } from './components/FloatingGlassNavBar';
import { NovaFloatingOrb } from './components/NovaFloatingOrb';
import { DailyBriefingModal } from './components/DailyBriefingModal';
import { OfflineIndicator } from './components/OfflineIndicator';
import { PrivacyTermsModal } from './components/PrivacyTermsModal';

import { HomeScreen } from './screens/HomeScreen';
import { NovaScreen } from './screens/NovaScreen';
import { StudyHubScreen } from './screens/StudyHubScreen';
import { PracticeHubScreen } from './screens/PracticeHubScreen';
import { FocusModeScreen } from './screens/FocusModeScreen';
import { UpdatesHubScreen } from './screens/UpdatesHubScreen';

// Lazy-loaded heavy modal overlays for lag-free performance
const ActiveMockTestScreen = lazy(() =>
  import('./screens/ActiveMockTestScreen').then((m) => ({ default: m.ActiveMockTestScreen }))
);
const DocumentSummarizerScreen = lazy(() =>
  import('./screens/DocumentSummarizerScreen').then((m) => ({ default: m.DocumentSummarizerScreen }))
);
const ExamReadinessScreen = lazy(() =>
  import('./screens/ExamReadinessScreen').then((m) => ({ default: m.ExamReadinessScreen }))
);
const NotificationCenterScreen = lazy(() =>
  import('./screens/NotificationCenterScreen').then((m) => ({ default: m.NotificationCenterScreen }))
);
const ProfileSettingsScreen = lazy(() =>
  import('./screens/ProfileSettingsScreen').then((m) => ({ default: m.ProfileSettingsScreen }))
);

const ScreenSuspenseLoader = () => (
  <div className="flex flex-col items-center justify-center min-h-[50vh] gap-3 text-sky-400">
    <div className="w-9 h-9 border-3 border-sky-400 border-t-transparent rounded-full animate-spin" />
    <span className="text-xs text-slate-400 font-medium">Loading high-yield module...</span>
  </div>
);

export function App() {
  // -------------------------------------------------------------
  // PERSISTENT LOCAL STATE INITIALIZATION
  // -------------------------------------------------------------
  const [user, setUser] = useState<UserProfile>(() => {
    const saved = localStorage.getItem('studymate_user');
    return saved ? JSON.parse(saved) : initialUserProfile;
  });

  const [exams, setExams] = useState<ExamInfo[]>(() => {
    const saved = localStorage.getItem('studymate_exams');
    return saved ? JSON.parse(saved) : initialExams;
  });

  const [activeExamId, setActiveExamId] = useState<string>(() => {
    const saved = localStorage.getItem('studymate_active_exam');
    return saved || 'exam_upsc_2026';
  });

  const [subjects, setSubjects] = useState<SubjectItem[]>(() => {
    const saved = localStorage.getItem('studymate_subjects');
    return saved ? JSON.parse(saved) : initialSubjects;
  });

  const [studyPlan, setStudyPlan] = useState<StudyPlanItem[]>(() => {
    const saved = localStorage.getItem('studymate_plan');
    return saved ? JSON.parse(saved) : initialStudyPlan;
  });

  const [flashcards, setFlashcards] = useState<Flashcard[]>(() => {
    const saved = localStorage.getItem('studymate_flashcards');
    return saved ? JSON.parse(saved) : initialFlashcards;
  });

  const [vacancies, setVacancies] = useState<VacancyItem[]>(() => {
    const saved = localStorage.getItem('studymate_vacancies');
    return saved ? JSON.parse(saved) : initialVacancies;
  });

  const [memories, setMemories] = useState<NovaMemoryItem[]>(() => {
    const saved = localStorage.getItem('studymate_memories');
    return saved ? JSON.parse(saved) : initialMemories;
  });

  const [notifications, setNotifications] = useState<StudyNotification[]>(() => {
    const saved = localStorage.getItem('studymate_notifs');
    return saved ? JSON.parse(saved) : initialNotifications;
  });

  const [testAttempts, setTestAttempts] = useState<MockTestAttempt[]>(() => {
    const saved = localStorage.getItem('studymate_attempts');
    return saved ? JSON.parse(saved) : [];
  });

  const [bookmarkedVacancyIds, setBookmarkedVacancyIds] = useState<string[]>(() => {
    const saved = localStorage.getItem('studymate_bookmarks');
    return saved ? JSON.parse(saved) : ['vac_1'];
  });

  const [appliedVacancyIds, setAppliedVacancyIds] = useState<string[]>(() => {
    const saved = localStorage.getItem('studymate_applied');
    return saved ? JSON.parse(saved) : [];
  });

  // -------------------------------------------------------------
  // ACTIVE NAVIGATION & MODAL OVERLAY STATES
  // -------------------------------------------------------------
  const [activeTab, setActiveTab] = useState<TabKey>('home');
  const [activeModal, setActiveModal] = useState<
    'none' | 'briefing' | 'active_test' | 'summarizer' | 'readiness' | 'notifs' | 'profile'
  >('none');
  const [showPrivacyModal, setShowPrivacyModal] = useState<boolean>(false);

  // Focus sprint state passed to Focus Shield
  const [focusSprintConfig, setFocusSprintConfig] = useState<{
    minutes: number;
    subject?: string;
    topic?: string;
  }>({ minutes: 25 });

  // Active test parameters
  const [activeTestConfig, setActiveTestConfig] = useState<{
    title: string;
    subject: string;
    questions: Question[];
  } | null>(null);

  // Nova prompt launcher
  const [novaInitialPrompt, setNovaInitialPrompt] = useState<string>('');

  // Handle Android hardware back-button navigation
  useEffect(() => {
    const handlePopState = () => {
      if (showPrivacyModal) {
        setShowPrivacyModal(false);
      } else if (activeModal !== 'none') {
        setActiveModal('none');
      } else if (activeTab !== 'home') {
        setActiveTab('home');
      }
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, [showPrivacyModal, activeModal, activeTab]);

  const handleTabChange = (tab: TabKey) => {
    triggerHaptic('light');
    setActiveTab(tab);
    setActiveModal('none');
    window.history.pushState({ tab }, '');
  };

  const handleOpenModal = (
    modal: 'briefing' | 'active_test' | 'summarizer' | 'readiness' | 'notifs' | 'profile'
  ) => {
    triggerHaptic('light');
    setActiveModal(modal);
    window.history.pushState({ modal }, '');
  };

  // -------------------------------------------------------------
  // SAVE TO LOCAL STORAGE
  // -------------------------------------------------------------
  useEffect(() => {
    localStorage.setItem('studymate_user', JSON.stringify(user));
  }, [user]);

  useEffect(() => {
    localStorage.setItem('studymate_exams', JSON.stringify(exams));
  }, [exams]);

  useEffect(() => {
    localStorage.setItem('studymate_active_exam', activeExamId);
  }, [activeExamId]);

  useEffect(() => {
    localStorage.setItem('studymate_subjects', JSON.stringify(subjects));
  }, [subjects]);

  useEffect(() => {
    localStorage.setItem('studymate_plan', JSON.stringify(studyPlan));
  }, [studyPlan]);

  useEffect(() => {
    localStorage.setItem('studymate_flashcards', JSON.stringify(flashcards));
  }, [flashcards]);

  useEffect(() => {
    localStorage.setItem('studymate_vacancies', JSON.stringify(vacancies));
  }, [vacancies]);

  useEffect(() => {
    localStorage.setItem('studymate_memories', JSON.stringify(memories));
  }, [memories]);

  useEffect(() => {
    localStorage.setItem('studymate_notifs', JSON.stringify(notifications));
  }, [notifications]);

  useEffect(() => {
    localStorage.setItem('studymate_attempts', JSON.stringify(testAttempts));
  }, [testAttempts]);

  useEffect(() => {
    localStorage.setItem('studymate_bookmarks', JSON.stringify(bookmarkedVacancyIds));
  }, [bookmarkedVacancyIds]);

  useEffect(() => {
    localStorage.setItem('studymate_applied', JSON.stringify(appliedVacancyIds));
  }, [appliedVacancyIds]);

  const activeExam = exams.find((e) => e.id === activeExamId) || exams[0];
  const unreadNotifsCount = notifications.filter((n) => !n.isRead).length;

  // -------------------------------------------------------------
  // HANDLERS
  // -------------------------------------------------------------
  const handleSelectExam = (exam: ExamInfo) => {
    setActiveExamId(exam.id);
  };

  const handleStartFocusSprint = (minutes: number, subject?: string, topic?: string) => {
    setFocusSprintConfig({ minutes, subject, topic });
    setActiveTab('focus');
    setActiveModal('none');
  };

  const handleOpenNovaWithPrompt = (prompt?: string) => {
    if (prompt) setNovaInitialPrompt(prompt);
    setActiveTab('nova');
    setActiveModal('none');
  };

  const handleToggleTopic = (subId: string, chapId: string, topId: string) => {
    setSubjects((prev) =>
      prev.map((sub) => {
        if (sub.id !== subId) return sub;
        const updatedChapters = sub.chapters.map((chap) => {
          if (chap.id !== chapId) return chap;
          return {
            ...chap,
            topics: chap.topics.map((top) =>
              top.id === topId ? { ...top, isCompleted: !top.isCompleted } : top
            ),
          };
        });

        const totalTopics = updatedChapters.reduce((acc, c) => acc + c.topics.length, 0);
        const completedTopics = updatedChapters.reduce(
          (acc, c) => acc + c.topics.filter((t) => t.isCompleted).length,
          0
        );
        const mastery = totalTopics > 0 ? Math.round((completedTopics / totalTopics) * 100) : 0;

        return {
          ...sub,
          chapters: updatedChapters,
          completedTopicsCount: completedTopics,
          masteryPercentage: mastery,
        };
      })
    );
  };

  const handleTogglePlanItem = (planId: string) => {
    setStudyPlan((prev) =>
      prev.map((p) => (p.id === planId ? { ...p, isCompleted: !p.isCompleted } : p))
    );
  };

  const handleAddPlanItem = (item: StudyPlanItem) => {
    setStudyPlan((prev) => [item, ...prev]);
  };

  const handleReviewFlashcard = (cardId: string, difficulty: 'EASY' | 'MEDIUM' | 'HARD') => {
    setFlashcards((prev) =>
      prev.map((c) => {
        if (c.id !== cardId) return c;
        const multiplier = difficulty === 'EASY' ? 2 : difficulty === 'MEDIUM' ? 1.5 : 1;
        const newInterval = Math.max(1, Math.round(c.intervalDays * multiplier));
        return {
          ...c,
          intervalDays: newInterval,
          repetitionCount: (c.repetitionCount || 0) + 1,
          nextReviewDate: `${newInterval} days`,
        };
      })
    );
  };

  const handleCompleteFocusSession = (session: FocusSession) => {
    setUser((u) => ({
      ...u,
      todayFocusedMinutes: u.todayFocusedMinutes + session.durationMinutes,
      totalFocusMinutes: (u.totalFocusMinutes || 0) + session.durationMinutes,
    }));

    // Add notification
    const newNotif: StudyNotification = {
      id: `notif_${Date.now()}`,
      title: 'Focus Sprint Completed! 🎯',
      message: `You focused for ${session.durationMinutes} minutes on ${session.topic}. Streak is rock solid!`,
      timestamp: 'Just now',
      isRead: false,
    };
    setNotifications((prev) => [newNotif, ...prev]);
  };

  const handleStartCustomTest = (title: string, subject: string, questions: Question[]) => {
    setActiveTestConfig({ title, subject, questions });
    setActiveModal('active_test');
  };

  const handleCompleteMockTest = (attempt: MockTestAttempt) => {
    setTestAttempts((prev) => [attempt, ...prev]);
    // Boost readiness slightly
    setExams((prev) =>
      prev.map((e) =>
        e.id === activeExamId
          ? { ...e, readinessScore: Math.min(99, e.readinessScore + 1) }
          : e
      )
    );
  };

  const handleResetData = () => {
    localStorage.clear();
    setUser(initialUserProfile);
    setExams(initialExams);
    setActiveExamId('exam_upsc_2026');
    setSubjects(initialSubjects);
    setStudyPlan(initialStudyPlan);
    setFlashcards(initialFlashcards);
    setVacancies(initialVacancies);
    setMemories(initialMemories);
    setNotifications(initialNotifications);
    setTestAttempts([]);
    setActiveModal('none');
    setActiveTab('home');
  };

  return (
    <div className="min-h-screen bg-[#070B19] text-slate-100 flex flex-col selection:bg-sky-500 selection:text-white relative">
      {/* Real-time Network Offline Detection Bar */}
      <OfflineIndicator />

      {/* Top Header Bar */}
      <HeaderBar
        user={user}
        activeExam={activeExam}
        allExams={exams}
        unreadNotifsCount={unreadNotifsCount}
        onSelectExam={handleSelectExam}
        onOpenNotifications={() => handleOpenModal('notifs')}
        onOpenProfile={() => handleOpenModal('profile')}
        onOpenReadiness={() => handleOpenModal('readiness')}
      />

      {/* Main Screen Container */}
      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 pt-5">
        <Suspense fallback={<ScreenSuspenseLoader />}>
          {/* Overlay Screens */}
          {activeModal === 'active_test' && activeTestConfig ? (
            <ActiveMockTestScreen
              testTitle={activeTestConfig.title}
              subject={activeTestConfig.subject}
              examName={activeExam.name}
              questions={activeTestConfig.questions}
              onCompleteTest={handleCompleteMockTest}
              onExitTest={() => {
                triggerHaptic('light');
                setActiveModal('none');
                setActiveTestConfig(null);
              }}
            />
          ) : activeModal === 'summarizer' ? (
            <DocumentSummarizerScreen
              activeExam={activeExam}
              onAddFlashcards={(cards) => setFlashcards((prev) => [...cards, ...prev])}
              onStartQuizWithQuestions={(t, s, q) => handleStartCustomTest(t, s, q)}
              onBack={() => {
                triggerHaptic('light');
                setActiveModal('none');
              }}
            />
          ) : activeModal === 'readiness' ? (
            <ExamReadinessScreen
              user={user}
              activeExam={activeExam}
              subjects={subjects}
              testAttempts={testAttempts}
              onStartFocusSprint={handleStartFocusSprint}
              onBack={() => {
                triggerHaptic('light');
                setActiveModal('none');
              }}
            />
          ) : activeModal === 'notifs' ? (
            <NotificationCenterScreen
              notifications={notifications}
              onMarkAllAsRead={() =>
                setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })))
              }
              onClearNotifications={() => setNotifications([])}
              onBack={() => {
                triggerHaptic('light');
                setActiveModal('none');
              }}
            />
          ) : activeModal === 'profile' ? (
            <ProfileSettingsScreen
              user={user}
              activeExam={activeExam}
              allExams={exams}
              onUpdateUser={(u) => setUser((prev) => ({ ...prev, ...u }))}
              onSelectExam={handleSelectExam}
              onResetData={handleResetData}
              onBack={() => {
                triggerHaptic('light');
                setActiveModal('none');
              }}
              onOpenPrivacy={() => {
                triggerHaptic('light');
                setShowPrivacyModal(true);
              }}
            />
          ) : (
            /* Primary Tabs */
            <>
              {activeTab === 'home' && (
                <HomeScreen
                  user={user}
                  activeExam={activeExam}
                  subjects={subjects}
                  vacancies={vacancies}
                  onNavigateTab={(tab) => handleTabChange(tab)}
                  onOpenNovaWithPrompt={handleOpenNovaWithPrompt}
                  onStartFocusSprint={handleStartFocusSprint}
                  onOpenSummarizer={() => handleOpenModal('summarizer')}
                  onOpenReadiness={() => handleOpenModal('readiness')}
                  onOpenDailyBriefing={() => handleOpenModal('briefing')}
                />
              )}

              {activeTab === 'nova' && (
                <NovaScreen
                  user={user}
                  activeExam={activeExam}
                  memories={memories}
                  onSaveMemory={(m) => setMemories((prev) => [m, ...prev])}
                  onDeleteMemory={(id) => setMemories((prev) => prev.filter((m) => m.id !== id))}
                  onStartFocusSprint={handleStartFocusSprint}
                  onStartQuiz={(sub, top) =>
                    handleStartCustomTest(`Quiz: ${sub}`, sub, initialPyqQuestions)
                  }
                  initialPrompt={novaInitialPrompt}
                />
              )}

              {activeTab === 'study' && (
                <StudyHubScreen
                  activeExam={activeExam}
                  subjects={subjects}
                  planItems={studyPlan}
                  flashcards={flashcards}
                  onToggleTopic={handleToggleTopic}
                  onTogglePlanItem={handleTogglePlanItem}
                  onAddPlanItem={handleAddPlanItem}
                  onStartFocusSprint={handleStartFocusSprint}
                  onOpenSummarizer={() => handleOpenModal('summarizer')}
                  onReviewFlashcard={handleReviewFlashcard}
                  onAskNovaAboutTopic={(topic, sub) =>
                    handleOpenNovaWithPrompt(`Samjhao: ${topic} (${sub}) ke core exam concepts kya hain?`)
                  }
                />
              )}

              {activeTab === 'practice' && (
                <PracticeHubScreen
                  activeExam={activeExam}
                  pyqQuestions={initialPyqQuestions}
                  testAttempts={testAttempts}
                  onStartCustomTest={handleStartCustomTest}
                  onViewDiagnosticReport={(att) => {
                    setActiveTestConfig({
                      title: att.testTitle,
                      subject: att.subject,
                      questions: att.questions || initialPyqQuestions,
                    });
                    handleOpenModal('active_test');
                  }}
                />
              )}

              {activeTab === 'focus' && (
                <FocusModeScreen
                  user={user}
                  activeExam={activeExam}
                  initialMinutes={focusSprintConfig.minutes}
                  initialSubject={focusSprintConfig.subject}
                  initialTopic={focusSprintConfig.topic}
                  onCompleteSession={handleCompleteFocusSession}
                  onAskNova={handleOpenNovaWithPrompt}
                />
              )}

              {activeTab === 'updates' && (
                <UpdatesHubScreen
                  vacancies={vacancies}
                  bookmarkedIds={bookmarkedVacancyIds}
                  appliedIds={appliedVacancyIds}
                  onToggleBookmark={(id) => {
                    triggerHaptic('light');
                    setBookmarkedVacancyIds((prev) =>
                      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
                    );
                  }}
                  onToggleApplied={(id) => {
                    triggerHaptic('light');
                    setAppliedVacancyIds((prev) =>
                      prev.includes(id) ? prev.filter((i) => i !== id) : [...prev, id]
                    );
                  }}
                />
              )}
            </>
          )}
        </Suspense>
      </main>

      {/* Floating Glass Navigation Bar */}
      {activeModal === 'none' && (
        <FloatingGlassNavBar
          activeTab={activeTab}
          onChangeTab={(tab) => handleTabChange(tab)}
        />
      )}

      {/* Floating Glowing Nova Orb */}
      {activeModal === 'none' && activeTab !== 'nova' && (
        <NovaFloatingOrb
          onOpenNova={handleOpenNovaWithPrompt}
          activeExamName={activeExam.shortName}
        />
      )}

      {/* Morning AI Daily Briefing Modal */}
      <DailyBriefingModal
        isOpen={activeModal === 'briefing'}
        onClose={() => {
          triggerHaptic('light');
          setActiveModal('none');
        }}
        activeExam={activeExam}
        user={user}
        onStartSprint={handleStartFocusSprint}
      />

      {/* Play Store & Compliance Privacy Policy Modal */}
      <PrivacyTermsModal
        isOpen={showPrivacyModal}
        onClose={() => {
          triggerHaptic('light');
          setShowPrivacyModal(false);
        }}
      />
    </div>
  );
}
export default App;
