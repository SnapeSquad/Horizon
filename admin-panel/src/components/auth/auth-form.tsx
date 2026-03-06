import { type KeyboardEvent, useMemo, useState } from "react"
import { GlassPanel } from "@/components/ui/glass-panel"
import { GradientButton } from "@/components/ui/gradient-button"
import { Eye, EyeOff } from "lucide-react"
import {
  ApiError,
  type ApiSession,
  type AuthNeed2FAResponse,
  type AuthSuccessResponse,
  login,
  verify2FA,
  verifyAdminAccess,
} from "@/lib/api"

export interface AuthSession {
  username: string
  accessToken?: string
  adminToken?: string
}

interface AuthFormProps {
  onAuthenticated: (session: AuthSession) => void
}

function isAuthSuccess(value: unknown): value is AuthSuccessResponse {
  if (!value || typeof value !== "object") {
    return false
  }
  const response = value as Partial<AuthSuccessResponse>
  return response.success === true && typeof response.token === "string" && typeof response.username === "string"
}

function isNeed2FA(value: unknown): value is AuthNeed2FAResponse {
  if (!value || typeof value !== "object") {
    return false
  }
  const response = value as Partial<AuthNeed2FAResponse>
  return response.success === false && response.status === "NEED_2FA"
}

function sanitizeCodeDigit(value: string) {
  return value.replace(/\D/g, "").slice(0, 1)
}

