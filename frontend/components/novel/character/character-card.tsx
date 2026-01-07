'use client'

import {
  Card,
  CardContent,
  CardHeader,
} from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Avatar, AvatarFallback, AvatarImage } from '@/components/ui/avatar'
import type { Character } from '@/lib/types/character'

interface CharacterCardProps {
  character: Character
  onClick?: () => void
}

const roleTypeMap: Record<string, { label: string, color: string }> = {
  protagonist: { label: '主角', color: 'bg-amber-500' },
  supporting: { label: '配角', color: 'bg-blue-500' },
  antagonist: { label: '反派', color: 'bg-red-500' },
  minor: { label: '龙套', color: 'bg-gray-500' },
}

export function CharacterCard({ character, onClick }: CharacterCardProps) {
  const roleType = roleTypeMap[character.roleType || 'minor'] || roleTypeMap.minor
  const initials = character.name.slice(0, 2)

  return (
    <Card
      className="cursor-pointer hover:shadow-md transition-shadow"
      onClick={onClick}
    >
      <CardHeader className="pb-3">
        <div className="flex items-center gap-3">
          <Avatar className="h-12 w-12">
            <AvatarImage src={(character as any).avatarUrl} />
            <AvatarFallback
              className="text-white"
              style={{ backgroundColor: (character as any).color || '#4D8088' }}
            >
              {initials}
            </AvatarFallback>
          </Avatar>
          <div className="flex-1 min-w-0">
            <div className="flex items-center gap-2">
              <h3 className="font-medium truncate">{character.name}</h3>
              <Badge
                className={`${roleType.color} text-white text-xs`}
                variant="secondary"
              >
                {roleType.label}
              </Badge>
            </div>
            {(character.age || character.gender) && (
              <p className="text-sm text-muted-foreground">
                {character.age ? `${character.age}岁` : ''} 
                {character.age && character.gender ? ' · ' : ''}
                {character.gender || ''}
              </p>
            )}
          </div>
        </div>
      </CardHeader>

      <CardContent className="pt-0">
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
