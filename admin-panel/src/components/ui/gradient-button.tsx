import { cn } from "@/lib/utils"
import { ButtonHTMLAttributes, ReactNode } from "react"

interface GradientButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  children: ReactNode
  variant?: "default" | "pulse"
  className?: string
}

export function GradientButton({ 
  children, 
  variant = "default",
  className,
  ...props 
}: GradientButtonProps) {
  return (
    <button
      className={cn(
        "gradient-button px-6 py-3 rounded-lg text-white font-semibold",
        variant === "pulse" && "pulse-glow",
        className
      )}
      style={{
        background: 'linear-gradient(to right, #667eea, #764ba2)',
        boxShadow: '0 0 20px rgba(102, 126, 234, 0.5)',
      }}
      {...props}
    >
      {children}
    </button>
  )
}
