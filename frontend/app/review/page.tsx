"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { motion, AnimatePresence } from "framer-motion";
import {
  ArrowLeft,
  Check,
  X,
  RefreshCw,
  Loader2,
  Inbox,
  TrendingUp,
  Sparkles,
} from "lucide-react";
import api, { getToken, ReviewQueueItem } from "@/lib/api";

function formatMoney(amount: number, currency: string) {
  return `${amount.toFixed(2)} ${currency}`;
}

export default function ReviewQueuePage() {
  const router = useRouter();
  const [items, setItems] = useState<ReviewQueueItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [syncing, setSyncing] = useState(false);
  const [actingOn, setActingOn] = useState<string | null>(null);
  const [syncMessage, setSyncMessage] = useState<string | null>(null);

  const loadItems = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get<ReviewQueueItem[]>("/api/review-queue");
      setItems(res.data);
    } catch {
      // interceptor handles 401
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!getToken()) {
      router.push("/login");
      return;
    }
    loadItems();
  }, [router, loadItems]);

  async function handleSync() {
    setSyncing(true);
    setSyncMessage(null);
    try {
      const res = await api.post<{ queued: number }>("/api/review-queue/sync");
      setSyncMessage(
        res.data.queued > 0
          ? `Found ${res.data.queued} new item${res.data.queued === 1 ? "" : "s"}`
          : "No new subscriptions found",
      );
      await loadItems();
    } catch {
      setSyncMessage("Sync failed — try again in a moment");
    } finally {
      setSyncing(false);
    }
  }

  async function handleApprove(id: string) {
    setActingOn(id);
    try {
      await api.post(`/api/review-queue/${id}/approve`);
      setItems((prev) => prev.filter((i) => i.id !== id));
    } catch {
      // leave it in the list, user can retry
    } finally {
      setActingOn(null);
    }
  }

  async function handleDismiss(id: string) {
    setActingOn(id);
    try {
      await api.post(`/api/review-queue/${id}/dismiss`);
      setItems((prev) => prev.filter((i) => i.id !== id));
    } catch {
      // leave it in the list, user can retry
    } finally {
      setActingOn(null);
    }
  }

  return (
    <div className="min-h-screen">
      <header className="border-b border-(--paper-line)] bg-white">
        <div className="mx-auto flex max-w-3xl items-center justify-between px-6 py-4">
          <Link
            href="/dashboard"
            className="flex items-center gap-1.5 text-sm text-(--bone)] transition hover:text-(--ink)]"
          >
            <ArrowLeft className="h-4 w-4" />
            Dashboard
          </Link>
          <button
            onClick={handleSync}
            disabled={syncing}
            className="flex items-center gap-2 rounded-xl bg-(--ink)] px-4 py-2 text-sm font-semibold text-(--paper)] transition hover:bg-(--ink-soft)] disabled:opacity-60"
          >
            {syncing ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <RefreshCw className="h-4 w-4" />
            )}
            Sync Gmail
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-3xl px-6 py-10">
        <h1 className="font-display text-2xl font-semibold text-(--ink)]">
          Review queue
        </h1>
        <p className="mt-1 text-sm text-(--bone)]">
          Nothing saves automatically — confirm each one before it becomes a
          subscription.
        </p>

        <AnimatePresence>
          {syncMessage && (
            <motion.p
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: "auto" }}
              exit={{ opacity: 0, height: 0 }}
              className="mt-4 rounded-lg bg-(--moss-wash)] px-3 py-2 text-sm text-(--moss)]"
            >
              {syncMessage}
            </motion.p>
          )}
        </AnimatePresence>

        {loading ? (
          <div className="mt-16 flex justify-center">
            <Loader2 className="h-6 w-6 animate-spin text-(--bone)]" />
          </div>
        ) : items.length === 0 ? (
          <div className="mt-16 flex flex-col items-center rounded-2xl border border-dashed border-(--paper-line)] py-16 text-center">
            <Inbox className="h-8 w-8 text-(--bone)]" />
            <p className="mt-3 text-sm font-medium text-(--ink)]">
              Nothing to review
            </p>
            <p className="mt-1 text-sm text-(--bone)]">
              Sync Gmail to check for new billing emails.
            </p>
          </div>
        ) : (
          <div className="mt-6 space-y-3">
            <AnimatePresence>
              {items.map((item) => (
                <motion.div
                  key={item.id}
                  layout
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, x: -20 }}
                  className="rounded-2xl border border-(--paper-line)] bg-white p-5"
                >
                  <div className="flex items-start justify-between gap-4">
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="font-semibold text-(--ink)]">
                          {item.merchantName}
                        </h3>
                        {item.type === "PRICE_CHANGE" ? (
                          <span className="flex items-center gap-1 rounded-full bg-(--stamp-wash)] px-2 py-0.5 text-[11px] font-medium text-(--stamp)]">
                            <TrendingUp className="h-3 w-3" />
                            price change
                          </span>
                        ) : (
                          <span className="rounded-full bg-neutral-100 px-2 py-0.5 text-[11px] font-medium text-neutral-500">
                            new
                          </span>
                        )}
                      </div>
                      {item.rawSnippet && (
                        <p className="mt-1 line-clamp-2 text-xs text-(--bone)]">
                          {item.rawSnippet}
                        </p>
                      )}
                    </div>
                    {item.confidenceScore !== null && (
                      <span className="flex shrink-0 items-center gap-1 text-xs text-(--bone)]">
                        <Sparkles className="h-3 w-3" />
                        {Math.round(item.confidenceScore * 100)}%
                      </span>
                    )}
                  </div>

                  <div className="mt-3 flex items-baseline gap-2">
                    {item.previousAmount != null && (
                      <span className="font-mono-num text-sm text-(--ink-faint)] line-through">
                        {formatMoney(item.previousAmount, item.currency)}
                      </span>
                    )}
                    <span className="font-mono-num text-xl font-bold text-(--ink)]">
                      {formatMoney(item.amount, item.currency)}
                    </span>
                    <span className="text-xs text-(--bone)]">
                      / {item.billingCycle.toLowerCase()}
                    </span>
                  </div>

                  <div className="mt-4 flex gap-2">
                    <button
                      onClick={() => handleApprove(item.id)}
                      disabled={actingOn === item.id}
                      className="flex flex-1 items-center justify-center gap-1.5 rounded-xl bg-(--moss)] py-2 text-sm font-semibold text-white transition hover:opacity-90 disabled:opacity-50"
                    >
                      {actingOn === item.id ? (
                        <Loader2 className="h-4 w-4 animate-spin" />
                      ) : (
                        <Check className="h-4 w-4" />
                      )}
                      Approve
                    </button>
                    <button
                      onClick={() => handleDismiss(item.id)}
                      disabled={actingOn === item.id}
                      className="flex flex-1 items-center justify-center gap-1.5 rounded-xl border border-(--paper-line)] py-2 text-sm font-semibold text-(--ink)] transition hover:bg-neutral-50 disabled:opacity-50"
                    >
                      <X className="h-4 w-4" />
                      Dismiss
                    </button>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        )}
      </main>
    </div>
  );
}
