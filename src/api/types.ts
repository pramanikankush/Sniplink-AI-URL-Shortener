export interface ShortenRequest {
  url: string;
}

export interface ShortenResponse {
  code: string;
  shortUrl: string;
  riskScore: number;
}

export interface StatsResponse {
  code: string;
  longUrl: string;
  clicks: number;
  createdAt: string; // ISO-8601
}

export interface ApiError {
  error: string;
  status: number;
  timestamp?: string;
}
