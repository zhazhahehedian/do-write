'use client'

import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { usePathname, useParams } from 'next/navigation'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import {
  ArrowLeft,
  Globe,
  Users,
  List,
  FileText,
  Sparkles,
  Loader2,
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { projectApi } from '@/lib/api/project'

const projectTabs = [
  { value: 'world', label: '世界观', icon: Globe, href: 'world' },
  { value: 'characters', label: '角色', icon: Users, href: 'characters' },
  { value: 'outlines', label: '大纲', icon: List, href: 'outlines' },
  { value: 'chapters', label: '章节', icon: FileText, href: 'chapters' },
]

export default function ProjectLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const params = useParams()
  const projectId = params.projectId as string
  const pathname = usePathname()

  const { data: project, isLoading } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.getById(projectId),
    enabled: !!projectId,
  })

  const currentTab = projectTabs.find(
    tab => pathname.includes(`/project/${projectId}/${tab.href}`)
  )?.value || 'world'

  return (
    <div className="space-y-6">
      {/* 项目标题区域 */}
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" asChild>
            <Link href="/home">
              <ArrowLeft className="h-4 w-4" />
            </Link>
          </Button>
          <div>
            {isLoading ? (
              <div className="flex items-center gap-2">
                <Loader2 className="h-4 w-4 animate-spin" />
                <span className="text-muted-foreground">加载中...</span>
              </div>
            ) : (
              <>
                <h1 className="text-2xl font-bold">{project?.title || '未命名项目'}</h1>
                <p className="text-muted-foreground">
                  {[project?.genre, project?.theme].filter(Boolean).join(' · ') || '暂无分类'}
                </p>
              </>
            )}
          </div>
        </div>
        <Link href={`/project/wizard/${projectId}`}>
          <Button className="bg-gradient-to-r from-primary to-accent text-white hover:opacity-90">
            <Sparkles className="mr-2 h-4 w-4" />
            继续创作
          </Button>
        </Link>
      </div>

      {/* Tab 导航 */}
      <Tabs value={currentTab} className="w-full">
        <TabsList>
          {projectTabs.map((tab) => (
            <TabsTrigger key={tab.value} value={tab.value} asChild>
              <Link href={`/project/${projectId}/${tab.href}`}>
                <tab.icon className="mr-2 h-4 w-4" />
                {tab.label}
              </Link>
            </TabsTrigger>
          ))}
        </TabsList>
      </Tabs>

      {/* 内容区域 */}
      {children}
    </div>
  )
}
