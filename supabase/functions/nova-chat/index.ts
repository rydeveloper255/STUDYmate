// Supabase Edge Function: nova-chat
// Secure server-side AI engine for StudyMate NOVA
// Uses server-side GEMINI_API_KEY and SERPER_API_KEY (never exposed in client APK)

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

interface ChatRequest {
  userPrompt: string;
  conversationHistory?: Array<{ role: string; text: string }>;
  studyContext?: {
    studentName?: string;
    targetExam?: string;
    examDaysRemaining?: number;
    subjects?: string[];
    weakTopics?: string[];
    strongTopics?: string[];
    dailyTargetMinutes?: number;
    todayFocusMinutes?: number;
    currentStreak?: number;
    pendingPlanCount?: number;
    pendingTasksSummary?: string[];
    revisionsDueCount?: number;
    revisionsDueTopics?: string[];
    recentMockAccuracyPercent?: number;
    nextScheduledSession?: string;
    topDistractingAppName?: string;
    topDistractingAppUsageMins?: number;
    preferredLanguage?: string;
    preferredStudyDurationMins?: number;
    memories?: Array<{ category: string; key: string; value: string }>;
  };
  settings?: {
    useBossGreeting?: boolean;
    memoryEnabled?: boolean;
    voiceEnabled?: boolean;
  };
  requireWebSearch?: boolean;
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { userPrompt, conversationHistory = [], studyContext = {}, settings = {}, requireWebSearch = false }: ChatRequest = await req.json();

    const geminiApiKey = Deno.env.get("GEMINI_API_KEY") || "";
    const serperApiKey = Deno.env.get("SERPER_API_KEY") || "";

