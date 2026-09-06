import express from 'express';
import path from 'path';
import { GoogleGenAI } from '@google/genai';

const distPath = path.join(process.cwd(), 'dist');

const app = express();
app.use(express.json({ limit: '25mb' }));

const PORT = 3000;

// Lazy initialization of Gemini client
let genAIClient: GoogleGenAI | null = null;
function getGenAI(): GoogleGenAI | null {
  const apiKey = process.env.GEMINI_API_KEY;
  if (!apiKey || apiKey === 'dummy_key') {
    return null;
  }
  if (!genAIClient) {
    genAIClient = new GoogleGenAI({
      apiKey,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build',
        },
      },
    });
  }
  return genAIClient;
}

// Health Check
app.get('/api/health', (req, res) => {
  res.json({
    status: 'ok',
    geminiConfigured: !!process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY !== 'dummy_key',
    elevenLabsConfigured: !!process.env.ELEVENLABS_API_KEY,
    timestamp: new Date().toISOString(),
  });
});

// Helper: Build Nova system instruction
function getNovaSystemPrompt(studyContext: any, settings: any, persona?: string) {
  const exam = studyContext?.targetExam || 'Competitive Exam (UPSC / SSC / Banking)';
  const studentName = studyContext?.studentName || 'Aspirant';
  const language = studyContext?.preferredLanguage || 'Hinglish';
  const weakTopics = Array.isArray(studyContext?.weakTopics) ? studyContext.weakTopics.join(', ') : '';

  return `You are "Nova 2.0", an elite, empathetic, and exceptionally intelligent AI Study Coach & Exam Mentor for Indian competitive exam aspirants (targeting ${exam}).
Candidate Name: ${studentName}
Dialect / Language: ${language} (Use natural, encouraging Hinglish or clear English according to candidate query)
Persona: ${persona || settings?.selectedPersona || 'Empathetic Socratic Tutor'}
Target Exam: ${exam}
Identified Weak Areas: ${weakTopics || 'General revision'}

Core Pedagogical Directives:
1. Break down complex topics into clear, digestible, numbered or bulleted concepts.
2. Mark high-yield points with "🎯 High-Yield Exam Note:".
3. Provide mnemonics, elimination shortcuts, or formulas where applicable.
4. If an image is provided (handwritten or printed question/diagram), transcribe the question first, state the core principle, eliminate wrong options step-by-step, and state the exact correct answer.
5. Provide actionable next steps. If user asks for a quiz or focus session, suggest starting a Focus Sprint or Mock Test.
6. Return formatted GitHub-flavored markdown with clean spacing and clear bold headers.`;
}

// 1. Nova AI Assistant & Multimodal Chat
async function handleNovaChat(req: express.Request, res: express.Response) {
  try {
    const {
      userPrompt,
      conversationHistory,
      studyContext,
      settings,
      useThinkingMode,
      imageBase64,
      messages,
      userProfile,
      mode,
    } = req.body;

    const promptText = userPrompt || (messages && messages.length > 0 ? messages[messages.length - 1].text : 'Hello Nova');
    const ai = getGenAI();

    if (!ai) {
      // Offline fallback with contextual advice
      const name = studyContext?.studentName || userProfile?.name || 'Aspirant';
      const examName = studyContext?.targetExam || userProfile?.targetExam || 'Competitive Exam';
      return res.json({
        replyMarkdown: `### 🌟 Nova 2.0 Offline Study Engine\n\nNamaste **${name}**! For your **${examName}** preparation:\n\n1. **Core Concept**: Focus on high-frequency questions and core statutory/factual principles.\n2. **Active Recall**: Solve 5 targeted MCQs in the Practice Hub to lock this concept into long-term memory.\n3. **Quick Strategy**: Connect recent current affairs updates with static syllabus notes!\n\n*Target: 25-min high-yield sprint recommended.*`,
        text: `Namaste ${name}! Focus on high-yield static concepts for ${examName}. Would you like to start a 25-minute practice sprint?`,
        actionType: 'START_FOCUS',
        actionPayload: JSON.stringify({ minutes: 25, subject: 'General Studies', topic: 'Core Revision' }),
        isOfflineFallback: true,
      });
    }

    const systemPrompt = getNovaSystemPrompt(studyContext, settings, settings?.selectedPersona);
    const parts: any[] = [{ text: systemPrompt }];

    // Add image if attached (multimodal doubt scan)
    if (imageBase64 && typeof imageBase64 === 'string') {
      const match = imageBase64.match(/^data:(image\/[a-zA-Z+]+);base64,(.+)$/);
      if (match) {
        parts.push({
          inlineData: {
            mimeType: match[1],
            data: match[2],
          },
        });
      } else {
        parts.push({
          inlineData: {
            mimeType: 'image/jpeg',
            data: imageBase64.replace(/^data:image\/\w+;base64,/, ''),
          },
        });
      }
    }

    // Add history context
    const historyText = Array.isArray(conversationHistory)
      ? conversationHistory.map((m: any) => `${m.role || m.sender}: ${m.text}`).slice(-6).join('\n\n')
      : '';

    parts.push({
      text: `Context History:\n${historyText}\n\nStudent Query: "${promptText}"\n\nProvide an insightful, comprehensive, and high-yield breakdown with next-action guidance:`,
    });

    const modelToUse = useThinkingMode ? 'gemini-3.7-flash' : 'gemini-3.7-flash';

    const response = await ai.models.generateContent({
      model: modelToUse,
      contents: [{ role: 'user', parts }],
      config: {
        temperature: 0.6,
        maxOutputTokens: 1500,
      },
    });

    const reply = response.text || 'I am ready to help you master this concept. What topic shall we tackle next?';

    // Infer action recommendation
    let actionType = 'NONE';
    let actionPayload: string | null = null;
    if (promptText.toLowerCase().includes('quiz') || promptText.toLowerCase().includes('test') || promptText.toLowerCase().includes('mcq')) {
      actionType = 'START_QUIZ';
      actionPayload = JSON.stringify({ subject: studyContext?.targetExam || 'General Studies', topic: 'Practice Quiz' });
    } else if (promptText.toLowerCase().includes('sprint') || promptText.toLowerCase().includes('focus') || promptText.toLowerCase().includes('pomodoro') || promptText.toLowerCase().includes('study')) {
      actionType = 'START_FOCUS';
      actionPayload = JSON.stringify({ minutes: 25, subject: 'General Studies', topic: 'Focus Session' });
    }

    return res.json({
      replyMarkdown: reply,
      text: reply,
      actionType,
      actionPayload,
      actionSuggestions: [
        '🎯 Generate 3 Practice MCQs on this',
        '💡 Explain with memory shortcut / mnemonic',
        '⏱️ Start a 25-minute Focus Sprint',
        '🗂️ Create a Flashcard for Spaced Revision',
      ],
      isOfflineFallback: false,
    });
  } catch (error: any) {
    console.error('Error in Nova chat:', error);
    res.json({
      replyMarkdown: `### 🌟 Nova Study Coach\n\nI have logged your request. Here is a high-yield takeaway for your exam syllabus:\n- Focus on core fundamental definitions and previous year questions (PYQs).\n- Eliminate extreme options in prelims statements.\n\n*Tap below to start an instant 25-min study sprint.*`,
      text: 'Keep up the consistent momentum! Let us tackle your next study goal.',
      actionType: 'START_FOCUS',
      actionPayload: JSON.stringify({ minutes: 25, subject: 'General Studies', topic: 'Revision' }),
      isOfflineFallback: true,
    });
  }
}

