import { ExamInfo, SubjectItem, VacancyItem, AppNotification, Flashcard, StudyPlanItem, NovaMemoryItem, Question, UserProfile } from '../types';

export const INITIAL_EXAMS: ExamInfo[] = [
  {
    id: 'upsc_cse',
    name: 'UPSC Civil Services Examination (IAS/IPS)',
    shortName: 'UPSC CSE',
    category: 'UPSC',
    daysRemaining: 42,
    examDate: 'May 25, 2026',
    totalCandidates: '11.5 Lakhs',
    syllabusCoveragePercent: 68,
    readinessScore: 74,
    subjects: ['Polity & Governance', 'Indian Economy', 'Modern History', 'Environment & Ecology', 'CSAT & Reasoning']
  },
  {
    id: 'ssc_cgl',
    name: 'SSC Combined Graduate Level (CGL Tier-I)',
    shortName: 'SSC CGL',
    category: 'SSC',
    daysRemaining: 65,
    examDate: 'July 14, 2026',
    totalCandidates: '24.8 Lakhs',
    syllabusCoveragePercent: 82,
    readinessScore: 88,
    subjects: ['Quantitative Aptitude', 'General Intelligence & Reasoning', 'English Comprehension', 'General Awareness']
  },
  {
    id: 'ibps_po',
    name: 'IBPS Probationary Officer (PO / MT)',
    shortName: 'IBPS PO',
    category: 'BANKING',
    daysRemaining: 88,
    examDate: 'August 18, 2026',
    totalCandidates: '8.2 Lakhs',
    syllabusCoveragePercent: 60,
    readinessScore: 69,
    subjects: ['Data Analysis & Interpretation', 'Reasoning & Computer Aptitude', 'Banking & Economy Awareness', 'English Language']
  },
  {
    id: 'jee_main',
    name: 'JEE Main (Joint Entrance Examination)',
    shortName: 'JEE Main',
    category: 'ENGINEERING',
    daysRemaining: 34,
    examDate: 'April 8, 2026',
    totalCandidates: '12.2 Lakhs',
    syllabusCoveragePercent: 78,
    readinessScore: 81,
    subjects: ['Physics (Mechanics & Electrodynamics)', 'Chemistry (Organic & Physical)', 'Mathematics (Calculus & Algebra)']
  },
  {
    id: 'neet_ug',
    name: 'NEET UG (National Eligibility cum Entrance Test)',
    shortName: 'NEET UG',
    category: 'MEDICAL',
    daysRemaining: 28,
    examDate: 'May 4, 2026',
    totalCandidates: '23.5 Lakhs',
    syllabusCoveragePercent: 85,
    readinessScore: 86,
    subjects: ['Human Physiology & Botany', 'Zoology & Genetics', 'Organic & Inorganic Chemistry', 'Physics Mechanics & Optics']
  }
];

