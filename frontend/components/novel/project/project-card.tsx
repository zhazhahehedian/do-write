'use client'

import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Progress } from '@/components/ui/progress'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import {
  MoreHorizontal,
  BookOpen,
  Users,
  FileText,
  Sparkles,
  Trash2,
} from 'lucide-react'
import Link from 'next/link'
import type { Project } from '@/lib/types/project'

interface ProjectCardProps {
  project: Project
  onDelete?: (id: string) => void
}

const statusMap = {
  planning: { label: '规划中', variant: 'secondary' as const },
  writing: { label: '创作中', variant: 'default' as const },
  completed: { label: '已完成', variant: 'outline' as const },
}

export function ProjectCard({ project, onDelete }: ProjectCardProps) {
  const status = statusMap[project.status] || statusMap.planning
  const progress = project.targetWords > 0
    ? Math.round((project.currentWords / project.targetWords) * 100)
    : 0

  return (
    <Card className="group hover:shadow-md transition-shadow">
      <CardHeader className="pb-3">
        <div className="flex items-start justify-between">
          <div className="space-y-1">
            <CardTitle className="text-lg">
              <Link
                href={`/project/${project.id}`}
                className="hover:text-primary transition-colors"
              >
                {project.title}
              </Link>
            </CardTitle>
            <CardDescription className="line-clamp-2">
              {project.description || '暂无描述'}
            </CardDescription>
          </div>
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                size="icon"
                className="opacity-0 group-hover:opacity-100 transition-opacity"
              >
                <MoreHorizontal className="h-4 w-4" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent align="end">
              <DropdownMenuItem asChild>
                <Link href={`/project/${project.id}/wizard`}>
                  <Sparkles className="mr-2 h-4 w-4" />
                  创作向导
                </Link>
              </DropdownMenuItem>
              <DropdownMenuItem
                className="text-destructive"
                onClick={() => onDelete?.(project.id.toString())}
              >
                <Trash2 className="mr-2 h-4 w-4" />
                删除项目
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </CardHeader>

      <CardContent className="pb-3">
        {/* 标签 */}
        <div className="flex flex-wrap gap-2 mb-4">
          <Badge variant={status.variant}>{status.label}</Badge>
          {project.genre && (
            <Badge variant="outline">{project.genre}</Badge>
          )}
          {project.theme && (
            <Badge variant="outline">{project.theme}</Badge>
          )}
        </div>

        {/* 统计信息 */}
        <div className="grid grid-cols-3 gap-4 text-sm">
          <div className="flex items-center gap-1.5 text-muted-foreground">
            <FileText className="h-4 w-4" />
            <span>{project.chapterCount || 0} 章</span>
          </div>
          <div className="flex items-center gap-1.5 text-muted-foreground">
            <Users className="h-4 w-4" />
            <span>{project.characterCount || 0} 角色</span>
          </div>
          <div className="flex items-center gap-1.5 text-muted-foreground">
            <BookOpen className="h-4 w-4" />
            <span>{(project.currentWords / 10000).toFixed(1)}万字</span>
          </div>
        </div>

        {/* 进度条 */}
        {project.targetWords > 0 && (
          <div className="mt-4 space-y-1.5">
            <div className="flex justify-between text-xs text-muted-foreground">
              <span>写作进度</span>
              <span>{progress}%</span>
            </div>
            <Progress value={progress} className="h-1.5" />
          </div>
        )}
      </CardContent>

      <CardFooter className="pt-3 border-t">
        <div className="flex items-center justify-between w-full text-xs text-muted-foreground">
          <span>更新于 {new Date(project.updateTime).toLocaleDateString()}</span>
          <Link href={`/project/${project.id}/chapters`}>
            <Button variant="ghost" size="sm">
              继续阅读
            </Button>
          </Link>
        </div>
      </CardFooter>
    </Card>
  )
}
