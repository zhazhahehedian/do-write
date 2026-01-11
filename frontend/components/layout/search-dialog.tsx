'use client'

import { useEffect, useMemo, useState } from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Search } from 'lucide-react'
import { useQuery } from '@tanstack/react-query'
import { searchApi } from '@/lib/api/search'
import { useRouter } from 'next/navigation'

function useDebouncedValue<T>(value: T, delayMs: number) {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    return () => clearTimeout(timer)
  }, [value, delayMs])

  return debounced
}

interface SearchDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
}

export function SearchDialog({ open, onOpenChange }: SearchDialogProps) {
  const router = useRouter()
  const [keyword, setKeyword] = useState('')
  const debouncedKeyword = useDebouncedValue(keyword, 250)

  const normalizedKeyword = useMemo(() => debouncedKeyword.trim(), [debouncedKeyword])

  const { data, isFetching } = useQuery({
    queryKey: ['global-search', normalizedKeyword],
    queryFn: () => searchApi.search(normalizedKeyword, 5),
    enabled: open && normalizedKeyword.length > 0,
  })

  const hasAnyResult = useMemo(() => {
    if (!data) return false
    return (
      (data.projects?.length || 0) +
        (data.chapters?.length || 0) +
        (data.outlines?.length || 0) +
        (data.characters?.length || 0) +
        (data.memories?.length || 0) >
      0
    )
  }, [data])

  const goto = (url: string) => {
    onOpenChange(false)
    setKeyword('')
    router.push(url)
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[550px]">
        <DialogHeader>
          <DialogTitle>搜索</DialogTitle>
        </DialogHeader>
        <div className="flex items-center border-b px-3" cmdk-input-wrapper="">
          <Search className="mr-2 h-4 w-4 shrink-0 opacity-50" />
          <Input
            placeholder="搜索项目、章节或角色..."
            className="flex h-11 w-full rounded-md bg-transparent py-3 text-sm outline-none placeholder:text-muted-foreground disabled:cursor-not-allowed disabled:opacity-50 border-none focus-visible:ring-0"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
          />
        </div>
        <div className="max-h-[420px] overflow-auto py-3">
          {!normalizedKeyword && (
            <div className="py-6 text-center text-sm text-muted-foreground">
              输入关键词开始搜索
            </div>
          )}

          {normalizedKeyword && isFetching && (
            <div className="py-6 text-center text-sm text-muted-foreground">
              正在搜索...
            </div>
          )}

          {normalizedKeyword && !isFetching && !hasAnyResult && (
            <div className="py-6 text-center text-sm text-muted-foreground">
              暂无匹配结果
            </div>
          )}

          {!isFetching && hasAnyResult && (
            <div className="space-y-4 text-sm">
              {!!data?.projects?.length && (
                <div>
                  <div className="mb-2 text-xs font-medium text-muted-foreground">项目</div>
                  <div className="space-y-1">
                    {data.projects.map((p) => (
                      <button
                        key={p.id}
                        className="w-full rounded-md px-2 py-2 text-left hover:bg-muted"
                        onClick={() => goto(`/project/${p.id}/world`)}
                      >
                        <div className="font-medium">{p.title}</div>
                        {p.description && (
                          <div className="mt-0.5 line-clamp-1 text-xs text-muted-foreground">
                            {p.description}
                          </div>
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {!!data?.chapters?.length && (
                <div>
                  <div className="mb-2 text-xs font-medium text-muted-foreground">章节</div>
                  <div className="space-y-1">
                    {data.chapters.map((c) => (
                      <button
                        key={c.id}
                        className="w-full rounded-md px-2 py-2 text-left hover:bg-muted"
                        onClick={() => goto(`/project/${c.projectId}/chapters/${c.id}`)}
                      >
                        <div className="font-medium">
                          {c.chapterNumber ? `第${c.chapterNumber}章：` : ''}{c.title || '未命名章节'}
                        </div>
                        {c.summary && (
                          <div className="mt-0.5 line-clamp-1 text-xs text-muted-foreground">
                            {c.summary}
                          </div>
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {!!data?.outlines?.length && (
                <div>
                  <div className="mb-2 text-xs font-medium text-muted-foreground">大纲</div>
                  <div className="space-y-1">
                    {data.outlines.map((o) => (
                      <button
                        key={o.id}
                        className="w-full rounded-md px-2 py-2 text-left hover:bg-muted"
                        onClick={() => goto(`/project/${o.projectId}/outlines`)}
                      >
                        <div className="font-medium">
                          {o.orderIndex ? `第${o.orderIndex}章：` : ''}{o.title || '未命名大纲'}
                        </div>
                        {o.content && (
                          <div className="mt-0.5 line-clamp-1 text-xs text-muted-foreground">
                            {o.content}
                          </div>
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {!!data?.characters?.length && (
                <div>
                  <div className="mb-2 text-xs font-medium text-muted-foreground">角色</div>
                  <div className="space-y-1">
                    {data.characters.map((c) => (
                      <button
                        key={c.id}
                        className="w-full rounded-md px-2 py-2 text-left hover:bg-muted"
                        onClick={() => goto(`/project/${c.projectId}/characters`)}
                      >
                        <div className="font-medium">
                          {c.isOrganization ? '组织：' : '角色：'}{c.name}
                        </div>
                        {c.roleType && (
                          <div className="mt-0.5 text-xs text-muted-foreground">
                            {c.roleType}
                          </div>
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              {!!data?.memories?.length && (
                <div>
                  <div className="mb-2 text-xs font-medium text-muted-foreground">记忆</div>
                  <div className="space-y-1">
                    {data.memories.map((m) => (
                      <button
                        key={m.id}
                        className="w-full rounded-md px-2 py-2 text-left hover:bg-muted"
                        onClick={() => goto(`/project/${m.projectId}/chapters/${m.chapterId}`)}
                      >
                        <div className="font-medium">{m.title}</div>
                        {m.content && (
                          <div className="mt-0.5 line-clamp-1 text-xs text-muted-foreground">
                            {m.content}
                          </div>
                        )}
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  )
}
