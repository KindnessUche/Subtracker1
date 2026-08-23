"use client";

import { useState } from "react";
import { motion, AnimatePresence } from "framer-motion";
import { X, Loader2 } from "lucide-react";
import api, { SubscriptionInput } from "@/lib/api";

interface AddSubscriptionFormProps {
  isOpen: boolean;
  onClose: () => void;
  onCreated: () => void;
}

const CATEGORIES = ["streaming", "software", "fitness", "news", "other"];
const CYCLES = ["WEEKLY", "MONTHLY", "QUARTERLY", "ANNUAL"];

const emptyForm: SubscriptionInput = {
  merchantName: "",
  amount: 0,
  currency: "USD",
  billingCycle: "MONTHLY",
  nextBillingDate: "",
  category: "streaming",
  notes: "",
  isTrial: false,
  trialEndDate: null,
};

export default function AddSubscriptionForm({
  isOpen,
  onClose,
  onCreated,
}: AddSubscriptionFormProps) {
  const [form, setForm] = useState<SubscriptionInput>(emptyForm);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  function update<K extends keyof SubscriptionInput>(
    key: K,
    value: SubscriptionInput[K],
  ) {
    setForm((f) => ({ ...f, [key]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!form.merchantName.trim() || !form.amount || !form.nextBillingDate) {
      setError("Merchant name, amount, and next billing date are required.");
      return;
    }

    setLoading(true);
    try {
      await api.post("/api/subscriptions", form);
      setForm(emptyForm);
      onCreated();
    } catch {
      setError("Couldn't save this subscription. Please try again.");
    } finally {
      setLoading(false);
    }
  }

  const inputClass =
    "w-full rounded-xl border border-(--paper-line)] bg-white px-4 py-2.5 text-sm text-(--ink)] outline-none transition focus:border-(--stamp)] focus:ring-2 focus:ring-(--stamp-wash)]";
  const labelClass = "mb-1.5 block text-sm font-medium text-(--ink)]";

  return (
    <AnimatePresence>
      {isOpen && (
        <>
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="fixed inset-0 z-40 bg-black/40 backdrop-blur-sm"
          />
          <motion.div
            initial={{ x: "100%" }}
            animate={{ x: 0 }}
            exit={{ x: "100%" }}
            transition={{ type: "spring", damping: 28, stiffness: 260 }}
            className="fixed right-0 top-0 z-50 h-full w-full max-w-md overflow-y-auto bg-white p-6 shadow-2xl sm:p-8"
          >
            <div className="flex items-center justify-between">
              <h2 className="font-display text-lg font-semibold text-(--ink)]">
                Add subscription
              </h2>
              <button
                onClick={onClose}
                className="rounded-lg p-1.5 text-(--bone)] transition hover:bg-neutral-100 hover:text-(--ink)]"
                aria-label="Close"
              >
                <X className="h-5 w-5" />
              </button>
            </div>

            <form onSubmit={handleSubmit} className="mt-6 space-y-4">
              <div>
                <label className={labelClass}>Merchant name</label>
                <input
                  type="text"
                  value={form.merchantName}
                  onChange={(e) => update("merchantName", e.target.value)}
                  placeholder="Netflix"
                  className={inputClass}
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className={labelClass}>Amount</label>
                  <input
                    type="number"
                    step="0.01"
                    min="0"
                    value={form.amount || ""}
                    onChange={(e) =>
                      update("amount", parseFloat(e.target.value) || 0)
                    }
                    placeholder="13.99"
                    className={inputClass}
                  />
                </div>
                <div>
                  <label className={labelClass}>Currency</label>
                  <input
                    type="text"
                    value={form.currency}
                    onChange={(e) =>
                      update("currency", e.target.value.toUpperCase())
                    }
                    maxLength={3}
                    className={inputClass}
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className={labelClass}>Billing cycle</label>
                  <select
                    value={form.billingCycle}
                    onChange={(e) => update("billingCycle", e.target.value)}
                    className={inputClass}
                  >
                    {CYCLES.map((c) => (
                      <option key={c} value={c}>
                        {c.charAt(0) + c.slice(1).toLowerCase()}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className={labelClass}>Next billing date</label>
                  <input
                    type="date"
                    value={form.nextBillingDate}
                    onChange={(e) => update("nextBillingDate", e.target.value)}
                    className={inputClass}
                  />
                </div>
              </div>

              <div>
                <label className={labelClass}>Category</label>
                <select
                  value={form.category ?? "other"}
                  onChange={(e) => update("category", e.target.value)}
                  className={inputClass}
                >
                  {CATEGORIES.map((c) => (
                    <option key={c} value={c}>
                      {c.charAt(0).toUpperCase() + c.slice(1)}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className={labelClass}>
                  Notes <span className="text-(--bone)]">(optional)</span>
                </label>
                <textarea
                  value={form.notes ?? ""}
                  onChange={(e) => update("notes", e.target.value)}
                  rows={2}
                  className={`${inputClass} resize-none`}
                />
              </div>

              <label className="flex items-center gap-2 text-sm text-(--ink)]">
                <input
                  type="checkbox"
                  checked={form.isTrial ?? false}
                  onChange={(e) => update("isTrial", e.target.checked)}
                  className="h-4 w-4 rounded border-(--paper-line)] accent-(--stamp)]"
                />
                This is a free trial
              </label>

              {form.isTrial && (
                <div>
                  <label className={labelClass}>Trial ends</label>
                  <input
                    type="date"
                    value={form.trialEndDate ?? ""}
                    onChange={(e) => update("trialEndDate", e.target.value)}
                    className={inputClass}
                  />
                </div>
              )}

              {error && (
                <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">
                  {error}
                </p>
              )}

              <button
                type="submit"
                disabled={loading}
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-(--ink)] py-2.5 text-sm font-semibold text-(--paper)] transition hover:bg-(--ink-soft)] disabled:opacity-60"
              >
                {loading ? (
                  <Loader2 className="h-4 w-4 animate-spin" />
                ) : (
                  "Save subscription"
                )}
              </button>
            </form>
          </motion.div>
        </>
      )}
    </AnimatePresence>
  );
}