export const INITIAL_SUBJECTS: SubjectItem[] = [
  {
    id: 'sub_polity',
    name: 'Polity & Governance',
    color: 'from-sky-500 to-blue-600',
    iconName: 'Scale',
    chaptersCount: 6,
    completedTopicsCount: 14,
    totalTopicsCount: 18,
    masteryPercentage: 77,
    chapters: [
      {
        id: 'ch_fr',
        name: 'Fundamental Rights & DPSP',
        subjectId: 'sub_polity',
        topics: [
          { id: 't_art14', name: 'Article 14 - Right to Equality & Reasonable Classification', isCompleted: true, difficulty: 'Medium', estimatedMinutes: 30, revisionsCount: 3 },
          { id: 't_art21', name: 'Article 21 - Right to Life, Privacy & Due Process of Law', isCompleted: true, difficulty: 'Hard', estimatedMinutes: 45, revisionsCount: 4 },
          { id: 't_dpsp', name: 'Directive Principles vs Fundamental Rights Conflict Cases', isCompleted: false, difficulty: 'Hard', estimatedMinutes: 40, revisionsCount: 1, isWeakArea: true }
        ]
      },
      {
        id: 'ch_parl',
        name: 'Parliament & Judiciary',
        subjectId: 'sub_polity',
        topics: [
          { id: 't_bills', name: 'Money Bill vs Financial Bills Procedure (Art 110)', isCompleted: true, difficulty: 'Medium', estimatedMinutes: 25, revisionsCount: 2 },
          { id: 't_judic', name: 'Basic Structure Doctrine & Judicial Review Landmark Verdicts', isCompleted: false, difficulty: 'Hard', estimatedMinutes: 50, revisionsCount: 0, isWeakArea: true }
        ]
      }
    ]
  },
  {
    id: 'sub_economy',
    name: 'Indian Economy & Macroeconomics',
    color: 'from-emerald-500 to-teal-600',
    iconName: 'TrendingUp',
    chaptersCount: 5,
    completedTopicsCount: 10,
    totalTopicsCount: 16,
    masteryPercentage: 62,
    chapters: [
      {
        id: 'ch_monetary',
        name: 'Monetary Policy & Banking',
        subjectId: 'sub_economy',
        topics: [
          { id: 't_repo', name: 'Repo Rate, Reverse Repo & Standing Deposit Facility (SDF)', isCompleted: true, difficulty: 'Easy', estimatedMinutes: 20, revisionsCount: 3 },
          { id: 't_inflation', name: 'Headline vs Core Inflation & CPI / WPI Basket Weights', isCompleted: false, difficulty: 'Medium', estimatedMinutes: 35, revisionsCount: 1, isWeakArea: true },
          { id: 't_npa', name: 'Insolvency and Bankruptcy Code (IBC) & Bad Banks', isCompleted: true, difficulty: 'Medium', estimatedMinutes: 30, revisionsCount: 2 }
        ]
      }
    ]
  },
  {
    id: 'sub_history',
    name: 'Modern Indian History',
    color: 'from-amber-500 to-orange-600',
    iconName: 'BookOpen',
    chaptersCount: 7,
    completedTopicsCount: 18,
    totalTopicsCount: 22,
    masteryPercentage: 81,
    chapters: [
      {
        id: 'ch_freedom',
        name: 'Freedom Struggle & National Movements',
        subjectId: 'sub_history',
        topics: [
          { id: 't_ncm', name: 'Non-Cooperation Movement (1920) Causes & Impact', isCompleted: true, difficulty: 'Easy', estimatedMinutes: 25, revisionsCount: 4 },
          { id: 't_cdm', name: 'Civil Disobedience Movement & Gandhi-Irwin Pact', isCompleted: true, difficulty: 'Medium', estimatedMinutes: 30, revisionsCount: 3 },
          { id: 't_quit', name: 'Quit India Movement 1942 & INA Trials', isCompleted: true, difficulty: 'Medium', estimatedMinutes: 35, revisionsCount: 2 }
        ]
      }
    ]
  },
  {
    id: 'sub_science',
    name: 'General Science & Technology',
    color: 'from-purple-500 to-indigo-600',
    iconName: 'Cpu',
    chaptersCount: 4,
    completedTopicsCount: 8,
    totalTopicsCount: 15,
    masteryPercentage: 53,
    chapters: [
      {
        id: 'ch_space',
        name: 'Space, AI & Defence Tech',
        subjectId: 'sub_science',
        topics: [
          { id: 't_isro', name: 'Gaganyaan & Aditya L1 Lagrange Points Trajectory', isCompleted: true, difficulty: 'Medium', estimatedMinutes: 30, revisionsCount: 2 },
          { id: 't_semicon', name: 'Semiconductor Fabrication & India Semiconductor Mission', isCompleted: false, difficulty: 'Hard', estimatedMinutes: 40, revisionsCount: 0, isWeakArea: true }
        ]
      }
    ]
  }
];

