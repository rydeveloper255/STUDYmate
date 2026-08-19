// Supabase Edge Function: smart-search
// Server-side Academic & Concept Synthesis Engine with Serper & Gemini

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

interface SearchRequest {
  query: string;
  examName?: string;
  subject?: string;
  requireWebSearch?: boolean;
}

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const { query, examName = "Competitive Exam", subject = "General", requireWebSearch = false }: SearchRequest = await req.json();

    const geminiApiKey = Deno.env.get("GEMINI_API_KEY") || "";
    const serperApiKey = Deno.env.get("SERPER_API_KEY") || "";

    if (!query || query.trim() === "") {
      return new Response(
        JSON.stringify({ error: "Missing search query" }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    let webSearchSnippets: any[] = [];
    if (requireWebSearch && serperApiKey) {
      try {
        const serperRes = await fetch("https://google.serper.dev/search", {
          method: "POST",
          headers: {
            "X-API-KEY": serperApiKey,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ q: `${query} ${examName} ${subject}`, gl: "in", hl: "en", num: 5 }),
        });
        if (serperRes.ok) {
          const searchJson = await serperRes.json();
          webSearchSnippets = (searchJson.organic || []).map((item: any) => ({
            title: item.title,
            snippet: item.snippet,
            url: item.link,
            domain: new URL(item.link).hostname,
            isOfficial: /gov\.in|nic\.in|nta\.ac\.in|ncert\.nic\.in/i.test(item.link),
          }));
        }
      } catch (e) {
        console.warn("Serper search failed:", e);
      }
    }

    const systemPrompt = `You are StudyMate Smart Search academic breakdown engine.
Provide a pedagogically sound, high-yield conceptual breakdown for students preparing for ${examName}.
Respond in strict JSON format:
{
  "studentFriendlyAnswer": "Markdown formatted explanation with steps and analogies...",
  "keyPoints": ["Key point 1", "Key point 2", "Key point 3"],
  "formulasAndDefinitions": ["Formula 1", "Definition 1"],
  "sources": [
    {
      "title": "NCERT / Authority Reference",
      "snippet": "Textbook summary...",
      "url": "https://ncert.nic.in",
      "domain": "ncert.nic.in",
      "isOfficial": true
    }
  ],
  "sourcesDisagree": false,
  "disagreementDetails": "",
  "suggestedQuestions": ["Follow up 1?", "Follow up 2?"],
  "practiceQuestions": [
    {
      "questionText": "MCQ Question text?",
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "correctOptionIndex": 0,
      "explanation": "Step by step solution..."
    }
  ]
}`;

    const prompt = `Query: "${query}"\nExam: ${examName}\nSubject: ${subject}\n${webSearchSnippets.length > 0 ? "Web Search Reference:\n" + JSON.stringify(webSearchSnippets) : ""}`;

    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${geminiApiKey}`;

    const geminiRes = await fetch(geminiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents: [{ role: "user", parts: [{ text: prompt }] }],
        generationConfig: { temperature: 0.2 },
        systemInstruction: { parts: [{ text: systemPrompt }] },
      }),
    });

    if (!geminiRes.ok) {
      throw new Error(`Gemini API error: ${geminiRes.status}`);
    }

    const geminiData = await geminiRes.json();
    const rawText = geminiData.candidates?.[0]?.content?.parts?.[0]?.text || "{}";
    const cleaned = rawText.replace(/```json/g, "").replace(/```/g, "").trim();
    const parsed = JSON.parse(cleaned);

    return new Response(JSON.stringify(parsed), {
      headers: { ...corsHeaders, "Content-Type": "application/json" },
    });
  } catch (err: any) {
    console.error("Error in smart-search function:", err);
    return new Response(
      JSON.stringify({ error: err.message || "Failed to perform smart search" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
