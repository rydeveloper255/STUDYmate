export type NavigationTab = 
  | 'dashboard'
  | 'nova-tutor'
  | 'practice'
  | 'study-hub'
  | 'flashcards'
  | 'planner'
  | 'focus-shield'
  | 'updates'
  | 'analytics';

export type ExamCategory = 
  | 'UPSC / Civil Services'
  | 'SSC CGL / CHSL'
  | 'Banking (IBPS/SBI PO)'
  | 'Railways (RRB NTPC)'
  | 'Engineering (GATE/JEE)'
  | 'Medical (NEET)'
  | 'State PSC & Police'
  | 'General Academic';

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  targetExam: ExamCategory;
  examDate: string;
  dailyStudyGoalMinutes: number;
  streakDays: number;
  xpPoints: number;
  level: number;
  coins: number;
  avatarSeed: string;
  selectedLanguage: 'en' | 'hi' | 'hinglish';
  weakTopics: string[];
  strongTopics: string[];
}

export interface NovaChatMessage {
  id: string;
  sender: 'user' | 'nova' | 'system';
  text: string;
  timestamp: string;
  mode?: 'tutor' | 'doubt-solver' | 'quiz' | 'summarizer' | 'hinglish-motivation';
  isVoiceSpoken?: boolean;
  actionSuggestions?: string[];
  relatedTopic?: string;
  citations?: string[];
}

export interface Question {
  id: string;
  subject: string;
  topic: string;
  difficulty: 'Easy' | 'Medium' | 'Hard';
  examTag: string;
  year?: string;
  questionText: string;
  options: string[];
  correctOptionIndex: number;
  detailedExplanation: string;
  trickOrShortCut?: string;
  conceptTag?: string;
}

export interface MockTest {
  id: string;
  title: string;
  category: ExamCategory;
  totalQuestions: number;
  durationMinutes: number;
  difficulty: 'Easy' | 'Medium' | 'Hard' | 'Adaptive';
  attemptsCount: number;
  questions: Question[];
  avgScorePercent?: number;
}

export interface MockTestResult {
  id: string;
  testId: string;
  testTitle: string;
  dateCompleted: string;
  totalQuestions: number;
  correctCount: number;
  incorrectCount: number;
  unattemptedCount: number;
  score: number;
  totalMarks: number;
  timeSpentSeconds: number;
  accuracyRate: number;
  userAnswers: { [questionId: string]: number };
  subjectBreakdown: { [subject: string]: { correct: number; total: number } };
  novaAiFeedback?: string;
}

export interface Flashcard {
  id: string;
  deckId: string;
  deckName: string;
  front: string;
  back: string;
  subject: string;
  difficulty: 'Easy' | 'Medium' | 'Hard';
  lastReviewed?: string;
  nextReview?: string;
  intervalDays: number;
  easeFactor: number;
  reviewCount: number;
}

export interface StudyMaterial {
  id: string;
  title: string;
  subject: string;
  category: string;
  summary: string;
  readTimeMinutes: number;
  contentMarkdown: string;
  isBookmarked: boolean;
  isGkWeekly?: boolean;
  keyPoints: string[];
  mindmapNodes?: { title: string; children: string[] }[];
  downloadUrl?: string;
}

export interface LiveExamUpdate {
  id: string;
  title: string;
  category: ExamCategory;
  organization: string;
  date: string;
  status: 'Registration Open' | 'Admit Card Released' | 'Results Declared' | 'Notification Out' | 'Syllabus Updated';
  linkText: string;
  officialUrl: string;
  summary: string;
  vacanciesCount?: number;
  importantDates: { event: string; date: string }[];
  eligibilityNotes?: string;
}

export interface StudyTask {
  id: string;
  title: string;
  subject: string;
  durationMinutes: number;
  scheduledTime: string;
  completed: boolean;
  priority: 'High' | 'Medium' | 'Low';
  energyLevelReq: 'High' | 'Medium' | 'Low';
}

export interface FocusSessionLog {
  id: string;
  startTime: string;
  durationMinutes: number;
  subject: string;
  mode: 'Pomodoro' | 'Deep Focus' | 'Exam Simulation';
  distractionsBlocked: number;
  completedSuccessfully: boolean;
  notes?: string;
}

export interface MistakeEntry {
  id: string;
  questionId: string;
  questionText: string;
  userChoice: string;
  correctChoice: string;
  subject: string;
  topic: string;
  whyMissed: 'Conceptual Error' | 'Silly Mistake / Calculation' | 'Time Pressure' | 'Guesswork';
  solutionExplanation: string;
  dateAdded: string;
  resolved: boolean;
}