export const INITIAL_FLASHCARDS: Flashcard[] = [
  {
    id: 'fc_1',
    deckId: 'deck_polity',
    subject: 'Polity & Governance',
    topic: 'Judicial Writs',
    front: 'Which writ is known as the "Bulwark of Personal Freedom" and can be issued against both public and private entities?',
    back: 'Habeas Corpus ("To have the body of"). It commands the producing of a detained person before court.',
    hint: 'Article 32 & 226 remedy against unlawful detention.',
    difficulty: 'MEDIUM',
    intervalDays: 4,
    repetitionCount: 3,
    easeFactor: 2.5,
    nextReviewDate: '2026-05-20'
  },
  {
    id: 'fc_2',
    deckId: 'deck_polity',
    subject: 'Polity & Governance',
    topic: 'Basic Structure',
    front: 'Which landmark Supreme Court judgment in 1973 established the "Basic Structure Doctrine"?',
    back: 'Kesavananda Bharati v. State of Kerala (13-judge bench, 7-6 majority verdict).',
    hint: '50th anniversary observed in 2023.',
    difficulty: 'EASY',
    intervalDays: 7,
    repetitionCount: 5,
    easeFactor: 2.8,
    nextReviewDate: '2026-05-22'
  },
  {
    id: 'fc_3',
    deckId: 'deck_economy',
    subject: 'Indian Economy',
    topic: 'Monetary Policy',
    front: 'What is the statutory composition and inflation target of the RBI Monetary Policy Committee (MPC)?',
    back: '6 members (3 from RBI, 3 appointed by Central Govt). Target: 4% CPI Inflation with a tolerance band of +/- 2% (i.e. 2% to 6%).',
    hint: 'Chaired ex-officio by the RBI Governor.',
    difficulty: 'HARD',
    intervalDays: 2,
    repetitionCount: 2,
    easeFactor: 2.2,
    nextReviewDate: '2026-05-18'
  },
  {
    id: 'fc_4',
    deckId: 'deck_science',
    subject: 'General Science',
    topic: 'Space Tech',
    front: 'At which Lagrange Point is the Aditya-L1 solar observatory halo-orbited?',
    back: 'Sun-Earth L1 point, located ~1.5 million km from Earth towards the Sun for uninterrupted solar viewing.',
    hint: 'Gravitational equilibrium point.',
    difficulty: 'MEDIUM',
    intervalDays: 5,
    repetitionCount: 3,
    easeFactor: 2.6,
    nextReviewDate: '2026-05-21'
  }
];

export const INITIAL_PLAN_ITEMS: StudyPlanItem[] = [
  {
    id: 'plan_1',
    subject: 'Polity & Governance',
    topic: 'Landmark SC Verdicts & Basic Structure',
    durationMinutes: 35,
    priority: 'HIGH',
    isCompleted: false,
    scheduledTime: '09:00 AM',
    notes: 'Focus on 24th vs 42nd Constitutional Amendment clauses'
  },
  {
    id: 'plan_2',
    subject: 'Indian Economy',
    topic: 'RBI Monetary Policy & Inflation Metrics',
    durationMinutes: 40,
    priority: 'HIGH',
    isCompleted: true,
    scheduledTime: '11:30 AM',
    notes: 'Revise Core vs Headline CPI basket weights'
  },
  {
    id: 'plan_3',
    subject: 'Modern History',
    topic: 'Active Recall Flashcards (20 Cards)',
    durationMinutes: 20,
    priority: 'MEDIUM',
    isCompleted: false,
    scheduledTime: '03:00 PM',
    notes: 'Spaced repetition queue review'
  },
  {
    id: 'plan_4',
    subject: 'CSAT & Reasoning',
    topic: 'Speed Quant: Percentages & Data Interpretation',
    durationMinutes: 30,
    priority: 'LOW',
    isCompleted: false,
    scheduledTime: '06:00 PM',
    notes: 'Solve 15 timed practice problems'
  }
];

