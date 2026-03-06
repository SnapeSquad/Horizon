export interface ApiSession {
  accessToken?: string
  adminToken?: string
}

export type UserRole = "player" | "moderator" | "curator" | "admin" | "owner"

export interface AuthSuccessResponse {
  success: true
  token: string
  username: string
  has2FA?: boolean
}

export interface AuthNeed2FAResponse {
  success: false
  status: "NEED_2FA"
  requires2FA: true
  message: string
}

export interface ApiErrorResponse {
  success: false
  message: string
}

export interface AdminUser {
  id: number
  username: string
  role: UserRole | null
  currency: number | null
  hwid: string | null
  created_at: string
}

export interface BanEntry {
  id: number
  hwid: string
  reason: string | null
  banned_by: string | null
  created_at: string
}

export interface AdminCosmetic {
  id: number
  name: string
  description: string | null
  pivot_point: string
  price: number
  rarity: string
  is_active: number
  created_at: string
}

export interface AdminNews {
  id: number
  title: string
  content: string
  image_url: string | null
  author: string | null
  views: number
  created_at: string
  updated_at: string
}

type RequestOptions = Omit<RequestInit, "headers"> & {
  headers?: Record<string, string>
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL as string | undefined)?.replace(/\/+$/, "") || "http://127.0.0.1:3000"

export class ApiError extends Error {
  status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

function buildHeaders(session?: ApiSession, headers?: Record<string, string>) {
  const result: Record<string, string> = {
    ...(headers || {}),
  }

  if (session?.accessToken) {
    result.Authorization = `Bearer ${session.accessToken}`
  }
  if (session?.adminToken) {
    result["x-admin-token"] = session.adminToken
  }

  return result
}

async function parseJson<T>(response: Response): Promise<T> {
  try {
    return (await response.json()) as T
  } catch {
    throw new ApiError("Некорректный ответ сервера", response.status)
  }
}

async function requestRaw<T>(path: string, options?: RequestOptions, session?: ApiSession) {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...options,
    headers: buildHeaders(session, options?.headers),
  })
  const json = await parseJson<T>(response)
  return { response, json }
}

async function requestOrThrow<T extends { success?: boolean; message?: string }>(
  path: string,
  options?: RequestOptions,
  session?: ApiSession
) {
  const { response, json } = await requestRaw<T>(path, options, session)

  if (!response.ok) {
    const message = json.message || `HTTP ${response.status}`
    throw new ApiError(message, response.status)
  }

  if (json.success === false) {
    throw new ApiError(json.message || "Операция завершилась с ошибкой", response.status)
  }

  return json
}

export async function login(username: string, password: string) {
  const { json } = await requestRaw<AuthSuccessResponse | AuthNeed2FAResponse | ApiErrorResponse>(
    "/api/auth/login",
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, password }),
    }
  )

  return json
}

export async function verify2FA(username: string, code: string) {
  return requestOrThrow<AuthSuccessResponse | ApiErrorResponse>(
    "/api/auth/verify-2fa",
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, code }),
    }
  )
}

export async function verifyAdminAccess(session: ApiSession) {
  const response = await requestOrThrow<{ success: true; users: AdminUser[] }>("/api/admin/users", undefined, session)
  return response.users
}

export async function getAdminUsers(session: ApiSession) {
  const response = await requestOrThrow<{ success: true; users: AdminUser[] }>("/api/admin/users", undefined, session)
  return response.users
}

export async function updateUserRole(session: ApiSession, userId: number, role: UserRole) {
  return requestOrThrow<{ success: true; message: string; role: UserRole }>(
    `/api/admin/users/${userId}/role`,
    {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ role }),
    },
    session
  )
}

export async function getBans(session: ApiSession) {
  const response = await requestOrThrow<{ success: true; bans: BanEntry[] }>("/api/admin/bans", undefined, session)
  return response.bans
}

export async function banUser(session: ApiSession, hwid: string, reason: string, username: string) {
  return requestOrThrow<{ success: true; message: string }>(
    "/api/admin/users/ban",
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ hwid, reason, username }),
    },
    session
  )
}

export async function unbanUser(session: ApiSession, hwid: string) {
  return requestOrThrow<{ success: true; message: string }>(
    `/api/admin/users/unban/${encodeURIComponent(hwid)}`,
    { method: "DELETE" },
    session
  )
}

export async function giveCurrency(session: ApiSession, username: string, amount: number) {
  return requestOrThrow<{ success: true; message: string; balance: number }>(
    "/api/admin/currency/give",
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username, amount }),
    },
    session
  )
}

export async function getAdminCosmetics(session: ApiSession) {
  const response = await requestOrThrow<{ success: true; cosmetics: AdminCosmetic[] }>(
    "/api/admin/cosmetics",
    undefined,
    session
  )
  return response.cosmetics
}

export async function uploadCosmetic(
  session: ApiSession,
  payload: {
    name: string
    description: string
    pivotPoint: string
    price: number
    rarity: string
    modelFile: File
    textureFile: File
  }
) {
  const formData = new FormData()
  formData.append("name", payload.name)
  formData.append("description", payload.description)
  formData.append("pivot_point", payload.pivotPoint)
  formData.append("price", String(payload.price))
  formData.append("rarity", payload.rarity)
  formData.append("model", payload.modelFile)
  formData.append("texture", payload.textureFile)

  return requestOrThrow<{ success: true; message: string }>(
    "/api/admin/cosmetics",
    {
      method: "POST",
      body: formData,
    },
    session
  )
}

export async function deleteCosmetic(session: ApiSession, cosmeticId: number) {
  return requestOrThrow<{ success: true; message: string }>(
    `/api/admin/cosmetics/${cosmeticId}`,
    { method: "DELETE" },
    session
  )
}

export async function getAdminNews(session: ApiSession) {
  const response = await requestOrThrow<{ success: true; news: AdminNews[] }>("/api/admin/news", undefined, session)
  return response.news
}

export async function createNews(
  session: ApiSession,
  payload: { title: string; content: string; imageUrl?: string; author?: string }
) {
  return requestOrThrow<{ success: true; message: string; news_id: number }>(
    "/api/admin/news",
    {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        title: payload.title,
        content: payload.content,
        image_url: payload.imageUrl || null,
        author: payload.author || null,
      }),
    },
    session
  )
}

export async function deleteNews(session: ApiSession, newsId: number) {
  return requestOrThrow<{ success: true; message: string }>(
    `/api/admin/news/${newsId}`,
    { method: "DELETE" },
    session
  )
}
