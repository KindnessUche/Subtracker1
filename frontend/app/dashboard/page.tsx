"use client";

import { useEffect, useState, useCallback, Suspense } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { motion, AnimatePresence } from "framer-motion";
import {
  Plus,
  LogOut,
  Calendar,
  TrendingUp,
  Loader2,
  Inbox,
  Trash2,
  Repeat,
} from "lucide-react";
import api, { getToken, clearToken, Subscription } from "@/lib/api";
import AddSubscriptionForm from "@/components/AddSubscriptionForm";
import GmailConnectionCard from "@/components/GmailConnectionCard";

const statusStyles: Record<string, string> = {
  ACTIVE: "bg-(--moss-wash)] text-(--moss)]",
  PAUSED: "bg-neutral-100 text-neutral-500",
  CANCELLED: "bg-neutral-100 text-neutral-400",
};

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleDateString("en-US", {
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export default function DashboardPage() {
  const router = useRouter();
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [formOpen, setFormOpen] = useState(false);

  const loadSubscriptions = useCallback(async () => {
    setLoading(true);
    try {
      const res = await api.get<Subscription[]>("/api/subscriptions");
      setSubscriptions(res.data);
    } catch {
      // interceptor handles 401 redirect; other errors fail into empty state
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!getToken()) {
      router.push("/login");
      return;
    }
    loadSubscriptions();
  }, [router, loadSubscriptions]);

  function handleLogout() {
    clearToken();
    router.push("/login");
  }

  async function handleDelete(id: string) {
    setSubscriptions((subs) => subs.filter((s) => s.id !== id));
    try {
      await api.delete(`/api/subscriptions/${id}`);
    } catch {
      loadSubscriptions();
    }
  }

  const totalMonthly = subscriptions
    .filter((s) => s.status === "ACTIVE")
    .reduce((sum, s) => {
      const multiplier =
        s.billingCycle === "WEEKLY"
          ? 4.33
          : s.billingCycle === "QUARTERLY"
            ? 1 / 3
            : s.billingCycle === "ANNUAL"
              ? 1 / 12
              : 1;
      return sum + s.amount * multiplier;
    }, 0);

  return (
    <div className="min-h-screen">
      <header className="border-b border-(--paper-line)] bg-white">
        <div className="mx-auto flex max-w-5xl items-center justify-between px-6 py-4">
          <div className="flex items-center gap-2">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-(--ink)]">
              <Repeat className="h-4 w-4 text-(--paper)]" strokeWidth={2.5} />
            </div>
            <span className="font-display text-lg font-semibold text-(--ink)]">
              Uchekd
            </span>
          </div>
          <button
            onClick={handleLogout}
            className="flex items-center gap-1.5 rounded-lg px-3 py-1.5 text-sm text-(--bone)] transition hover:bg-neutral-100 hover:text-(--ink)]"
          >
            <LogOut className="h-4 w-4" />
            Log out
          </button>
        </div>
      </header>

      <main className="mx-auto max-w-5xl px-6 py-10">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <h1 className="font-display text-2xl font-semibold text-(--ink)]">
              Your subscriptions
            </h1>
            <p className="mt-1 text-sm text-(--bone)]">
              {loading
                ? "Loading..."
                : `$${totalMonthly.toFixed(2)} / month across ${subscriptions.length} subscription${subscriptions.length === 1 ? "" : "s"}`}
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Link
              href="/review"
              className="flex items-center gap-2 rounded-xl border border-(--paper-line)] px-4 py-2.5 text-sm font-semibold text-(--ink)] transition hover:bg-neutral-50"
            >
              <Inbox className="h-4 w-4" />
              Review queue
            </Link>
            <button
              onClick={() => setFormOpen(true)}
              className="flex items-center gap-2 rounded-xl bg-(--ink)] px-4 py-2.5 text-sm font-semibold text-(--paper)] transition hover:bg-(--ink-soft)]"
            >
              <Plus className="h-4 w-4" />
              Add subscription
            </button>
          </div>
        </div>

        <div className="mt-6">
          <Suspense
            fallback={
              <div className="flex items-center gap-2 rounded-2xl border border-(--paper-line)] bg-white px-5 py-4 text-sm text-(--bone)]">
                <Loader2 className="h-4 w-4 animate-spin" />
                Loading...
              </div>
            }
          >
            <GmailConnectionCard />
          </Suspense>
        </div>

        {loading ? (
          <div className="mt-16 flex justify-center">
            <Loader2 className="h-6 w-6 animate-spin text-(--bone)]" />
          </div>
        ) : subscriptions.length === 0 ? (
          <div className="mt-16 flex flex-col items-center rounded-2xl border border-dashed border-(--paper-line)] py-16 text-center">
            <Inbox className="h-8 w-8 text-(--bone)]" />
            <p className="mt-3 text-sm font-medium text-(--ink)]">
              No subscriptions yet
            </p>
            <p className="mt-1 text-sm text-(--bone)]">
              Add your first one to start tracking.
            </p>
            <button
              onClick={() => setFormOpen(true)}
              className="mt-5 rounded-xl bg-(--ink)] px-4 py-2 text-sm font-semibold text-(--paper)] transition hover:bg-(--ink-soft)]"
            >
              Add subscription
            </button>
          </div>
        ) : (
          <div className="mt-8 grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <AnimatePresence>
              {subscriptions.map((sub) => (
                <motion.div
                  key={sub.id}
                  layout
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  className="rounded-2xl border border-(--paper-line)] bg-white p-5 shadow-sm"
                >
                  <div className="flex items-start justify-between">
                    <div>
                      <h3 className="font-semibold text-(--ink)]">
                        {sub.merchantName}
                      </h3>
                      {sub.category && (
                        <p className="text-xs text-(--bone)]">{sub.category}</p>
                      )}
                    </div>
                    <div className="flex items-center gap-1">
                      <span
                        className={`rounded-full px-2 py-0.5 text-[11px] font-medium ${statusStyles[sub.status] ?? "bg-neutral-100 text-neutral-500"}`}
                      >
                        {sub.status}
                      </span>
                      <button
                        onClick={() => handleDelete(sub.id)}
                        className="rounded-lg p-1 text-(--bone)] transition hover:bg-red-50 hover:text-red-600"
                        aria-label={`Delete ${sub.merchantName}`}
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>

                  <div className="mt-4 flex items-baseline gap-1">
                    <span className="font-mono-num text-2xl font-bold text-(--ink)]">
                      {sub.amount.toFixed(2)}
                    </span>
                    <span className="text-sm text-(--bone)]">
                      {sub.currency} / {sub.billingCycle.toLowerCase()}
                    </span>
                  </div>

                  <div className="mt-4 flex items-center gap-1.5 text-xs text-(--bone)]">
                    <Calendar className="h-3.5 w-3.5" />
                    Renews {formatDate(sub.nextBillingDate)}
                  </div>

                  {sub.isTrial && (
                    <div className="mt-2 flex items-center gap-1.5 text-xs font-medium text-(--stamp)]">
                      <TrendingUp className="h-3.5 w-3.5" />
                      Trial
                      {sub.trialEndDate
                        ? ` · ends ${formatDate(sub.trialEndDate)}`
                        : ""}
                    </div>
                  )}
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        )}
      </main>

      <AddSubscriptionForm
        isOpen={formOpen}
        onClose={() => setFormOpen(false)}
        onCreated={() => {
          setFormOpen(false);
          loadSubscriptions();
        }}
      />
    </div>
  );
}