export const INITIAL_MEMORIES: NovaMemoryItem[] = [
  {
    id: 'mem_1',
    category: 'GOALS',
    key: 'Target Exam',
    value: 'UPSC CSE 2026 - Aiming for Top 100 Rank',
    timestamp: 'Yesterday'
  },
  {
    id: 'mem_2',
    category: 'MISTAKES',
    key: 'Weak Topic Trap',
    value: 'Tendency to confuse DPSP Article 39(b)(c) priority with Art 14',
    timestamp: '2 days ago'
  },
  {
    id: 'mem_3',
    category: 'STUDY_PREFERENCES',
    key: 'Study Sprint Preference',
    value: 'Prefers 25-minute Pomodoro sprints with ambient binaural focus music',
    timestamp: '3 days ago'
  }
];

export const SAMPLE_PYQ_QUESTIONS: Question[] = [
  {
    id: 'pyq_1',
    questionText: 'With reference to the Indian economy, demand-pull inflation can be caused/increased by which of the following?\n1. Expansionary policies\n2. Fiscal stimulus\n3. Inflation-indexing wages\n4. Higher purchasing power\n5. Rising interest rates\n\nSelect the correct answer using the code given below:',
    options: [
      '1, 2 and 4 only',
      '3, 4 and 5 only',
      '1, 2, 3 and 4 only',
      '1, 2, 3, 4 and 5'
    ],
    correctOptionIndex: 2,
    explanation: 'Demand-pull inflation occurs when aggregate demand outpaces aggregate supply. Expansionary policies, fiscal stimulus, wage indexing, and higher purchasing power all augment aggregate demand. Rising interest rates contract demand, acting as a deflationary measure.',
    subject: 'Indian Economy',
    topic: 'Inflation & Fiscal Policy',
    difficulty: 'Hard',
    source: 'PREVIOUS_YEAR',
    sourceLabel: 'UPSC CSE 2021 Prelims Paper 1',
    yearOrTag: '2021 PYQ'
  },
  {
    id: 'pyq_2',
    questionText: 'Consider the following statements regarding the Attorney General of India:\n1. He is appointed by the President of India.\n2. He must have the same qualifications as are required for a judge of the Supreme Court.\n3. He can take part in the proceedings of either House of Parliament without the right to vote.\n\nWhich of the statements given above are correct?',
    options: [
      '1 and 2 only',
      '2 and 3 only',
      '1 and 3 only',
      '1, 2 and 3'
    ],
    correctOptionIndex: 3,
    explanation: 'Under Article 76 and Article 88 of the Constitution, all three statements are correct. The Attorney General is appointed by the President, must qualify as a Supreme Court judge, and has the right to speak and take part in Parliament proceedings without voting rights.',
    subject: 'Polity & Governance',
    topic: 'Constitutional Bodies',
    difficulty: 'Medium',
    source: 'PREVIOUS_YEAR',
    sourceLabel: 'UPSC CSE 2019 Prelims',
    yearOrTag: '2019 PYQ'
  },
  {
    id: 'pyq_3',
    questionText: 'Which of the following protected areas is well-known for the conservation of a sub-species of the Indian swamp deer (Barasingha) that thrives well on hard ground and is exclusively graminivorous?',
    options: [
      'Kanha National Park',
      'Manas National Park',
      'Mudumalai Wildlife Sanctuary',
      'Tal Chhapar Sanctuary'
    ],
    correctOptionIndex: 0,
    explanation: 'The hard-ground barasingha (Rucervus duvaucelii branderi) is the state animal of Madhya Pradesh and found exclusively in Kanha National Park through dedicated conservation initiatives.',
    subject: 'Environment & Ecology',
    topic: 'National Parks & Wildlife',
    difficulty: 'Medium',
    source: 'PREVIOUS_YEAR',
    sourceLabel: 'UPSC CSE 2020 Prelims',
    yearOrTag: '2020 PYQ'
  },
  {
    id: 'pyq_4',
    questionText: 'In the context of space exploration, what is the significance of the "Lagrange Point L1" chosen for the Aditya-L1 mission?',
    options: [
      'It provides continuous, uninterrupted view of the Sun without any occultation/eclipses',
      'It allows the satellite to land on the solar corona surface',
      'It requires zero propellant to reach from Low Earth Orbit',
      'It is located completely outside Earth’s gravitational field'
    ],
    correctOptionIndex: 0,
    explanation: 'L1 is a point of gravitational equilibrium between the Sun and Earth ~1.5 million km away. A satellite in halo orbit around L1 observes the Sun continuously without eclipses or occultation.',
    subject: 'Science & Technology',
    topic: 'Space Exploration',
    difficulty: 'Easy',
    source: 'CURATED',
    sourceLabel: 'High-Yield Current Affairs 2024-2025',
    yearOrTag: 'Expected Pattern'
  }
];

