import { useState } from "react"
import { GlassPanel } from "@/components/ui/glass-panel"
import { GradientButton } from "@/components/ui/gradient-button"
import { Eye, EyeOff } from "lucide-react"

export function AuthForm() {
  const [showPassword, setShowPassword] = useState(false)
  const [is2FA, setIs2FA] = useState(false)
  const [code, setCode] = useState(['', '', '', '', '', ''])

  const handle2FACodeChange = (index: number, value: string) => {
    if (value.length > 1) return
    const newCode = [...code]
    newCode[index] = value
    setCode(newCode)
    
    // Auto-focus next input
    if (value && index < 5) {
      const nextInput = document.getElementById(`code-${index + 1}`)
      nextInput?.focus()
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center bg-background relative overflow-hidden">
      {/* Blurred background */}
      <div className="absolute inset-0 bg-gradient-to-br from-purple-900/20 to-blue-900/20 blur-3xl" />
      
      <GlassPanel className="w-full max-w-md p-8 relative z-10">
        {!is2FA ? (
          <>
            <h2 className="text-3xl font-bold text-center mb-8 font-minecraft">
              Вход в систему
            </h2>
            
            <div className="space-y-6">
              {/* Login Input */}
              <div>
                <input
                  type="text"
                  placeholder="Логин"
                  className="input-bottom-border w-full px-4 py-3 text-text-main placeholder-text-muted bg-transparent"
                />
              </div>
              
              {/* Password Input */}
              <div className="relative">
                <input
                  type={showPassword ? "text" : "password"}
                  placeholder="Пароль"
                  className="input-bottom-border w-full px-4 py-3 pr-12 text-text-main placeholder-text-muted bg-transparent"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-4 top-1/2 -translate-y-1/2 text-text-muted hover:text-accent transition-colors"
                >
                  {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                </button>
              </div>
              
              {/* Forgot Password */}
              <div className="flex items-center justify-between">
                <button
                  type="button"
                  className="text-accent hover:text-primary-start transition-colors text-sm"
                >
                  Забыли пароль?
                </button>
              </div>
              
              {/* Login Button */}
              <GradientButton
                variant="pulse"
                className="w-full"
                onClick={() => setIs2FA(true)}
              >
                Войти
              </GradientButton>
            </div>
          </>
        ) : (
          <>
            <h2 className="text-2xl font-bold text-center mb-4 font-minecraft">
              Двухфакторная аутентификация
            </h2>
            <p className="text-text-muted text-center mb-8">
              Введите код из Telegram
            </p>
            
            <div className="flex gap-3 justify-center mb-6">
              {code.map((digit, index) => (
                <GlassPanel
                  key={index}
                  className="w-14 h-14 flex items-center justify-center"
                >
                  <input
                    id={`code-${index}`}
                    type="text"
                    inputMode="numeric"
                    maxLength={1}
                    value={digit}
                    onChange={(e) => handle2FACodeChange(index, e.target.value)}
                    className="w-full h-full text-center text-2xl font-bold bg-transparent text-text-main outline-none"
                  />
                </GlassPanel>
              ))}
            </div>
            
            <GradientButton
              variant="pulse"
              className="w-full"
              onClick={() => {
                // Handle 2FA verification
                console.log('2FA Code:', code.join(''))
              }}
            >
              Подтвердить
            </GradientButton>
            
            <button
              type="button"
              onClick={() => setIs2FA(false)}
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
