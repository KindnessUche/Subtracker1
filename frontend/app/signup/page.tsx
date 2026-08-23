"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { motion, useReducedMotion } from "framer-motion";
import {
  Mail,
  Lock,
  Eye,
  EyeOff,
  ArrowRight,
  Loader2,
  Check,
  Repeat,
} from "lucide-react";
import api, { saveToken } from "@/lib/api";
import AuthVisualPanel from "@/components/auth/AuthVisualPanel";

export default function SignupPage() {
  const router = useRouter();
  const reduceMotion = useReducedMotion();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const passwordValid = password.length >= 8;

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError(null);

    if (!passwordValid) {
      setError("Password must be at least 8 characters.");
      return;
    }

    setLoading(true);
    try {
      const res = await api.post("/api/auth/signup", { email, password });
      saveToken(res.data.token);
      router.push("/dashboard");
    } catch (err: any) {
      if (err.response?.status === 409) {
        setError("That email is already registered. Try logging in instead.");
      } else if (err.response?.status === 400) {
        setError("Please check your email and password and try again.");
      } else {
        setError("Something went wrong. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="flex min-h-screen">
      <AuthVisualPanel
        headline="Catch the price hike before it catches you."
        subtext="We read your billing emails, flag every increase, and let you decide what stays."
      />

      <div className="flex w-full flex-col justify-center px-6 py-12 sm:px-12 lg:w-1/2 lg:px-20">
        <motion.div
          initial={reduceMotion ? false : { opacity: 0, y: 12 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, ease: "easeOut" }}
          className="mx-auto w-full max-w-sm"
        >
          <div className="mb-8 flex items-center gap-2 lg:hidden">
            <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-(--ink)]">
              <Repeat className="h-4 w-4 text-(--paper)]" strokeWidth={2.5} />
            </div>
            <span className="font-display text-lg font-semibold text-(--ink)]">
              Uchekd
            </span>
          </div>

          <h1 className="font-display text-2xl font-semibold text-(--ink)]">
            Create your account
          </h1>
          <p className="mt-2 text-sm text-(--bone)]">
            Start tracking every subscription in one place.
          </p>

          <form onSubmit={handleSubmit} className="mt-8 space-y-4">
            <div>
              <label className="mb-1.5 block text-sm font-medium text-(--ink)]">
                Email
              </label>
              <div className="relative">
                <Mail className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-(--bone)]" />
                <input
                  type="email"
                  required
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="you@example.com"
                  className="w-full rounded-xl border border-(--paper-line)] bg-white py-2.5 pl-10 pr-4 text-sm text-(--ink)] outline-none transition focus:border-(--stamp)] focus:ring-2 focus:ring-(--stamp-wash)]"
                />
              </div>
            </div>

            <div>
              <label className="mb-1.5 block text-sm font-medium text-(--ink)]">
                Password
              </label>
              <div className="relative">
                <Lock className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-(--bone)]" />
                <input
                  type={showPassword ? "text" : "password"}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="••••••••"
                  className="w-full rounded-xl border border-(--paper-line)] bg-white py-2.5 pl-10 pr-10 text-sm text-(--ink)] outline-none transition focus:border-(--stamp)] focus:ring-2 focus:ring-(--stamp-wash)]"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((v) => !v)}
                  className="absolute right-3.5 top-1/2 -translate-y-1/2 text-(--bone)] hover:text-(--ink)]"
                  aria-label={showPassword ? "Hide password" : "Show password"}
                >
                  {showPassword ? (
                    <EyeOff className="h-4 w-4" />
                  ) : (
                    <Eye className="h-4 w-4" />
                  )}
                </button>
              </div>
              <p
                className={`mt-1.5 flex items-center gap-1 text-xs ${
                  passwordValid ? "text-(--moss)]" : "text-(--bone)]"
                }`}
              >
                {passwordValid && <Check className="h-3 w-3" />}
                At least 8 characters
              </p>
            </div>

            {error && (
              <motion.p
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600"
              >
                {error}
              </motion.p>
            )}

            <button
              type="submit"
              disabled={loading}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-(--ink)] py-2.5 text-sm font-semibold text-(--paper)] transition hover:bg-(--ink-soft)] disabled:opacity-60"
            >
              {loading ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <>
                  Create account <ArrowRight className="h-4 w-4" />
                </>
              )}
            </button>
          </form>

          <p className="mt-6 text-center text-sm text-(--bone)]">
            Already have an account?{" "}
            <Link
              href="/login"
              className="font-medium text-(--ink)] underline underline-offset-2"
            >
              Log in
            </Link>
          </p>
        </motion.div>
      </div>
    </div>
  );
}
