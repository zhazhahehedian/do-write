'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { CharacterList } from '@/components/novel/character/character-list'
import { characterApi } from '@/lib/api/character'
import { toast } from 'sonner'

export default function CharactersPage() {
  const params = useParams()
  const projectId = params.projectId as string

  const { data, isLoading } = useQuery({
    queryKey: ['characters', projectId],
    queryFn: () => characterApi.list(projectId, { pageNum: 1, pageSize: 100 }),
  })

  const handleAdd = () => {
    toast.info('创建角色功能开发中')
  }

  const handleEdit = (character: any) => {
    toast.info(`编辑角色: ${character.name}`)
  }

  return (
    <CharacterList
      characters={data?.list || []}
      isLoading={isLoading}
      onAdd={handleAdd}
      onEdit={handleEdit}
    />
  )
}