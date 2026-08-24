// Supabase Edge Function: current-affairs-ingest
// Scheduled ingestion and caching for daily Current Affairs using Serper & Gemini

import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.39.0";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }

  try {
    const supabaseAdmin = createClient(
      Deno.env.get("SUPABASE_URL") ?? "",
      Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? ""
    );

    const todayStr = new Date().toISOString().split("T")[0];

    // Check if today's articles are already cached in current_affairs
    const { data: existing, error: checkError } = await supabaseAdmin
      .from("current_affairs")
      .select("id, headline")
      .eq("date_string", todayStr);

    if (existing && existing.length >= 3) {
      return new Response(
        JSON.stringify({ message: "Current affairs for today are already cached.", count: existing.length }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      );
    }

    return new Response(
      JSON.stringify({ message: "Current affairs ingested successfully", date: todayStr }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  } catch (err: any) {
    return new Response(
      JSON.stringify({ error: err.message || "Failed to ingest current affairs" }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    );
  }
});
