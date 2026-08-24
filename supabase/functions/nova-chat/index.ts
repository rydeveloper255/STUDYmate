// Supabase Edge Function: nova-chat
// Secure server-side gateway to Gemini API & Serper for NOVA Tutor
// Deno TypeScript environment on Supabase Edge Functions

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-request-id",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  const requestId = req.headers.get("x-request-id") || crypto.randomUUID();

  try {
    const authHeader = req.headers.get("Authorization");
    if (!authHeader) {
      return new Response(
        JSON.stringify({ error: "Missing authorization header", requestId }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const supabaseClient = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_ANON_KEY") ?? "",
      { global: { headers: { Authorization: authHeader } } }
    );

    const { data: { user }, error: authError } = await supabaseClient.auth.getUser();
    if (authError || !user) {
      return new Response(
        JSON.stringify({ error: "Invalid user token", requestId }),
        { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const body = await req.json();
    const userPrompt = body.userPrompt?.trim() || "";
    const conversationHistory = body.conversationHistory || [];
    const studyContext = body.studyContext || {};

    if (!userPrompt) {
      return new Response(
        JSON.stringify({ error: "userPrompt cannot be empty", requestId }),
        { status: 400, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const geminiApiKey = Deno.env.get("GEMINI_API_KEY");
    if (!geminiApiKey) {
      return new Response(
        JSON.stringify({
          text: "NOVA backend is currently being configured. Please ensure GEMINI_API_KEY secret is active.",
          actions: [],
          groundedSources: [],
          requestId
        }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    // Call Gemini 2.5 Flash model server-side
    const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${geminiApiKey}`;

    const systemPrompt = `You are NOVA, the world-class academic tutor and mentor for ${studyContext.studentName || "Student"} preparing for ${studyContext.targetExam || "Competitive Exams"}.
Provide clear, pedagogically sound, encouraging explanations.
When relevant, recommend deep-link actions in this format:
- [ACTION:OPEN_CURRENT_AFFAIRS:{}]
- [ACTION:OPEN_MOCK_TEST:{"exam":"${studyContext.targetExam || "JEE Main"}"}]
- [ACTION:OPEN_SMART_NOTES:{}]
- [ACTION:OPEN_STUDY_PLAN:{}]
- [ACTION:OPEN_FOCUS_MODE:{}]
- [ACTION:OPEN_MISTAKES:{}]`;

    const contents = [
      { role: "user", parts: [{ text: `SYSTEM DIRECTIVE:\n${systemPrompt}` }] },
      ...conversationHistory.map((msg: any) => ({
        role: msg.role === "USER" || msg.role === "user" ? "user" : "model",
        parts: [{ text: msg.text }]
      })),
      { role: "user", parts: [{ text: userPrompt }] }
    ];

    const geminiRes = await fetch(geminiUrl, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        contents,
        generationConfig: {
          temperature: 0.4,
          maxOutputTokens: 2048,
        }
      })
    });

    if (!geminiRes.ok) {
      const errText = await geminiRes.text();
      return new Response(
        JSON.stringify({
          error: "Gemini API failure",
          details: errText,
          requestId
        }),
        { status: 502, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    const geminiData = await geminiRes.json();
    const candidateText = geminiData.candidates?.[0]?.content?.parts?.[0]?.text || "I am ready to help you with your studies!";

    return new Response(
      JSON.stringify({
        text: candidateText,
        actions: [],
        groundedSources: [],
        requestId
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: any) {
    return new Response(
      JSON.stringify({ error: err.message || "Internal server error", requestId }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
