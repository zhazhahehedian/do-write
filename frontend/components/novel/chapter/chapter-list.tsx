'use client'

import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  MoreHorizontal,
  RefreshCw,
  Eye,
  Pencil,
  Trash2,
  Sparkles,
} from 'lucide-react'
import Link from 'next/link'
import type { Chapter } from '@/lib/types/chapter'

interface ChapterListProps {
  chapters: Chapter[]
  projectId: string
  onGenerate?: (chapterId: string) => void
  onRegenerate?: (chapterId: string) => void
  onDelete?: (chapterId: string) => void
}

const statusMap = {
  pending: { label: '待生成', variant: 'secondary' as const },
  generating: { label: '生成中', variant: 'default' as const },
  completed: { label: '已完成', variant: 'outline' as const },
  failed: { label: '失败', variant: 'destructive' as const },
}

export function ChapterList({
  chapters,
  projectId,
  onGenerate,
  onRegenerate,
  onDelete,
}: ChapterListProps) {
  return (
    <div className="rounded-md border">
      <Table>
        <TableHeader>
          <TableRow>
            <TableHead className="w-[80px]">章节</TableHead>
            <TableHead>标题</TableHead>
            <TableHead className="w-[100px]">字数</TableHead>
            <TableHead className="w-[100px]">状态</TableHead>
            <TableHead className="w-[150px]">更新时间</TableHead>
            <TableHead className="w-[100px] text-right">操作</TableHead>
          </TableRow>
        </TableHeader>
        <TableBody>
          {chapters.map((chapter) => {
            const status = statusMap[chapter.generationStatus] || statusMap.pending
            const isGenerating = chapter.generationStatus === 'generating'

            return (
              <TableRow key={chapter.id}>
                <TableCell className="font-medium">
                  第{chapter.chapterNumber}章
                  {chapter.subIndex > 0 && `-${chapter.subIndex}`}
                </TableCell>
                <TableCell>
                  <Link
                    href={`/project/${projectId}/chapters/${chapter.id}`}
                    className="hover:text-primary transition-colors"
                  >
                    {chapter.title || '未命名章节'}
                  </Link>
                </TableCell>
                <TableCell className="text-muted-foreground">
                  {chapter.wordCount?.toLocaleString() || 0}
                </TableCell>
                <TableCell>
                  <Badge variant={status.variant}>
                    {isGenerating && (
                      <span className="mr-1 h-2 w-2 rounded-full bg-primary animate-pulse" />
                    )}
                    {status.label}
                  </Badge>
                </TableCell>
                <TableCell className="text-muted-foreground text-sm">
                  {new Date(chapter.updateTime).toLocaleString()}
                </TableCell>
                <TableCell className="text-right">
                  <DropdownMenu>
                    <DropdownMenuTrigger asChild>
                      <Button variant="ghost" size="icon">
                        <MoreHorizontal className="h-4 w-4" />
                      </Button>
                    </DropdownMenuTrigger>
                    <DropdownMenuContent align="end">
                      {chapter.generationStatus === 'pending' && (
                        <DropdownMenuItem onClick={() => onGenerate?.(chapter.id.toString())}>
                          <Sparkles className="mr-2 h-4 w-4" />
                          生成章节
                        </DropdownMenuItem>
                      )}
                      {chapter.generationStatus === 'completed' && (
                        <>
                          <DropdownMenuItem asChild>
                            <Link href={`/project/${projectId}/chapters/${chapter.id}`}>
                              <Eye className="mr-2 h-4 w-4" />
                              阅读
                            </Link>
                          </DropdownMenuItem>
                          <DropdownMenuItem asChild>
                            <Link href={`/project/${projectId}/chapters/${chapter.id}/edit`}>
                              <Pencil className="mr-2 h-4 w-4" />
                              编辑
                            </Link>
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => onRegenerate?.(chapter.id.toString())}>
                            <RefreshCw className="mr-2 h-4 w-4" />
                            重新生成
                          </DropdownMenuItem>
                        </>
                      )}
                      <DropdownMenuItem
                        className="text-destructive"
                        onClick={() => onDelete?.(chapter.id.toString())}
                      >
                        <Trash2 className="mr-2 h-4 w-4" />
                        删除
                      </DropdownMenuItem>
                    </DropdownMenuContent>
                  </DropdownMenu>
                </TableCell>
              </TableRow>
            )
          })}

          {chapters.length === 0 && (
            <TableRow>
              <TableCell colSpan={6} className="h-24 text-center">
                <div className="text-muted-foreground">
                  暂无章节，请先生成大纲
                </div>
              </TableCell>
            </TableRow>
          )}
        </TableBody>
      </Table>
    </div>
  )
}