app.post('/api/nova/chat', handleNovaChat);
app.post('/api/nova-chat', handleNovaChat);

// 2. Real-time Streaming SSE endpoint for typewriter effect
app.post('/api/nova/stream', async (req, res) => {
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');

  try {
    const { userPrompt, studyContext, settings } = req.body;
    const ai = getGenAI();

    if (!ai) {
      res.write(`data: ${JSON.stringify({ chunk: "Namaste! Focusing on your core exam syllabus and consistent daily recall is your fastest path to a top percentile rank." })}\n\n`);
      res.write('data: [DONE]\n\n');
      return res.end();
    }

    const systemPrompt = getNovaSystemPrompt(studyContext, settings);
    const responseStream = await ai.models.generateContentStream({
      model: 'gemini-3.7-flash',
      contents: [
        {
          role: 'user',
          parts: [{ text: `${systemPrompt}\n\nStudent Query: "${userPrompt}"\n\nProvide an insightful step-by-step explanation:` }],
        },
      ],
      config: {
        temperature: 0.6,
        maxOutputTokens: 1200,
      },
    });

    for await (const chunk of responseStream) {
      if (chunk.text) {
        res.write(`data: ${JSON.stringify({ chunk: chunk.text })}\n\n`);
      }
    }
    res.write('data: [DONE]\n\n');
    res.end();
  } catch (error: any) {
    console.error('Error in /api/nova/stream:', error);
    res.write(`data: ${JSON.stringify({ chunk: "\n\n*Keep solving PYQs and revise weak topics consistently!*" })}\n\n`);
    res.write('data: [DONE]\n\n');
    res.end();
  }
});

// 3. Concept Explainer API
app.post('/api/tutor/explain', async (req, res) => {
  try {
    const { topic, subject, actionType, persona, targetExam, userQuery } = req.body;
    const ai = getGenAI();

    if (!ai) {
      return res.json({
        replyMarkdown: `### 📖 ${topic} (${subject})\n\n• **Core Principle**: Essential concept for ${targetExam || 'Competitive Exams'}.\n• **Key Rule**: Test-taking strategies require eliminating extreme statements.\n• **High-Yield Fact**: Frequently asked in prelims and mains evaluations.\n• **Memory Trick**: Associate with recent landmark events to retain long-term.`,
        isOfflineFallback: true,
      });
    }

    const prompt = `You are an expert exam tutor for ${targetExam || 'Competitive Exam'}.
Explain "${topic}" in subject "${subject}".
Instruction Style / Action: ${actionType || 'EXPLAIN_CONCEPT'}.
Tutor Persona: ${persona || 'Empathetic Socratic Tutor'}.
Specific Student Query: "${userQuery || 'Explain core fundamentals and exam shortcuts'}"

Provide a structured, beautifully formatted markdown response with:
1. 📌 **Executive Overview**
2. 🔑 **Core Conceptual Breakdown**
3. 🎯 **High-Yield Exam Takeaway & Traps to Avoid**
4. 🧠 **Memory Shortcut / Mnemonic (if applicable)**
5. ⚡ **1 Sample Exam MCQ with instant solution**`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.7-flash',
      contents: prompt,
      config: { temperature: 0.4, maxOutputTokens: 1200 },
    });

    res.json({
      replyMarkdown: response.text || `Concept explanation for ${topic} completed.`,
      isOfflineFallback: false,
    });
  } catch (err: any) {
    console.error('Error in /api/tutor/explain:', err);
    res.json({
      replyMarkdown: `### 📖 Concept Overview: ${req.body.topic}\n\nReview this topic along with past year questions (PYQs) to reinforce conceptual clarity.`,
      isOfflineFallback: true,
    });
  }
});

