import axios from "axios";

export interface Subscription {
  id: string;
  merchantName: string;
  logoUrl: string | null;
  amount: number;
  currency: string;
  billingCycle: "WEEKLY" | "MONTHLY" | "QUARTERLY" | "ANNUAL";
  nextBillingDate: string;
  status: "ACTIVE" | "PAUSED" | "CANCELLED";
  category: string | null;
  notes: string | null;
  isTrial: boolean;
  trialEndDate: string | null;
  createdAt: string;
}

export interface SubscriptionInput {
  merchantName: string;
  logoUrl?: string | null;
  amount: number;
  currency?: string;
  billingCycle: string;
  nextBillingDate: string;
  status?: string;
  category?: string | null;
  notes?: string | null;
  isTrial?: boolean;
  trialEndDate?: string | null;
}

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  // The JWT now lives in an httpOnly cookie set by the backend (see AuthController),
  // instead of localStorage. withCredentials makes the browser send that cookie on
  // every request and store any Set-Cookie header from responses.
  withCredentials: true,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      error.response?.status === 401 &&
      typeof window !== "undefined" &&
      window.location.pathname !== "/login"
    ) {
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

/**
 * Checks whether the current session (httpOnly cookie) is valid, by asking the
 * backend rather than reading anything client-side — there's nothing for JS to read now.
 */
export async function isAuthenticated(): Promise<boolean> {
  try {
    await api.get("/api/auth/me");
    return true;
  } catch {
    return false;
  }
}

export async function logout(): Promise<void> {
  try {
    await api.post("/api/auth/logout");
  } finally {
    window.location.href = "/login";
  }
}

export interface ReviewQueueItem {
  id: string;
  type: "NEW_SUBSCRIPTION" | "PRICE_CHANGE";
  merchantName: string;
  amount: number;
  currency: string;
  billingCycle: string;
  previousAmount: number | null;
  confidenceScore: number | null;
  rawSnippet: string | null;
  status: string;
  createdAt: string;
  isTrial: boolean | null;
  trialEndDate: string | null;
}

export default api;