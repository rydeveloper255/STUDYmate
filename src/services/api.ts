import { Question, NovaChatMessage, NovaStudyContext, NovaSettings } from '../types';

export interface NovaChatResponse {
  replyMarkdown: string;
  actionType: string;
  actionPayload: string | null;
  memoryToSave?: any;
  isOfflineFallback?: boolean;
}

export async function askNovaAssistant(
  userPrompt: string,
  conversationHistory: { role: string; text: string }[],
  studyContext: any,
  settings: NovaSettings,
  useThinkingMode: boolean = false,
  imageBase64: string | null = null
): Promise<NovaChatResponse> {
  const response = await fetch('/api/nova/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      userPrompt,
      conversationHistory,
      studyContext,
      settings,
      useThinkingMode,
      imageBase64,
    }),
  });

  if (!response.ok) {
    throw new Error(`Nova API failed with status ${response.status}`);
  }

  return response.json();
}

export async function explainConcept(
  topic: string,
  subject: string,
  actionType: string = 'EXPLAIN_CONCEPT',
  persona: string = 'Empathetic Socratic Tutor',
  targetExam: string = 'UPSC CSE',
  userQuery: string = ''
): Promise<{ replyMarkdown: string; isOfflineFallback: boolean }> {
  const response = await fetch('/api/tutor/explain', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      topic,
      subject,
      actionType,
      persona,
      targetExam,
      userQuery,
    }),
  });

  if (!response.ok) {
    throw new Error('Failed to fetch explanation from tutor');
  }

  return response.json();
}

export async function generateQuizQuestions(
  subject: string,
  topic: string,
  difficulty: string = 'Medium',
  count: number = 5,
  examName: string = 'UPSC CSE',
  language: string = 'English',
  mode: string = 'Practice'
): Promise<{ questions: Question[]; isOfflineFallback: boolean }> {
  const response = await fetch('/api/quiz/generate', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      subject,
      topic,
      difficulty,
      count,
      examName,
      language,
      mode,
    }),
  });

  if (!response.ok) {
    throw new Error('Failed to generate quiz questions');
  }

  return response.json();
}

export async function summarizeDocument(
  documentText: string,
  targetExam: string = 'UPSC CSE',
  subject: string = 'General'
): Promise<{
  summaryMarkdown: string;
  flashcards: { front: string; back: string; hint?: string }[];
  keyPoints: string[];
  mindMapOutline: { heading: string; subpoints: string[] }[];
  quizQuestions: Question[];
  isOfflineFallback: boolean;
}> {
  const response = await fetch('/api/document/summarize', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      documentText,
      targetExam,
      subject,
    }),
  });

  if (!response.ok) {
    throw new Error('Document summarization failed');
  }

  return response.json();
}

export async function getTestDiagnostic(
  examName: string,
  subject: string,
  score: number,
  totalQuestions: number,
  accuracyPercent: number,
  timeSpentSeconds: number,
  weakTopics: string[],
  strongTopics: string[],
  incorrectQuestions: any[]
): Promise<{ diagnosticMarkdown: string; isOfflineFallback: boolean }> {
  const response = await fetch('/api/tutor/diagnose', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      examName,
      subject,
      score,
      totalQuestions,
      accuracyPercent,
      timeSpentSeconds,
      weakTopics,
      strongTopics,
      incorrectQuestions,
    }),
  });

  if (!response.ok) {
    throw new Error('Diagnostic analysis failed');
  }

  return response.json();
}