// 4. MCQ / Quiz Generator API
async function handleGenerateQuiz(req: express.Request, res: express.Response) {
  try {
    const { topic, subject, difficulty, count = 5, examName, examCategory, language } = req.body;
    const target = examName || examCategory || 'Competitive Exam (UPSC / SSC / Banking)';
    const ai = getGenAI();

    if (!ai) {
      return res.json({
        questions: [
          {
            id: `gen-q-${Date.now()}-1`,
            subject: subject || 'General Studies',
            topic: topic || 'Core Principles',
            difficulty: difficulty || 'Medium',
            examTag: target,
            questionText: `Regarding ${topic || 'the study topic'}, which of the following statements is conceptually accurate?`,
            options: [
              'It applies exclusively during national emergency declarations.',
              'It constitutes a fundamental, constitutionally safeguarded principle.',
              'It was wholly repealed by subsequent legislative statutory amendments.',
              'It is non-binding and purely advisory in constitutional character.',
            ],
            correctOptionIndex: 1,
            detailedExplanation: `Option B is correct. In ${topic || 'this subject'}, statutory and constitutional provisions ensure direct judicial and procedural enforcement.`,
            trickOrShortCut: 'Eliminate extreme words like "exclusively" and "wholly".',
          },
          {
            id: `gen-q-${Date.now()}-2`,
            subject: subject || 'General Studies',
            topic: topic || 'Core Principles',
            difficulty: difficulty || 'Medium',
            examTag: target,
            questionText: `Which among the following best demonstrates the primary objective of ${topic || 'this concept'} in competitive exam frameworks?`,
            options: [
              'Ensuring balanced systemic checks and institutional governance',
              'Eliminating judicial review completely',
              'Centralizing administrative powers without parliamentary scrutiny',
              'None of the above',
            ],
            correctOptionIndex: 0,
            detailedExplanation: 'Option A is correct. Institutional checks and balances represent the bedrock of modern public administrative governance.',
            trickOrShortCut: 'Identify core governance themes centered on accountability.',
          },
        ],
        isOfflineFallback: true,
      });
    }

    const prompt = `Generate ${count} authentic, challenging multiple-choice questions (MCQs) for "${target}" on the topic: "${topic || 'General Studies'}" in subject: "${subject || 'General'}".
Difficulty level: ${difficulty || 'Medium'}. Language preference: ${language || 'English'}.

Return ONLY a valid JSON array of objects conforming exactly to this schema:
[
  {
    "id": "gen_q_1",
    "subject": "${subject || 'General'}",
    "topic": "${topic || 'Core'}",
    "difficulty": "${difficulty || 'Medium'}",
    "examTag": "${target}",
    "questionText": "Question text here (clear, rigorous)",
    "options": ["Option A", "Option B", "Option C", "Option D"],
    "correctOptionIndex": 0,
    "detailedExplanation": "Thorough explanation explaining why the correct option is right and others are incorrect.",
    "trickOrShortCut": "Elimination technique, mnemonic, or shortcut rule."
  }
]
No markdown wrapping, return raw JSON array only.`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.7-flash',
      contents: prompt,
      config: {
        responseMimeType: 'application/json',
        temperature: 0.35,
      },
    });

    const parsed = JSON.parse(response.text || '[]');
    res.json({ questions: parsed, isOfflineFallback: false });
  } catch (error: any) {
    console.error('Error in quiz generator:', error);
    res.json({
      questions: [
        {
          id: `fallback-${Date.now()}`,
          subject: req.body.subject || 'Polity',
          topic: req.body.topic || 'Revision Drill',
          difficulty: 'Medium',
          examTag: 'Target Exam',
          questionText: `Which of the following study strategies consistently delivers the highest percentile retention in competitive exams?`,
          options: [
            'Passive re-reading of textbooks without practice',
            'Spaced repetition combined with active recall and PYQ analysis',
            'Studying exclusively during the last 24 hours',
            'Skipping post-test mistake analysis',
          ],
          correctOptionIndex: 1,
          detailedExplanation: 'Cognitive science confirms that active recall and spaced repetition strengthen neural retrieval pathways by over 300%.',
          trickOrShortCut: 'Active Recall + Spaced Repetition = 99th Percentile.',
        },
      ],
      isOfflineFallback: true,
    });
  }
}

