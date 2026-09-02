export type ExamCategory =
  | 'UPSC'
  | 'SSC'
  | 'BANKING'
  | 'ENGINEERING'
  | 'MEDICAL'
  | 'STATE_PSC'
  | 'TEACHING'
  | 'DEFENCE'
  | 'SCHOOL';

export interface ExamInfo {
  id: string;
  name: string;
  shortName: string;
  category: ExamCategory;
  daysRemaining: number;
  examDate: string;
  totalCandidates: string;
  syllabusCoveragePercent: number;
  readinessScore: number; // 0 - 100
  subjects: string[];
}

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  targetExamId: string;
  targetScore: number;
  studyGoalMinutesPerDay: number;
  todayFocusedMinutes: number;
  totalFocusMinutes?: number;
  totalHoursStudied: number;
  streakDays: number;
  readinessScore: number;
  preferredLanguage: 'English' | 'Hindi' | 'Hinglish';
  soundEnabled: boolean;
  strictAppBlocker: boolean;
  theme: 'dark' | 'midnight' | 'amoled';
}

export interface TopicItem {
  id: string;
  name: string;
  isCompleted: boolean;
  difficulty: 'Easy' | 'Medium' | 'Hard';
  estimatedMinutes: number;
  lastStudiedDate?: string;
  revisionsCount: number;
  notes?: string;
  isWeakArea?: boolean;
}

export interface ChapterItem {
  id: string;
  name: string;
  subjectId: string;
  topics: TopicItem[];
}

export interface SubjectItem {
  id: string;
  name: string;
  color: string; // Tailwind color class or hex
  iconName: string;
  chaptersCount: number;
  completedTopicsCount: number;
  totalTopicsCount: number;
  masteryPercentage: number;
  chapters: ChapterItem[];
}

export interface StudyPlanItem {
  id: string;
  subject: string;
  topic: string;
  durationMinutes: number;
  priority: 'HIGH' | 'MEDIUM' | 'LOW';
  isCompleted: boolean;
  scheduledTime: string;
  notes?: string;
}

export interface Flashcard {
  id: string;
  deckId: string;
  subject: string;
  topic: string;
  front: string;
  back: string;
  hint?: string;
  difficulty: 'EASY' | 'MEDIUM' | 'HARD';
  intervalDays: number;
  repetitionCount: number;
  easeFactor: number;
  nextReviewDate: string; // YYYY-MM-DD
}

export interface FlashcardDeck {
  id: string;
  title: string;
  subject: string;
  cardsCount: number;
  dueTodayCount: number;
  masteryPercentage: number;
}

export interface Question {
  id: string;
  questionText: string;
  options: string[];
  correctOptionIndex: number;
  explanation: string;
  subject: string;
  topic: string;
  difficulty: 'Easy' | 'Medium' | 'Hard';
  source?: 'PREVIOUS_YEAR' | 'AI_GENERATED' | 'CURATED';
  sourceLabel?: string;
  yearOrTag?: string;
}

export interface MockTestAttempt {
  id: string;
  testTitle: string;
  examName: string;
  subject: string;
  timestamp: number;
  totalQuestions: number;
  correctCount: number;
  incorrectCount: number;
  unattemptedCount: number;
  score: number;
  accuracyPercent: number;
  timeSpentSeconds: number;
  weakTopics: string[];
  strongTopics: string[];
  diagnosticReport?: string;
  userAnswers: { [questionId: string]: number }; // questionId -> selectedOptionIndex
  markedForReview: string[];
  questions: Question[];
}

export interface NovaMemoryItem {
  id: string;
  category: 'STUDY_PREFERENCES' | 'ACADEMIC' | 'GOALS' | 'MISTAKES';
  key: string;
  value: string;
  timestamp: string;
}

export interface NovaChatMessage {
  id: string;
  sender: 'user' | 'assistant';
  text: string;
  timestamp: string;
  actionType?: string;
  actionPayload?: string;
  memoryItem?: NovaMemoryItem;
  imageUri?: string;
  isAudioPlaying?: boolean;
}

export interface NovaSettings {
  useBossGreeting: boolean;
  voiceEnabled: boolean;
  memoryEnabled: boolean;
  thinkingMode: boolean;
  selectedPersona: 'Empathetic Socratic Tutor' | 'Strict Exam Strategist' | 'ELI5 Friendly Mentor';
}

export interface FocusSessionLog {
  id: string;
  startTime?: number;
  durationMinutes: number;
  subject: string;
  topic: string;
  notes?: string;
  distractionAttemptsCount?: number;
}

export type FocusSession = FocusSessionLog;

export type UpdateCategory = 'VACANCY' | 'ADMIT_CARD' | 'RESULT' | 'ANSWER_KEY' | 'ADMISSION';

export interface VacancyItem {
  id: string;
  title: string;
  organization: string;
  category: UpdateCategory;
  totalPosts: string;
  qualification: string;
  ageLimit: string;
  salary: string;
  salaryPayScale?: string;
  applyStartDate: string;
  lastDateToApply: string;
  examDate?: string;
  officialNotificationUrl?: string;
  officialUrl?: string;
  applyOnlineUrl?: string;
  isBookmarked: boolean;
  isApplied: boolean;
  eligibilitySnippet: string;
  description: string;
  importantDates: { label: string; date: string }[];
}

export interface AppNotification {
  id: string;
  title: string;
  message: string;
  timeAgo?: string;
  timestamp?: string;
  category?: 'EXAM_ALERT' | 'STUDY_REMINDER' | 'NOVA_COACH' | 'ACHIEVEMENT';
  isRead: boolean;
  actionUrl?: string;
}

export type StudyNotification = AppNotification;

export interface NovaStudyContext {
  studentName: string;
  targetExam: string;
  examDaysRemaining: number;
  subjects: string[];
  weakTopics: string[];
  strongTopics: string[];
  dailyTargetMinutes: number;
  todayFocusMinutes: number;
  currentStreak: number;
  preferredLanguage: string;
  preferredStudyDurationMins?: number;
  memories?: any[];
}

export interface StudyDocument {
  id: string;
  title: string;
  subject: string;
  originalText: string;
  dateAdded: string;
  summaryMarkdown: string;
  keyPoints: string[];
  mindMapOutline: { heading: string; subpoints: string[] }[];
  flashcardsCount: number;
}
