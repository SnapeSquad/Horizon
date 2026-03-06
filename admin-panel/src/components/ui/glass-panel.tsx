import { cn } from "@/lib/utils"
import type { ComponentPropsWithoutRef, ReactNode } from "react"

interface GlassPanelProps extends ComponentPropsWithoutRef<"div"> {
  children: ReactNode
  blur?: boolean
}

export function GlassPanel({
  children,
  className,
  blur = true,
  style,
  ...rest
}: GlassPanelProps) {
  return (
    <div
      {...rest}
      className={cn(
        "glass-panel",
        blur && "backdrop-blur-glass",
        className
      )}
      style={{
        background: 'rgba(30, 30, 45, 0.6)',
        backdropFilter: blur ? 'blur(50px)' : 'none',
        WebkitBackdropFilter: blur ? 'blur(50px)' : 'none',
        border: '1px solid rgba(255, 255, 255, 0.1)',
        ...style,
      }}
    >
      {children}
    </div>
  )
}