app.post('/api/quiz/generate', handleGenerateQuiz);
app.post('/api/generate-quiz', handleGenerateQuiz);

// 5. Document / Note Summarizer API
async function handleSummarize(req: express.Request, res: express.Response) {
  try {
    const { documentText, content, targetExam, subject, title } = req.body;
    const textToSummarize = documentText || content || '';
    const exam = targetExam || 'Competitive Exams';
    const sub = subject || title || 'General Studies';
    const ai = getGenAI();

    if (!ai) {
      return res.json({
        summaryMarkdown: `### 📑 High-Yield Summary: ${title || sub}\n\n• **Core Concept**: Fundamental knowledge distilled for ${exam}.\n• **Key Takeaway**: Essential definitions, classifications, and historical/statutory context.\n• **Action Plan**: Review the flashcards below and attempt 3 quick practice questions.`,
        summary: `Essential summary for ${title || sub} targeting ${exam}.`,
        keyPoints: [
          'Core definition and operational framework',
          'Key exceptions frequently tested in prelims',
          'Interlinkage with current affairs and constitutional articles',
        ],
        keyTakeaways: ['High-frequency theme', 'Standard statutory procedure'],
        mindMapOutline: [
          { heading: '1. Foundation', subpoints: ['Core Definitions', 'Historical Context'] },
          { heading: '2. Application', subpoints: ['Key Articles/Rules', 'Judicial Pronouncements'] },
        ],
        flashcards: [
          {
            front: `What is the core takeaway of ${title || sub}?`,
            back: 'It forms an essential conceptual pillar frequently tested in preliminary and mains exams.',
            hint: 'Recall primary definitions.',
          },
        ],
        generatedFlashcards: [
          {
            front: `What is the core takeaway of ${title || sub}?`,
            back: 'It forms an essential conceptual pillar frequently tested in preliminary and mains exams.',
            difficulty: 'Medium',
          },
        ],
        quizQuestions: [],
        isOfflineFallback: true,
      });
    }

    const prompt = `You are a premier academic summarizer for ${exam} aspirants.
Analyze the following study material on "${sub}":
"""
${textToSummarize}
"""

Return strictly valid JSON with this exact schema:
{
  "summaryMarkdown": "Comprehensive markdown summary with bold key terms, bullet points, and high-yield callouts",
  "keyPoints": ["Key point 1", "Key point 2", "Key point 3", "Key point 4"],
  "mindMapOutline": [
    {"heading": "Branch 1", "subpoints": ["subpoint A", "subpoint B"]},
    {"heading": "Branch 2", "subpoints": ["subpoint C", "subpoint D"]}
  ],
  "flashcards": [
    {"front": "Question/Prompt", "back": "Crisp answer/explanation", "hint": "Helpful mnemonic or hint"}
  ],
  "quizQuestions": [
    {
      "id": "sum_q_1",
      "subject": "${sub}",
      "topic": "${sub}",
      "difficulty": "Medium",
      "examTag": "${exam}",
      "questionText": "Question text",
      "options": ["A", "B", "C", "D"],
      "correctOptionIndex": 0,
      "detailedExplanation": "Reason",
      "trickOrShortCut": "Mnemonic"
    }
  ]
}`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.7-flash',
      contents: prompt,
      config: {
        responseMimeType: 'application/json',
        temperature: 0.3,
      },
    });

    const parsed = JSON.parse(response.text || '{}');
    res.json({
      ...parsed,
      summary: parsed.summaryMarkdown,
      keyTakeaways: parsed.keyPoints,
      generatedFlashcards: parsed.flashcards,
      isOfflineFallback: false,
    });
  } catch (err: any) {
    console.error('Error in summarizer:', err);
    res.status(500).json({ error: 'Failed to summarize document' });
  }
}

app.post('/api/document/summarize', handleSummarize);
app.post('/api/summarize-content', handleSummarize);

