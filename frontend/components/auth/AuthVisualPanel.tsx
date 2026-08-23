"use client";

import { motion, useReducedMotion } from "framer-motion";
import { Repeat, Check } from "lucide-react";

interface AuthVisualPanelProps {
  headline: string;
  subtext: string;
}

const rows: {
  name: string;
  was: string | null;
  now: string;
  flagged: boolean;
}[] = [
  { name: "Netflix", was: "9.99", now: "13.99", flagged: true },
  { name: "Spotify", was: null, now: "11.99", flagged: false },
  { name: "Notion", was: null, now: "8.00", flagged: false },
];

export default function AuthVisualPanel({
  headline,
  subtext,
}: AuthVisualPanelProps) {
  const reduceMotion = useReducedMotion();

  return (
    <div className="relative hidden lg:flex lg:w-1/2 flex-col justify-between overflow-hidden bg-(--ink)] px-10 py-12">
      <div
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            "radial-gradient(ellipse at 50% 35%, rgba(246,239,225,0.06) 0%, transparent 60%)",
        }}
      />

      <div className="relative z-10 flex items-center gap-2">
        <div className="flex h-8 w-8 items-center justify-center rounded-lg bg-(--paper)]">
          <Repeat className="h-4 w-4 text-(--ink)]" strokeWidth={2.5} />
        </div>
        <span className="font-display text-lg font-semibold text-(--paper)]">
          Uchekd
        </span>
      </div>

      <div className="relative z-10 max-w-sm">
        <h2 className="font-display text-[28px] font-semibold italic leading-tight text-(--paper)]">
          {headline}
        </h2>
        <p className="mt-3 text-[15px] leading-relaxed text-(--bone)]">
          {subtext}
        </p>
      </div>

      <motion.div
        initial={reduceMotion ? false : { opacity: 0, y: 30, rotate: -6 }}
        animate={{ opacity: 1, y: 0, rotate: -2 }}
        transition={{ duration: 0.8, ease: [0.22, 1, 0.36, 1] }}
        className="relative z-10 w-full max-w-75 self-center"
      >
        <div className="torn-edge bg-(--paper)] px-5 pb-5 pt-6 shadow-[0_20px_40px_rgba(0,0,0,0.35)]">
          <div className="flex items-center justify-between font-mono-num text-[10px] uppercase tracking-widest text-(--ink-faint)]">
            <span>Uchekd receipt</span>
            <span>Jun 2026</span>
          </div>

          <div className="mt-4 space-y-2.5">
            {rows.map((row) => (
              <div
                key={row.name}
                className="flex items-center justify-between text-[13px]"
              >
                <span className="font-mono-num text-(--ink)]">{row.name}</span>
                <span className="font-mono-num flex items-center gap-1.5">
                  {row.was && (
                    <span className="text-(--ink-faint)] line-through">
                      ${row.was}
                    </span>
                  )}
                  {row.flagged ? (
                    <span className="font-semibold text-(--stamp)]">
                      ${row.now}
                    </span>
                  ) : (
                    <span className="flex items-center gap-1 text-(--ink)]">
                      <Check className="h-3 w-3 text-(--moss)]" />${row.now}
                    </span>
                  )}
                </span>
              </div>
            ))}
          </div>

          <div className="my-4 border-t border-dashed border-(--paper-line)]" />

          <div className="flex items-center justify-between font-mono-num text-[13px] font-semibold text-(--ink)]">
            <span>Total this month</span>
            <span>$33.98</span>
          </div>

          <div className="barcode mt-5 opacity-30" />
        </div>

        <motion.div
          initial={
            reduceMotion ? false : { opacity: 0, scale: 1.4, rotate: -25 }
          }
          animate={{ opacity: 1, scale: 1, rotate: -12 }}
          transition={{
            duration: 0.4,
            delay: reduceMotion ? 0 : 0.7,
            ease: "easeOut",
          }}
          className="absolute -right-3 top-10 rounded border-2 border-(--stamp)] px-2 py-1 font-mono-num text-[10px] font-bold uppercase tracking-wide text-(--stamp)]"
        >
          +40% ↑
        </motion.div>
      </motion.div>
    </div>
  );
}
