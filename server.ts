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