// 6. Test Diagnostic API
app.post('/api/tutor/diagnose', async (req, res) => {
  try {
    const {
      examName,
      subject,
      score,
      totalQuestions,
      accuracyPercent,
      timeSpentSeconds,
      weakTopics,
      strongTopics,
      incorrectQuestions,
    } = req.body;

    const ai = getGenAI();
    if (!ai) {
      return res.json({
        diagnosticMarkdown: `### 📊 Diagnostic Analysis: ${examName || 'Mock Test'}\n\n**Score**: ${score}/${totalQuestions} (${accuracyPercent}% Accuracy)\n\n• **Strengths**: You displayed confident mastery in ${strongTopics?.join(', ') || 'Static Fundamentals'}.\n• **Weak Areas to Remediate**: Pay special attention to ${weakTopics?.join(', ') || 'Exception Cases & Data Interpretation'}.\n• **Prescribed Action**: Execute a 25-min targeted revision sprint on your lowest-accuracy topics today!`,
        isOfflineFallback: true,
      });
    }

    const prompt = `You are the Lead Academic Diagnostic Director for ${examName || 'Competitive Exams'}.
Analyze the student's mock test results:
- Subject: ${subject}
- Score: ${score} out of ${totalQuestions}
- Accuracy: ${accuracyPercent}%
- Time Spent: ${timeSpentSeconds} seconds
- Identified Weak Areas: ${Array.isArray(weakTopics) ? weakTopics.join(', ') : 'None specified'}
- Identified Strong Areas: ${Array.isArray(strongTopics) ? strongTopics.join(', ') : 'None specified'}
- Sample Errors: ${JSON.stringify((incorrectQuestions || []).slice(0, 3))}

Provide a motivational, highly rigorous diagnostic report in formatted Markdown:
1. 🎯 **Performance Tier & Estimated Percentile Standing**
2. ⚠️ **Root Cause of Errors (Knowledge Gap vs Trap/Silly Mistake vs Time Pressure)**
3. 🚀 **Immediate 3-Step Action Plan (Next 48 Hours)**
4. 💡 **Pro-Tip for Final Exam Day Execution**`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.7-flash',
      contents: prompt,
      config: { temperature: 0.5, maxOutputTokens: 1200 },
    });

    res.json({
      diagnosticMarkdown: response.text || 'Diagnostic report compiled successfully.',
      isOfflineFallback: false,
    });
  } catch (err: any) {
    console.error('Error in test diagnostic:', err);
    res.json({
      diagnosticMarkdown: `### 📊 Test Evaluation Summary\n\nGreat effort! Focus on revising missed concepts and solving 10 similar questions to lock in accuracy.`,
      isOfflineFallback: true,
    });
  }
});

// 7. Text to Speech (ElevenLabs proxy with graceful fallback)
app.post('/api/tts', async (req, res) => {
  try {
    const { text, voiceId } = req.body;
    const apiKey = process.env.ELEVENLABS_API_KEY;

    if (!apiKey) {
      return res.json({ supported: false, message: 'Use browser speech synthesis' });
    }

    const selectedVoice = voiceId || process.env.ELEVENLABS_VOICE_ID || '21m00Tcm4TlvDq8ikWAM';
    const modelId = process.env.ELEVENLABS_MODEL_ID || 'eleven_multilingual_v2';

    const response = await fetch(`https://api.elevenlabs.io/v1/text-to-speech/${selectedVoice}`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'xi-api-key': apiKey,
      },
      body: JSON.stringify({
        text: text.slice(0, 500),
        model_id: modelId,
        voice_settings: {
          stability: 0.5,
          similarity_boost: 0.75,
        },
      }),
    });

    if (!response.ok) {
      return res.json({ supported: false, message: 'ElevenLabs rate limit or key error' });
    }

    const arrayBuffer = await response.arrayBuffer();
    const base64Audio = Buffer.from(arrayBuffer).toString('base64');
    res.json({
      supported: true,
      audioData: `data:audio/mp3;base64,${base64Audio}`,
    });
  } catch (err: any) {
    console.error('Error in TTS:', err);
    res.json({ supported: false, message: 'Fallback to browser speech' });
  }
});

// 8. Smart Study Plan Generator
async function handleGeneratePlan(req: express.Request, res: express.Response) {
  try {
    const { targetExam, examDate, availableHoursPerDay, weakTopics } = req.body;
    const ai = getGenAI();

    if (!ai) {
      return res.json({
        dailySchedule: [
          { time: '08:00 AM - 09:30 AM', subject: 'Core Static Paper (GS)', activity: 'Deep Conceptual Reading & Note Synthesis', energy: 'High' },
          { time: '10:00 AM - 11:30 AM', subject: 'Weak Topic Remediation', activity: `Focused practice on ${(weakTopics || ['Key Weak Area'])[0]}`, energy: 'High' },
          { time: '02:00 PM - 03:30 PM', subject: 'Practice & PYQ Blitz', activity: '30 Timed MCQs + Instant Mistake Log Entry', energy: 'Medium' },
          { time: '05:00 PM - 06:00 PM', subject: 'Current Affairs & Editorial', activity: 'Daily Digest & Editorials breakdown with Nova', energy: 'Medium' },
          { time: '09:00 PM - 09:45 PM', subject: 'Spaced Flashcard Review', activity: 'Active recall session & day retrospective', energy: 'Low' },
        ],
        aiAdvice: `For your ${targetExam} on ${examDate}, your biggest score multiplier will come from converting your weak topics into solid strengths through daily 30-minute targeted drills.`,
      });
    }

    const prompt = `Create an optimized, science-backed daily study schedule for a candidate preparing for "${targetExam}" (Exam date: ${examDate}).
Daily available study time: ${availableHoursPerDay || 5} hours.
Weak areas identified: ${(weakTopics || []).join(', ')}.

Return strictly valid JSON:
{
  "dailySchedule": [
    {
      "time": "08:00 AM - 09:30 AM",
      "subject": "Subject Name",
      "activity": "Specific actionable study activity",
      "energy": "High | Medium | Low"
    }
  ],
  "aiAdvice": "Motivational and strategic high-impact advice in 2-3 sentences"
}`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.7-flash',
      contents: prompt,
      config: {
        responseMimeType: 'application/json',
        temperature: 0.5,
      },
    });

    res.json(JSON.parse(response.text || '{}'));
  } catch (error: any) {
    console.error('Error generating study plan:', error);
    res.status(500).json({ error: 'Failed to generate study plan' });
  }
}

