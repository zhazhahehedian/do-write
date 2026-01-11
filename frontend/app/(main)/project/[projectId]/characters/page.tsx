'use client'

import { useState } from 'react'
import { useParams } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { CharacterList } from '@/components/novel/character/character-list'
import { CharacterFormDialog } from '@/components/novel/character/character-form-dialog'
import { characterApi } from '@/lib/api/character'
import { toast } from 'sonner'
import type { Character } from '@/lib/types/character'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Loader2 } from 'lucide-react'

export default function CharactersPage() {
  const params = useParams()
  const projectId = params.projectId as string
  const queryClient = useQueryClient()

  const [formDialogOpen, setFormDialogOpen] = useState(false)
  const [editingCharacter, setEditingCharacter] = useState<Character | null>(null)
  const [pendingDeleteId, setPendingDeleteId] = useState<string | null>(null)

  const { data, isLoading } = useQuery({
    queryKey: ['characters', projectId],
    queryFn: () => characterApi.list(projectId, { pageNum: 1, pageSize: 100 }),
  })

  const deleteMutation = useMutation({
    mutationFn: characterApi.delete,
    onSuccess: () => {
      toast.success('角色已删除')
      setPendingDeleteId(null)
      queryClient.invalidateQueries({ queryKey: ['characters', projectId] })
    },
    onError: (error: any) => {
      toast.error(error.message || '删除失败')
    },
  })

  const handleAdd = () => {
    setEditingCharacter(null)
    setFormDialogOpen(true)
  }

  const handleEdit = (character: Character) => {
    setEditingCharacter(character)
    setFormDialogOpen(true)
  }

  const characters = data?.list || []
  const pendingDeleteCharacter = pendingDeleteId
    ? characters.find((c) => c.id?.toString() === pendingDeleteId)
    : undefined

  return (
    <>
      <CharacterList
        characters={characters}
        isLoading={isLoading}
        onAdd={handleAdd}
        onEdit={handleEdit}
        onDelete={(id) => setPendingDeleteId(id)}
      />

      <CharacterFormDialog
        open={formDialogOpen}
        onOpenChange={setFormDialogOpen}
        projectId={projectId}
        character={editingCharacter}
      />

      <AlertDialog
        open={pendingDeleteId !== null}
        onOpenChange={(open) => {
          if (deleteMutation.isPending) return
          if (!open) setPendingDeleteId(null)
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除角色？</AlertDialogTitle>
            <AlertDialogDescription>
              {pendingDeleteCharacter
                ? `删除后将无法恢复，且会永久删除「${pendingDeleteCharacter.name}」。`
                : '删除后将无法恢复。'}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteMutation.isPending}>取消</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              disabled={deleteMutation.isPending}
              onClick={(event) => {
                event.preventDefault()
                if (!pendingDeleteId) return
                deleteMutation.mutate(pendingDeleteId)
              }}
            >
              {deleteMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}