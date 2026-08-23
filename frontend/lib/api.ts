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

const TOKEN_KEY = "uchekd_token";

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

api.interceptors.request.use((config) => {
  if (typeof window !== "undefined") {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
  }
  return config;
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (
      error.response?.status === 401 &&
      typeof window !== "undefined" &&
      window.location.pathname !== "/login"
    ) {
      localStorage.removeItem(TOKEN_KEY);
      window.location.href = "/login";
    }
    return Promise.reject(error);
  }
);

export function saveToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token);
}

export function getToken(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(TOKEN_KEY);
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
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
}

export default api;