app.post('/api/generate-plan', handleGeneratePlan);
app.post('/api/study-plan/generate', handleGeneratePlan);

// -------------------------------------------------------------
// SARKARI RADAR LIVE FEED, SUPABASE & GEMINI INTEGRATION
// -------------------------------------------------------------
const SARKARI_FALLBACK_JOBS = [
  {
    id: 'sarkari_ssc_cgl_2026',
    title: 'SSC CGL 2026 (Combined Graduate Level)',
    organization_name: 'Staff Selection Commission (SSC)',
    department: 'DoPT, Govt. of India',
    category: 'SSC',
    total_vacancies: '14,800+ Posts',
    last_date: '2026-07-28',
    qualification: 'Bachelor\'s Degree in any stream (Graduate)',
    apply_url: 'https://ssc.gov.in',
    notification_pdf_url: 'https://ssc.gov.in/notices',
    salary: 'Level 4 to Level 8 (₹25,500 - ₹1,51,100)',
    age_limit: '18 - 30/32 Years',
    exam_date: 'Sept 2026',
    description: 'Premier recruitment for Inspector, Assistant Section Officer (ASO), Sub-Inspector & Tax Assistants.',
    created_at: new Date().toISOString(),
  },
  {
    id: 'sarkari_rrb_ntpc_2026',
    title: 'RRB NTPC CEN 06/2026 Non-Technical Popular Categories',
    organization_name: 'Railway Recruitment Boards (RRB)',
    department: 'Ministry of Railways',
    category: 'Railway',
    total_vacancies: '11,558 Posts',
    last_date: '2026-08-15',
    qualification: '12th Pass / Graduate depending on level',
    apply_url: 'https://www.rrbapply.gov.in',
    notification_pdf_url: 'https://indianrailways.gov.in',
    salary: 'Level 2 to Level 6 (₹19,900 - ₹35,400 Basic)',
    age_limit: '18 - 33 Years (Relaxation for OBC/SC/ST)',
    exam_date: 'Oct - Nov 2026',
    description: 'Station Master, Goods Train Manager, Junior Clerk cum Typist across all RRB divisions.',
    created_at: new Date().toISOString(),
  },
  {
    id: 'sarkari_up_police_si_2026',
    title: 'UP Police Sub Inspector (SI) & Platoon Commander',
    organization_name: 'UPPRPB (Uttar Pradesh Police)',
    department: 'Police & Defence Services',
    category: 'Police',
    total_vacancies: '3,800 Posts',
    last_date: '2026-08-05',
    qualification: 'Graduate in any discipline',
    apply_url: 'https://uppbpb.gov.in',
    notification_pdf_url: 'https://uppbpb.gov.in',
    salary: 'Pay Band 9300-34800, Grade Pay 4200',
    age_limit: '21 - 28 Years',
    exam_date: 'Nov 2026',
    description: 'Direct recruitment for Sub-Inspector (Civil Police) and PAC Platoon Commander.',
    created_at: new Date().toISOString(),
  },
  {
    id: 'sarkari_ibps_po_2026',
    title: 'IBPS PO / MT XIV Recruitment 2026',
    organization_name: 'Institute of Banking Personnel Selection',
    department: 'Participating Public Sector Banks',
    category: 'Banking',
    total_vacancies: '4,455 Posts',
    last_date: '2026-08-21',
    qualification: 'Graduation Degree from recognized University',
    apply_url: 'https://www.ibps.in',
    notification_pdf_url: 'https://www.ibps.in',
    salary: 'Basic ₹36,000 + DA + HRA (In-hand ~₹54,000+)',
    age_limit: '20 - 30 Years',
    exam_date: 'October 2026',
    description: 'Probationary Officers / Management Trainees in PNB, Canara Bank, Bank of Baroda, etc.',
    created_at: new Date().toISOString(),
  },
  {
    id: 'sarkari_ssc_gd_2026',
    title: 'SSC GD Constable (CAPFs, SSF, Assam Rifles)',
    organization_name: 'Staff Selection Commission (SSC)',
    department: 'Ministry of Home Affairs',
    category: 'Police',
    total_vacancies: '39,481 Posts',
    last_date: '2026-09-02',
    qualification: '10th Class (Matriculation) Pass',
    apply_url: 'https://ssc.gov.in',
    notification_pdf_url: 'https://ssc.gov.in',
    salary: 'Pay Level 3 (₹21,700 - ₹69,100)',
    age_limit: '18 - 23 Years',
    exam_date: 'Dec 2026 - Jan 2027',
    description: 'Constable GD in BSF, CISF, CRPF, ITBP, SSB, SSF and Rifleman in Assam Rifles.',
    created_at: new Date().toISOString(),
  }
];

