"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter, useSearchParams, usePathname } from "next/navigation";
import { motion, AnimatePresence } from "framer-motion";
import { Mail, Check, Loader2, ArrowRight } from "lucide-react";
import api from "@/lib/api";

export default function GmailConnectionCard() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [connected, setConnected] = useState<boolean | null>(null);
  const [connecting, setConnecting] = useState(false);
  const [justConnected, setJustConnected] = useState(false);

  const checkStatus = useCallback(async () => {
    try {
      const res = await api.get<{ connected: boolean }>("/api/gmail/status");
      setConnected(res.data.connected);
    } catch {
      setConnected(false);
    }
  }, []);

  useEffect(() => {
    checkStatus();
  }, [checkStatus]);

  useEffect(() => {
    if (searchParams.get("gmail") === "connected") {
      setJustConnected(true);
      setConnected(true);
      router.replace(pathname);
    }
  }, [searchParams, pathname, router]);

  async function handleConnect() {
    setConnecting(true);
    try {
      const res = await api.get<{ authUrl: string }>("/api/gmail/connect");
      window.location.href = res.data.authUrl;
    } catch {
      setConnecting(false);
    }
  }

  if (connected === null) {
    return (
      <div className="flex items-center gap-2 rounded-2xl border border-(--paper-line)] bg-white px-5 py-4 text-sm text-(--bone)]">
        <Loader2 className="h-4 w-4 animate-spin" />
        Checking Gmail connection...
      </div>
    );
  }

  return (
    <div className="rounded-2xl border border-(--paper-line)] bg-white p-5">
      <AnimatePresence>
        {justConnected && (
          <motion.div
            initial={{ opacity: 0, height: 0, marginBottom: 0 }}
            animate={{ opacity: 1, height: "auto", marginBottom: 12 }}
            exit={{ opacity: 0, height: 0, marginBottom: 0 }}
            className="flex items-center gap-2 rounded-xl bg-(--moss-wash)] px-3 py-2 text-sm font-medium text-(--moss)]"
          >
            <Check className="h-4 w-4" />
            Gmail connected successfully
          </motion.div>
        )}
      </AnimatePresence>

      <div className="flex items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <div
            className={`flex h-10 w-10 items-center justify-center rounded-xl ${
              connected ? "bg-(--moss-wash)]" : "bg-(--stamp-wash)]"
            }`}
          >
            <Mail
              className={`h-5 w-5 ${connected ? "text-(--moss)]" : "text-(--stamp)]"}`}
            />
          </div>
          <div>
            <h3 className="font-semibold text-(--ink)]">Gmail</h3>
            <p className="text-xs text-(--bone)]">
              {connected
                ? "Connected — reading billing emails soon"
                : "Not connected yet"}
            </p>
          </div>
        </div>

        {connected ? (
          <span className="flex items-center gap-1 rounded-full bg-(--moss-wash)] px-3 py-1 text-xs font-medium text-(--moss)]">
            <Check className="h-3 w-3" />
            Connected
          </span>
        ) : (
          <button
            onClick={handleConnect}
            disabled={connecting}
            className="flex items-center gap-2 rounded-xl bg-(--ink)] px-4 py-2 text-sm font-semibold text-(--paper)] transition hover:bg-(--ink-soft)] disabled:opacity-60"
          >
            {connecting ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <>
                Connect Gmail <ArrowRight className="h-4 w-4" />
              </>
            )}
          </button>
        )}
      </div>
    </div>
  );
}