export const INITIAL_VACANCIES: VacancyItem[] = [
  {
    id: 'vac_upsc_2026',
    title: 'Civil Services Examination (CSE) 2026',
    organization: 'Union Public Service Commission (UPSC)',
    category: 'VACANCY',
    totalPosts: '1,056 Posts',
    qualification: 'Graduation Degree in any discipline from a recognized University',
    ageLimit: '21 to 32 Years (Relaxation as per norms)',
    salary: 'Level 10 (₹56,100 to ₹1,77,500)',
    applyStartDate: 'Feb 14, 2026',
    lastDateToApply: 'March 05, 2026',
    examDate: 'May 25, 2026',
    isBookmarked: true,
    isApplied: true,
    eligibilitySnippet: 'Indian Citizen, Degree Holder. 6 attempts for General category.',
    description: 'Premier recruitment for IAS, IPS, IFS, IRS and other Group A/B central services.',
    importantDates: [
      { label: 'Notification Released', date: 'Feb 14, 2026' },
      { label: 'Last Date to Apply', date: 'March 05, 2026 (6:00 PM)' },
      { label: 'Prelims Exam Date', date: 'May 25, 2026' },
      { label: 'Mains Examination', date: 'Sept 19, 2026' }
    ]
  },
  {
    id: 'vac_ssc_cgl_2026',
    title: 'Combined Graduate Level Examination (CGL) 2026',
    organization: 'Staff Selection Commission (SSC)',
    category: 'VACANCY',
    totalPosts: '14,500+ Posts',
    qualification: 'Bachelor\'s Degree in any stream',
    ageLimit: '18 to 30/32 Years',
    salary: 'Level 4 to Level 8 (₹25,500 to ₹1,51,100)',
    applyStartDate: 'June 11, 2026',
    lastDateToApply: 'July 10, 2026',
    examDate: 'Sept 2026',
    isBookmarked: true,
    isApplied: false,
    eligibilitySnippet: 'Assistant Section Officer, Inspector (Income Tax/GST), Sub-Inspector.',
    description: 'Recruitment for Group B & C posts across various Central Ministries and Departments.',
    importantDates: [
      { label: 'Online Application Opens', date: 'June 11, 2026' },
      { label: 'Application Deadline', date: 'July 10, 2026' },
      { label: 'Tier 1 Computer Based Exam', date: 'Sept 2026' }
    ]
  },
  {
    id: 'vac_sbi_po_2026',
    title: 'Probationary Officers (PO) Recruitment 2026',
    organization: 'State Bank of India (SBI)',
    category: 'VACANCY',
    totalPosts: '2,000 Posts',
    qualification: 'Graduate in any discipline',
    ageLimit: '21 to 30 Years',
    salary: 'Basic Pay ₹41,960 + 4 Advance Increments (CTC ~₹15-18 LPA)',
    applyStartDate: 'Sept 01, 2026',
    lastDateToApply: 'Sept 21, 2026',
    examDate: 'Nov 2026',
    isBookmarked: false,
    isApplied: false,
    eligibilitySnippet: 'Candidates in final year of graduation are also eligible to apply provisionally.',
    description: 'Flagship banking officer recruitment with fast-track leadership promotions.',
    importantDates: [
      { label: 'Online Registration', date: 'Sept 01, 2026' },
      { label: 'Prelims Call Letter', date: 'Oct 2026' },
      { label: 'Phase 1 Online Exam', date: 'Nov 2026' }
    ]
  },
  {
    id: 'admit_upsc_2026',
    title: 'UPSC CSE Prelims 2026 e-Admit Card Released',
    organization: 'Union Public Service Commission',
    category: 'ADMIT_CARD',
    totalPosts: 'Direct Download',
    qualification: 'Registered Candidates',
    ageLimit: 'N/A',
    salary: 'N/A',
    applyStartDate: 'May 05, 2026',
    lastDateToApply: 'May 25, 2026',
    isBookmarked: false,
    isApplied: false,
    eligibilitySnippet: 'Download hall ticket with Registration ID or Roll Number.',
    description: 'Verify exam centre coordinates, roll number, session timings (Paper 1: 9:30 AM, CSAT: 2:30 PM).',
    importantDates: [
      { label: 'Admit Card Live', date: 'May 05, 2026' },
      { label: 'Exam Date', date: 'May 25, 2026' }
    ]
  }
];