const SARKARI_FALLBACK_ADMIT_CARDS = [
  {
    id: 'ac_ssc_chsl_tier1',
    title: 'SSC CHSL 10+2 Tier 1 Admit Card & City Slip 2026',
    exam_name: 'SSC CHSL Tier 1',
    organization_name: 'Staff Selection Commission',
    release_date: 'Live Now',
    exam_date: 'July 15 - July 26, 2026',
    download_url: 'https://ssc.gov.in',
    city_slip_url: 'https://ssc.gov.in',
    status: 'ACTIVE',
    created_at: new Date().toISOString(),
  },
  {
    id: 'ac_rrb_alp_cbt1',
    title: 'RRB Assistant Loco Pilot (ALP) CBT-1 Exam City Intimation',
    exam_name: 'RRB ALP CBT 1',
    organization_name: 'Railway Recruitment Boards',
    release_date: 'Live Now',
    exam_date: 'August 05 - August 10, 2026',
    download_url: 'https://www.rrbapply.gov.in',
    city_slip_url: 'https://www.rrbapply.gov.in',
    status: 'ACTIVE',
    created_at: new Date().toISOString(),
  }
];

const SARKARI_FALLBACK_RESULTS = [
  {
    id: 'res_upsc_prelims_2026',
    title: 'UPSC Civil Services Prelims 2026 Official Result & Roll Number List',
    exam_name: 'UPSC CSE Prelims',
    organization_name: 'UPSC',
    declared_date: 'Declared Today',
    result_url: 'https://upsc.gov.in',
    cutoff_details: 'Cutoff Marks & Answer Key will be released after Final Marks declaration.',
    status: 'DECLARED',
    created_at: new Date().toISOString(),
  }
];

// 1. Live Feed Proxy from Render / Supabase
app.get('/api/sarkari/live-feed', async (req, res) => {
  try {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 8000);
    const response = await fetch('https://studymate-sarkari.onrender.com/api/live-feed', {
      headers: { 'Accept': 'application/json' },
      signal: controller.signal,
    }).catch(() => null);
    clearTimeout(timeout);

    if (response && response.ok) {
      const data = await response.json();
      return res.json(data);
    }

    res.json({
      status: 'success',
      source: 'live_feed_cache',
      jobs: SARKARI_FALLBACK_JOBS,
      admitCards: SARKARI_FALLBACK_ADMIT_CARDS,
      results: SARKARI_FALLBACK_RESULTS,
    });
  } catch (err: any) {
    res.json({
      status: 'fallback',
      jobs: SARKARI_FALLBACK_JOBS,
      admitCards: SARKARI_FALLBACK_ADMIT_CARDS,
      results: SARKARI_FALLBACK_RESULTS,
    });
  }
});

// 2. AI Target Exam Study Planner (Gemini)
app.post('/api/sarkari/ai-study-plan', async (req, res) => {
  try {
    const {
      jobTitle,
      organization,
      category,
      totalVacancies,
      lastDate,
      qualification,
      examDate,
      userHoursPerDay,
    } = req.body;

    const hours = userHoursPerDay || 5;
    const ai = getGenAI();

    if (!ai) {
      const fallbackRoutines = Array.from({ length: 30 }, (_, i) => {
        const day = i + 1;
        const subjects = [
          'General Awareness & Static GK',
          'Quantitative Aptitude & Numerical Ability',
          'Reasoning & Analytical Logic',
          'English Language / Hindi Comprehension',
        ];
        const focusSubject = subjects[i % subjects.length];
        return {
          dayNumber: day,
          dayLabel: `Day ${day}`,
          focusSubject,
          highYieldTopics: [
            `${jobTitle} Core Topic: Unit ${Math.floor(i / 2) + 1}`,
            'Previous Years Question Drills (Last 5 Years)',
            'Speed Elimination & Accuracy Refinement',
          ],
          recommendedHours: hours,
          pomodoroSprints: Math.round((hours * 60) / 30),
          revisionAction: `Solve 25 sectional MCQs in StudyMate Practice Hub and log errors into memory.`,
        };
      });

      return res.json({
        examTitle: jobTitle || 'Target Exam',
        organization: organization || 'Government Body',
        daysRemaining: 30,
        summaryQuote: `Dedicate ${hours} hours daily with laser focus to crack ${jobTitle}. Consistent daily drills triumph over cramming!`,
        weeklyMilestones: [
          { week: 1, focus: 'High-Yield Core Foundations & Formula Consolidation', targetMockScore: '60% Accuracy' },
          { week: 2, focus: 'Sectional Speed Drills & Weakness Elimination', targetMockScore: '72% Accuracy' },
          { week: 3, focus: 'Full-Length Computer-Based Mocks & Real Exam Hall Simulation', targetMockScore: '80% Accuracy' },
          { week: 4, focus: 'Current Affairs Marathon, Negative Marking Shield & Final Rapid Revision', targetMockScore: '85%+ Benchmark' },
        ],
        dailyRoutines: fallbackRoutines,
        generalStrategy: `Prioritize high-weightage topics identified in previous ${organization} recruitment cycles. Use StudyMate Focus Shield for 25-minute distraction-free sprints.`,
      });
    }

    const prompt = `You are the Master Competitive Exam Strategist at StudyMate AI.
An aspirant just selected this verified government job as their target exam:
- Job Title: ${jobTitle}
- Organization / Department: ${organization} (Category: ${category})
- Vacancies: ${totalVacancies}
- Minimum Qualification: ${qualification}
- Application Deadline: ${lastDate}
- Tentative Exam Date: ${examDate || 'Within 60-90 days'}
- Aspirant Daily Study Time: ${hours} hours/day

Generate an exhaustive, realistic, and inspiring 30-Day Daily Study Routine & Focus Schedule tailored strictly to this specific exam's syllabus.

Output strictly valid JSON with this exact schema (no additional markdown or commentary outside the JSON):
{
  "examTitle": "${jobTitle}",
  "organization": "${organization}",
  "daysRemaining": 30,
  "summaryQuote": "A punchy, motivating 1-sentence quote customized to cracking ${jobTitle}",
  "weeklyMilestones": [
    { "week": 1, "focus": "string", "targetMockScore": "string" },
    { "week": 2, "focus": "string", "targetMockScore": "string" },
    { "week": 3, "focus": "string", "targetMockScore": "string" },
    { "week": 4, "focus": "string", "targetMockScore": "string" }
  ],
  "dailyRoutines": [
    {
      "dayNumber": 1,
      "dayLabel": "Day 1",
      "focusSubject": "string",
      "highYieldTopics": ["string", "string"],
      "recommendedHours": ${hours},
      "pomodoroSprints": ${Math.round((hours * 60) / 30)},
      "revisionAction": "string"
    }
  ],
  "generalStrategy": "Concise 2-3 sentence strategic advice on negative marking, speed techniques, and cutoff targets for this specific recruitment"
}

Ensure "dailyRoutines" contains 30 day entries (Day 1 through Day 30) systematically covering the full syllabus, revision cycles, and mock tests.`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.7-flash',
      contents: prompt,
      config: {
        responseMimeType: 'application/json',
        temperature: 0.35,
      },
    });

    const parsed = JSON.parse(response.text || '{}');
    res.json(parsed);
  } catch (err: any) {
    console.error('Error generating AI study plan:', err);
    res.status(500).json({ error: 'Failed to generate target exam study plan' });
  }
});