    if (!userPrompt || userPrompt.trim() === "") {
      return new Response(
        JSON.stringify({ error: "Missing userPrompt" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // 1. Web Search via Serper if current/external info is genuinely needed
    let webSearchContext = "";
    const shouldSearchWeb = requireWebSearch || /current affairs|news|latest cutoff|exam date announcement|who won|today's weather/i.test(userPrompt);

    if (shouldSearchWeb && serperApiKey) {
      try {
        const serperRes = await fetch("https://google.serper.dev/search", {
          method: "POST",
          headers: {
            "X-API-KEY": serperApiKey,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ q: userPrompt, gl: "in", hl: "en", num: 4 }),
        });

        if (serperRes.ok) {
          const searchJson = await serperRes.json();
          const snippets = (searchJson.organic || []).map(
            (item: any) => `[Source: ${item.title}] ${item.snippet} (${item.link})`
          ).join("\n");
          if (snippets) {
            webSearchContext = `\nREAL-TIME WEB SEARCH RESULTS:\n${snippets}\n`;
          }
        }
      } catch (e) {
        console.warn("Serper search error:", e);
      }
    }

    // 2. Build Compact Relevant Student Context
    const systemPrompt = `You are NOVA, the intelligent personal study companion living inside StudyMate for ${studyContext.studentName || "Scholar"}.
ROLE: Personal AI Study Mentor + Academic Coach + Productivity Companion.

CORE BEHAVIOR GUIDELINES:
1. Always base advice on the student's REAL study state from their database:
   - Target Exam: ${studyContext.targetExam || "Competitive Exam"} (${studyContext.examDaysRemaining ?? 30} days left)
   - Enrolled Subjects: ${(studyContext.subjects || []).join(", ") || "All Core Subjects"}
   - Weak Topics: ${(studyContext.weakTopics || []).join(", ") || "None flagged"}
   - Strong Topics: ${(studyContext.strongTopics || []).join(", ") || "None flagged"}
   - Daily Target: ${studyContext.dailyTargetMinutes ?? 180} mins (Completed today: ${studyContext.todayFocusMinutes ?? 0} mins)
   - Current Streak: ${studyContext.currentStreak ?? 1} days
   - Pending Study Plan Tasks: ${studyContext.pendingPlanCount ?? 0} tasks (${(studyContext.pendingTasksSummary || []).slice(0, 3).join("; ")})
   - Revisions Due (Spaced Repetition): ${studyContext.revisionsDueCount ?? 0} cards (${(studyContext.revisionsDueTopics || []).slice(0, 3).join(", ")})
   - Recent Quiz/Mock Accuracy: ${studyContext.recentMockAccuracyPercent ? `${studyContext.recentMockAccuracyPercent}%` : "Not enough data"}
   - Preferred Study Duration: ${studyContext.preferredStudyDurationMins ?? 25} minutes
   - Preferred Language: ${studyContext.preferredLanguage || "English/Hinglish"}
   ${studyContext.topDistractingAppName && (studyContext.topDistractingAppUsageMins ?? 0) > 0 ? `- Distraction Note: ${studyContext.topDistractingAppName} used for ${studyContext.topDistractingAppUsageMins} mins today.` : ""}

2. WHEN USER ASKS "What should I study today?":
   - Formulate a clean, realistic study plan prioritizing:
     a) Revisions due (quick active recall sprint)
     b) Identified weak topics with highest exam weightage
     c) Pending tasks from study planner
   - Keep sessions bounded by the student's preferred session length (${studyContext.preferredStudyDurationMins ?? 25} mins).
   - End with a tool action tag: [ACTION:START_FOCUS:{"subject":"<Subject>","topic":"<Topic>","minutes":${studyContext.preferredStudyDurationMins ?? 25}}]

3. WHEN USER ASKS "How am I doing?":
   - Provide an objective, encouraging summary using their actual streak, focus time today vs target, accuracy rate, and weak topic count.
   - Highlight positive consistency and pinpoint 1 clear actionable improvement.

4. WHEN USER IS FALLING BEHIND OR MISSED A SESSION:
   - Suggest a gentle, realistic recovery sprint without guilt, shame, pressure, or manipulation.

5. WHEN DISTRACTING APPS DETECTED:
   - If user asks about distraction, give a friendly nudge: "Boss, kaafi time ho gaya. Chalo 20 minute ka focused session complete kar lete hain."

6. PERSONALIZATION & MEMORY:
   ${settings.memoryEnabled && studyContext.memories && studyContext.memories.length > 0 ? "Saved Student Preferences:\n" + studyContext.memories.map(m => `- [${m.category}] ${m.key}: ${m.value}`).join("\n") : "No personal memories recorded yet."}

${webSearchContext}

TOOL ACTION PROTOCOL:
If recommending an immediate action, append one tag at the very end:
- [ACTION:START_FOCUS:{"subject":"Physics","topic":"Current Electricity","minutes":25}]
- [ACTION:START_QUIZ:{"subject":"Physics","topic":"Electrostatics"}]
- [ACTION:CREATE_PLAN:{"days":7}]
- [ACTION:CREATE_REMINDER:{"title":"Physics Revision","time":"7:00 PM"}]
- [ACTION:OPEN_APP_BLOCKING:{}]
- [ACTION:OPEN_MEMORY:{}]
If user asks to remember a personal preference, append:
- [MEMORY:{"category":"PREFERENCE","key":"Preferred Session","value":"45 mins deep work"}]
`;

    // 3. Call Gemini API
    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${geminiApiKey}`;

    const formattedContents = [
      ...conversationHistory.slice(-6).map((h) => ({
        role: h.role === "user" ? "user" : "model",
        parts: [{ text: h.text }],
      })),
      {
        role: "user",
        parts: [{ text: userPrompt }],
      },
    ];

    const geminiBody = {
      contents: formattedContents,
      generationConfig: {
        temperature: 0.4,
        topP: 0.9,
      },
      systemInstruction: {
        parts: [{ text: systemPrompt }],
      },
    };

    const geminiRes = await fetch(geminiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(geminiBody),
    });

    if (!geminiRes.ok) {
      const errText = await geminiRes.text();
      throw new Error(`Gemini API error: ${geminiRes.status} ${errText}`);
    }

    const geminiData = await geminiRes.json();
    const fullText = geminiData.candidates?.[0]?.content?.parts?.[0]?.text || "Boss, main tumhari madad ke liye ready hoon. Aaj kya study karein?";

    // Parse actions and memory tags
    let cleanText = fullText;
    let actionType = "NONE";
    let actionPayload: any = null;
    let memoryToSave: any = null;

    const actionRegex = /\[ACTION:([A-Z_]+):(\{.*?\})\]/;
    const actionMatch = actionRegex.exec(fullText);
    if (actionMatch) {
      actionType = actionMatch[1];
      try {
        actionPayload = JSON.parse(actionMatch[2]);
      } catch {
        actionPayload = actionMatch[2];
      }
      cleanText = cleanText.replace(actionMatch[0], "").trim();
    }

    const memoryRegex = /\[MEMORY:(\{.*?\})\]/;
    const memoryMatch = memoryRegex.exec(fullText);
    if (memoryMatch) {
      try {
        memoryToSave = JSON.parse(memoryMatch[1]);
      } catch (e) {
        console.warn("Failed to parse memory JSON", e);
      }
      cleanText = cleanText.replace(memoryMatch[0], "").trim();
    }

    return new Response(
      JSON.stringify({
        replyMarkdown: cleanText,
        actionType,
        actionPayload: actionPayload ? JSON.stringify(actionPayload) : null,
        memoryToSave,
      }),
      {
        headers: { ...corsHeaders, "Content-Type": "application/json" },
      }
    );
  } catch (err: any) {
    console.error("Error in nova-chat function:", err);
    return new Response(
      JSON.stringify({ error: err.message || "Internal server error" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