export const INITIAL_NOTIFICATIONS: AppNotification[] = [
  {
    id: 'notif_1',
    title: '🎯 Daily High-Yield Sprint Ready',
    message: 'Nova prepared a 25-min revision on DPSP & Landmark Cases for your UPSC target.',
    timeAgo: '10m ago',
    category: 'NOVA_COACH',
    isRead: false
  },
  {
    id: 'notif_2',
    title: '🔥 7-Day Study Streak Maintained!',
    message: 'You have logged 14+ hours of active study this week. Keep up the momentum!',
    timeAgo: '2h ago',
    category: 'ACHIEVEMENT',
    isRead: false
  },
  {
    id: 'notif_3',
    title: '📢 UPSC CSE Prelims 2026 Exam Countdown',
    message: '42 Days remaining. We recommend completing 2 full-length GS mock tests this weekend.',
    timeAgo: '1d ago',
    category: 'EXAM_ALERT',
    isRead: true
  }
];

export const INITIAL_STUDY_PLAN = INITIAL_PLAN_ITEMS;
export const INITIAL_PYQS = SAMPLE_PYQ_QUESTIONS;

export const initialUserProfile: UserProfile = {
  id: 'user_aspirant_1',
  name: 'Aarav Sharma',
  email: 'aarav.aspirant@studymate.ai',
  targetExamId: 'exam_upsc_2026',
  targetScore: 140,
  studyGoalMinutesPerDay: 90,
  todayFocusedMinutes: 65,
  totalFocusMinutes: 840,
  totalHoursStudied: 38,
  streakDays: 7,
  readinessScore: 78,
  preferredLanguage: 'Hinglish',
  soundEnabled: true,
  strictAppBlocker: true,
  theme: 'midnight',
};

// Aliases for seamless imports across components
export const initialExams = INITIAL_EXAMS;
export const initialSubjects = INITIAL_SUBJECTS;
export const initialStudyPlan = INITIAL_PLAN_ITEMS;
export const initialFlashcards = INITIAL_FLASHCARDS;
export const initialPyqQuestions = SAMPLE_PYQ_QUESTIONS;
export const initialVacancies = INITIAL_VACANCIES;
export const initialMemories = INITIAL_MEMORIES;
export const initialNotifications = INITIAL_NOTIFICATIONS.map((n) => ({
  id: n.id,
  title: n.title,
  message: n.message,
  timestamp: n.timeAgo,
  isRead: n.isRead,
}));