// 3. Hinglish AI Eligibility Assistant (Gemini)
app.post('/api/sarkari/ai-eligibility-chat', async (req, res) => {
  try {
    const { question, jobsContext } = req.body;
    const ai = getGenAI();

    if (!ai) {
      return res.json({
        replyMarkdown: `Namaste! StudyMate Sarkari Radar me currently active government vacancies hain:\n\n` +
          `1. **SSC CGL 2026**: Bachelor's Degree (Any Stream), Age 18-30/32 Yrs, 14,800+ Posts.\n` +
          `2. **RRB NTPC CEN 06/2026**: 12th Pass & Graduates eligible, Age 18-33 Yrs, 11,558 Posts.\n` +
          `3. **UP Police SI 2026**: Graduate, Age 21-28 Yrs, 3,800 Posts.\n` +
          `4. **SSC GD Constable**: 10th Pass, Age 18-23 Yrs, 39,481 Posts.\n\n` +
          `Aap apni exact qualification, category aur age bataiye taaki main direct apply link ke saath match karke bata saku!`,
      });
    }

    const prompt = `You are the "Hinglish AI Eligibility & Sarkari Job Advisor" in StudyMate AI.
You help Indian competitive exam aspirants understand their eligibility (qualification, age limit, category relaxation, physical standards, syllabus, and direct apply steps) in warm, encouraging, and natural Hinglish (Hindi written in Roman English script mixed with clear English terms).

The student asked:
"${question}"

Here are verified live jobs currently active from our shared database:
${JSON.stringify(jobsContext || [], null, 2)}

Directives:
1. Answer directly and precisely in friendly, encouraging Hinglish (e.g. "Bhai aapke liye ye vacancies best rahengi...", "Haan bilkul, aap 100% eligible hain...", "Aapki age aur qualification ke hisaab se...").
2. Reference the exact matching live vacancies from the database above (quote post title, total posts, last date, and qualification).
3. If they qualify for multiple jobs, highlight the top 2-3 with the most vacancies and upcoming application deadlines.
4. Explain category relaxations if relevant (OBC +3 years, SC/ST +5 years, Ex-Servicemen).
5. Explain required physical standards (PST/PET) or typing tests if the job is Police, Defence, or Clerk.
6. Provide a direct action tip: "Aap StudyMate ke Sarkari Radar tab se 'Direct Apply ↗' link par tap karke form bhar sakte hain, ya 'Set as Target Exam' karke 30-day AI routine generate kar lijiye!"
7. Format cleanly in GitHub-flavored Markdown with bold headers and bullet points.`;

    const response = await ai.models.generateContent({
      model: 'gemini-3.7-flash',
      contents: prompt,
      config: {
        temperature: 0.5,
      },
    });

    res.json({ replyMarkdown: response.text || '' });
  } catch (err: any) {
    console.error('Error in eligibility assistant:', err);
    res.status(500).json({ error: 'Failed to process eligibility inquiry' });
  }
});

// Serve frontend in production or start Vite in dev
async function startServer() {
  if (process.env.NODE_ENV === 'production') {
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  } else {
    // In dev mode, mount Vite as middleware
    const { createServer: createViteServer } = await import('vite');
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`StudyMate AI Server running on http://0.0.0.0:${PORT}`);
  });
}

startServer();

