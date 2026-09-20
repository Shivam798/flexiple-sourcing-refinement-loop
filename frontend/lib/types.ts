/** Mirrors the backend's response shape. camelCase throughout; the API speaks camelCase. */

export type CompanyType = "startup" | "scaleup" | "enterprise" | "agency";
export type VerdictLabel = "strong" | "possible" | "weak";

export interface PastCompany {
  company: string;
  companyType: CompanyType;
  title: string;
  years: number;
}

export interface Profile {
  id: string;
  name: string;
  currentTitle: string;
  yearsExperience: number;
  location: string;
  currentCompany: string;
  currentCompanyType: CompanyType;
  skills: string[];
  pastCompanies: PastCompany[];
  education: string;
  summary: string;
}

export interface Filters {
  requiredSkills: string[];
  preferredSkills: string[];
  minYearsExperience: number | null;
  maxYearsExperience: number | null;
  locations: string[];
  includeRemote: boolean;
  currentCompanyTypes: CompanyType[];
  pastCompanyTypes: CompanyType[];
  titleKeywords: string[];
  excludeTitles: string[];
}

export interface Criterion {
  name: string;
  weight: number;
  whatGoodLooksLike: string;
  redFlags: string;
}

export interface Rubric {
  roleSummary: string;
  criteria: Criterion[];
  dealbreakers: string[];
}

export interface Evidence {
  field: string;
  value: string;
  verified: boolean;
}

export interface ScoredProfile {
  profile: Profile;
  score: number | null;
  verdict: VerdictLabel | null;
  why: string | null;
  evidence: Evidence[];
  concerns: string[];
}

export interface Change {
  target: string;
  from: string;
  to: string;
  reason: string;
}

export interface Round {
  number: number;
  filters: Filters;
  rubric: Rubric;
  recruiterFeedback: string | null;
  assistantReply: string | null;
  changes: Change[];
  relaxations: string[];
  results: ScoredProfile[];
  totalMatched: number;
  createdAt: string;
}

export interface Session {
  id: string;
  originalQuery: string;
  frozen: boolean;
  roundCount: number;
  rounds: Round[];
}

export type ApiErrorCode =
  | "BAD_REQUEST"
  | "SESSION_NOT_FOUND"
  | "SEARCH_FROZEN"
  | "LLM_NOT_CONFIGURED"
  | "LLM_RATE_LIMITED"
  | "LLM_INVALID_RESPONSE"
  | "LLM_UNAVAILABLE"
  | "LLM_ERROR"
  | "INTERNAL_ERROR"
  | "NETWORK";

export interface ApiErrorBody {
  code: ApiErrorCode;
  message: string;
  retryAfterSeconds: number | null;
}

export interface Health {
  status: string;
  profilesLoaded: number;
  llmConfigured: boolean;
}