export function AuthForm({ onAuthenticated }: AuthFormProps) {
  const [showPassword, setShowPassword] = useState(false)
  const [is2FA, setIs2FA] = useState(false)
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [adminToken, setAdminToken] = useState("")
  const [pendingUsername, setPendingUsername] = useState("")
  const [code, setCode] = useState(["", "", "", "", "", ""])
  const [errorMessage, setErrorMessage] = useState("")
  const [infoMessage, setInfoMessage] = useState("")
  const [isSubmitting, setIsSubmitting] = useState(false)

  const normalizedAdminToken = adminToken.trim()
  const codeValue = useMemo(() => code.join(""), [code])

  const resetMessages = () => {
    setErrorMessage("")
    setInfoMessage("")
  }

  const handle2FACodeChange = (index: number, rawValue: string) => {
    const value = sanitizeCodeDigit(rawValue)
    const updated = [...code]
    updated[index] = value
    setCode(updated)

    if (value && index < updated.length - 1) {
      const nextInput = document.getElementById(`code-${index + 1}`)
      nextInput?.focus()
    }
  }

  const handle2FAKeyDown = (index: number, event: KeyboardEvent<HTMLInputElement>) => {
    if (event.key === "Backspace" && !code[index] && index > 0) {
      const prevInput = document.getElementById(`code-${index - 1}`)
      prevInput?.focus()
    }
  }

  const runTokenOnlyLogin = async () => {
    if (!normalizedAdminToken) {
      throw new Error("Укажите логин/пароль или ADMIN_TOKEN.")
    }
    const session: ApiSession = { adminToken: normalizedAdminToken }
    await verifyAdminAccess(session)
    onAuthenticated({
      username: "admin",
      adminToken: normalizedAdminToken,
    })
  }

  const runCredentialLogin = async () => {
    const normalizedUsername = username.trim()
    if (!normalizedUsername || !password) {
      throw new Error("Укажите логин и пароль.")
    }

    const response = await login(normalizedUsername, password)

    if (isAuthSuccess(response)) {
      onAuthenticated({
        username: response.username,
        accessToken: response.token,
        adminToken: normalizedAdminToken || undefined,
      })
      return
    }

    if (isNeed2FA(response)) {
      setPendingUsername(normalizedUsername)
      setIs2FA(true)
      setInfoMessage(response.message || "Введите код из Telegram.")
      return
    }

    const responseMessage =
      typeof response === "object" && response && "message" in response && typeof response.message === "string"
        ? response.message
        : "Не удалось выполнить вход."
    throw new Error(responseMessage)
  }

  const handlePrimaryLogin = async () => {
    resetMessages()
    setIsSubmitting(true)

    try {
      if (!username.trim() && !password) {
        await runTokenOnlyLogin()
      } else {
        await runCredentialLogin()
      }
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message)
      } else if (error instanceof Error) {
        setErrorMessage(error.message)
      } else {
        setErrorMessage("Неожиданная ошибка авторизации.")
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  const handle2FAConfirm = async () => {
    resetMessages()
    if (codeValue.length !== 6 || !pendingUsername) {
      setErrorMessage("Введите 6-значный код подтверждения.")
      return
    }

    setIsSubmitting(true)
    try {
      const response = await verify2FA(pendingUsername, codeValue)
      if (!isAuthSuccess(response)) {
        throw new Error("Сервер вернул неожиданный ответ при проверке 2FA.")
      }

      onAuthenticated({
        username: response.username,
        accessToken: response.token,
        adminToken: normalizedAdminToken || undefined,
      })
    } catch (error) {
      if (error instanceof ApiError) {
        setErrorMessage(error.message)
      } else if (error instanceof Error) {
        setErrorMessage(error.message)
      } else {
        setErrorMessage("Ошибка при проверке 2FA.")
      }
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-background relative overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-br from-purple-900/20 to-blue-900/20 blur-3xl" />

      <GlassPanel className="w-full max-w-md p-8 relative z-10">
        {!is2FA ? (
          <>
            <h2 className="text-3xl font-bold text-center mb-8 font-minecraft">Вход в систему</h2>

            <div className="space-y-6">
              <div>
                <input
                  type="text"
                  value={username}
                  onChange={(event) => setUsername(event.target.value)}
                  placeholder="Логин"
                  autoComplete="username"
                  className="input-bottom-border w-full px-4 py-3 text-text-main placeholder-text-muted bg-transparent"
                />
              </div>

              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                  placeholder="Пароль"
                  autoComplete="current-password"
                  className="input-bottom-border w-full px-4 py-3 pr-12 text-text-main placeholder-text-muted bg-transparent"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword((value) => !value)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-text-muted hover:text-accent transition-colors"
                >
                  {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                </button>
              </div>

              <div>
                <input
                  type="password"
                  value={adminToken}
                  onChange={(event) => setAdminToken(event.target.value)}
                  placeholder="ADMIN_TOKEN (опционально)"
                  autoComplete="off"
                  className="input-bottom-border w-full px-4 py-3 text-text-main placeholder-text-muted bg-transparent"
                />
                <p className="text-xs text-text-muted mt-2">
                  Для админ-операций используйте роль `admin` в аккаунте или передайте `ADMIN_TOKEN`.
                </p>
              </div>

              {errorMessage && <p className="text-red-400 text-sm">{errorMessage}</p>}
              {infoMessage && <p className="text-accent text-sm">{infoMessage}</p>}

              <GradientButton variant="pulse" className="w-full" onClick={handlePrimaryLogin} disabled={isSubmitting}>
                {isSubmitting ? "Проверка..." : "Войти"}
              </GradientButton>
            </div>
          </>
        ) : (
          <>
            <h2 className="text-2xl font-bold text-center mb-4 font-minecraft">Двухфакторная аутентификация</h2>
            <p className="text-text-muted text-center mb-8">Введите код из Telegram</p>

            <div className="flex gap-3 justify-center mb-6">
              {code.map((digit, index) => (
                <GlassPanel key={index} className="w-14 h-14 flex items-center justify-center">
                  <input
                    id={`code-${index}`}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={digit}
                    onChange={(event) => handle2FACodeChange(index, event.target.value)}
                    onKeyDown={(event) => handle2FAKeyDown(index, event)}
                    className="w-full h-full text-center text-2xl font-bold bg-transparent text-text-main outline-none"
                  />
                </GlassPanel>
              ))}
            </div>

            {errorMessage && <p className="text-red-400 text-sm mb-3">{errorMessage}</p>}
            {infoMessage && <p className="text-accent text-sm mb-3">{infoMessage}</p>}

            <GradientButton variant="pulse" className="w-full" onClick={handle2FAConfirm} disabled={isSubmitting}>
              {isSubmitting ? "Проверка..." : "Подтвердить"}
            </GradientButton>

            <button
              type="button"
              onClick={() => {
                setIs2FA(false)
                setCode(["", "", "", "", "", ""])
                resetMessages()
              }}
              className="w-full mt-4 text-text-muted hover:text-accent transition-colors text-sm"
            >
              Назад
            </button>
          </>
        )}
      </GlassPanel>
    </div>
  )
}
