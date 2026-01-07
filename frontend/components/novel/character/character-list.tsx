'use client'

import { CharacterCard } from './character-card'
import type { Character } from '@/lib/types/character'
import { Button } from '@/components/ui/button'
import { Plus, Users } from 'lucide-react'
import { Loader2 } from 'lucide-react'

interface CharacterListProps {
  characters: Character[]
  isLoading?: boolean
  onAdd?: () => void
  onEdit?: (character: Character) => void
}

export function CharacterList({ 
  characters, 
  isLoading, 
  onAdd, 
  onEdit 
}: CharacterListProps) {
  if (isLoading) {
    return (
      <div className="flex h-[400px] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  if (characters.length === 0) {
    return (
      <div className="flex h-[400px] flex-col items-center justify-center gap-4 rounded-lg border border-dashed text-center">
        <div className="rounded-full bg-muted p-4">
          <Users className="h-8 w-8 text-muted-foreground" />
        </div>
        <div>
          <h3 className="text-lg font-medium">暂无角色</h3>
          <p className="text-sm text-muted-foreground">
            创建您的第一个角色，丰富故事世界
          </p>
        </div>
        <Button onClick={onAdd}>
          <Plus className="mr-2 h-4 w-4" />
          创建角色
        </Button>
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center justify-end">
        <Button onClick={onAdd}>
          <Plus className="mr-2 h-4 w-4" />
          创建角色
        </Button>
      </div>

      <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {characters.map((character) => (
          <CharacterCard
            key={character.id}
            character={character}
            onClick={() => onEdit?.(character)}
          />
        ))}
      </div>
    </div>
  )
}
