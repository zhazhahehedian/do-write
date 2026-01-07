'use client'

import { useParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { OutlineList } from '@/components/novel/outline/outline-list'
import { outlineApi } from '@/lib/api/outline'
import { toast } from 'sonner'

export default function OutlinesPage() {
  const params = useParams()
  const projectId = params.projectId as string

  const { data, isLoading } = useQuery({
    queryKey: ['outlines', projectId],
    queryFn: () => outlineApi.list(projectId, { pageNum: 1, pageSize: 100 }),
  })

  const handleAdd = () => {
    toast.info('创建大纲功能开发中')
  }

  const handleEdit = (outline: any) => {
    toast.info(`编辑大纲: ${outline.title}`)
  }

  return (
    <OutlineList
      outlines={data?.list || []}
      isLoading={isLoading}
      onAdd={handleAdd}
      onEdit={handleEdit}
    />
  )
}