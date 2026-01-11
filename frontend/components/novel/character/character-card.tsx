'use client'

import {
  Card,
  CardContent,
  CardHeader,
} from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import type { Character } from '@/lib/types/character'
import { MoreVertical, Pencil, Trash2 } from 'lucide-react'

interface CharacterCardProps {
  character: Character
  onClick?: () => void
  onDelete?: () => void
}

const roleTypeMap: Record<string, { label: string, color: string }> = {
  protagonist: { label: '主角', color: 'bg-amber-500' },
  supporting: { label: '配角', color: 'bg-blue-500' },
  antagonist: { label: '反派', color: 'bg-red-500' },
  minor: { label: '龙套', color: 'bg-gray-500' },
}

export function CharacterCard({ character, onClick, onDelete }: CharacterCardProps) {
  const roleType = roleTypeMap[character.roleType || 'minor'] || roleTypeMap.minor
  const initials = character.name.slice(0, 2)
  const isOrganization = character.isOrganization === 1

  return (
    <Card className="hover:shadow-md transition-shadow relative group">
      <CardHeader className="pb-3">
        <div className="flex items-center gap-3">
          <Avatar className="h-12 w-12 cursor-pointer" onClick={onClick}>
            <AvatarImage src={(character as any).avatarUrl} />
            <AvatarFallback
              className="text-white"
              style={{ backgroundColor: isOrganization ? '#6B7280' : ((character as any).color || '#4D8088') }}
            >
              {initials}
            </AvatarFallback>
          </Avatar>
          <div className="flex-1 min-w-0 cursor-pointer" onClick={onClick}>
            <div className="flex items-center gap-2">
              <h3 className="font-medium truncate">{character.name}</h3>
              <Badge
                className={`${isOrganization ? 'bg-gray-600' : roleType.color} text-white text-xs`}
                variant="secondary"
              >
                {isOrganization ? '组织' : roleType.label}
              </Badge>
            </div>
            {!isOrganization && (character.age || character.gender) && (
              <p className="text-sm text-muted-foreground">
                {character.age ? `${character.age}岁` : ''}
                {character.age && character.gender ? ' · ' : ''}
                {character.gender || ''}
              </p>
            )}
            {isOrganization && character.organizationType && (
              <p className="text-sm text-muted-foreground">
                {character.organizationType}
              </p>
            )}
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="h-8 w-8 opacity-0 group-hover:opacity-100 transition-opacity"
                onClick={(e) => e.stopPropagation()}
              >
                <MoreVertical className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem onClick={onClick}>
                <Pencil className="mr-2 h-4 w-4" />
                编辑
              </DropdownMenuItem>
              <DropdownMenuItem
                className="text-destructive focus:text-destructive"
                onClick={(e) => {
                  e.stopPropagation()
                  onDelete?.()
                }}
              >
                <Trash2 className="mr-2 h-4 w-4" />
                删除
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </CardHeader>

      <CardContent className="pt-0 cursor-pointer" onClick={onClick}>
        {/* 性格特点 */}
        {character.personality && (
          <p className="text-sm text-muted-foreground line-clamp-2 mb-3">
            {character.personality}
          </p>
        )}

        {/* 外貌描述 */}
        {character.appearance && (
          <p className="text-sm text-muted-foreground line-clamp-2 mb-3">
            <span className="font-medium">外貌:</span> {character.appearance}
          </p>
        )}

        {/* 背景信息 */}
        {character.background && (
          <p className="text-xs text-muted-foreground italic mt-3 border-l-2 pl-2 line-clamp-2">
            {character.background}
          </p>
        )}
      </CardContent>
    </Card>
  )
}
