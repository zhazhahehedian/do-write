'use client'

import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { usePathname, useParams } from 'next/navigation'
import Link from 'next/link'
import {
  Globe,
  Users,
  List,
  FileText,
  Sparkles,
} from 'lucide-react'
import { Button } from '@/components/ui/button'

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

  const currentTab = projectTabs.find(
    tab => pathname.includes(`/project/${projectId}/${tab.href}`)
  )?.value || 'world'

  return (
    <div className="space-y-6">
      {/* 项目标题区域 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">项目名称</h1>
          <p className="text-muted-foreground">玄幻 · 都市</p>
        </div>
        <Link href={`/project/${projectId}/wizard`}>
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
