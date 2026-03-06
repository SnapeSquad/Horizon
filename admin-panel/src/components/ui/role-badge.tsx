import { cn } from "@/lib/utils"
import { colors } from "@/lib/colors"

type RoleType = keyof typeof colors.roles

interface RoleBadgeProps {
  role: RoleType
  username: string
  className?: string
}

export function RoleBadge({ role, username, className }: RoleBadgeProps) {
  const roleConfig = colors.roles[role]
  
  const getRoleStyles = () => {
    if ('start' in roleConfig && 'end' in roleConfig) {
      // Gradient role
      const gradient = `linear-gradient(90deg, ${roleConfig.start}, ${roleConfig.end})`
      return {
        background: gradient,
        WebkitBackgroundClip: 'text',
        WebkitTextFillColor: 'transparent',
        backgroundClip: 'text',
      }
    } else {
      // Solid color role
      return {
        color: roleConfig.color,
      }
    }
  }

  const getRoleClasses = () => {
    const classes: string[] = []
    
    if (roleConfig.weight.includes('bold')) classes.push('font-bold')
    if (roleConfig.weight.includes('italic')) classes.push('italic')
    if (roleConfig.weight.includes('underline')) classes.push('underline')
    
    if (roleConfig.effect === 'shimmering') {
      if (role === 'owner') classes.push('shimmer-text shimmer-owner')
      if (role === 'curator') classes.push('shimmer-text shimmer-curator')
    }
    
    return classes
  }

  return (
    <span
      className={cn(...getRoleClasses(), className)}
      style={getRoleStyles()}
    >
      {username}
    </span>
  )
}
