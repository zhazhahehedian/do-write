'use client'

import { useState, useEffect } from 'react'
import { useParams, useRouter, useSearchParams } from 'next/navigation'
import { WizardSteps, type WizardStep } from '@/components/novel/wizard/wizard-steps'
import { WorldGenerator } from '@/components/novel/wizard/world-generator'
import { Button } from '@/components/ui/button'
import { ArrowLeft } from 'lucide-react'
import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { projectApi } from '@/lib/api/project'
import { wizardApi } from '@/lib/api/wizard'
import { toast } from 'sonner'

const steps: WizardStep[] = [
  {
    id: 'world',
    title: '世界观设定',
    description: '生成时代背景、地点环境、世界规则等',
    status: 'current',
  },
  {
    id: 'characters',
    title: '角色设计',
    description: '生成主角、配角、反派等核心角色',
    status: 'pending',
  },
  {
    id: 'outlines',
    title: '大纲规划',
    description: '生成故事大纲、章节规划',
    status: 'pending',
  },
]

export default function WizardPage() {
  const params = useParams()
  const router = useRouter()
  const searchParams = useSearchParams()
  const projectId = params.projectId as string
  const stepParam = searchParams.get('step')

  // 根据 URL 参数或项目状态确定当前步骤
  const [currentStepIndex, setCurrentStepIndex] = useState(0)

  // 获取项目信息
  const { data: project } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.getById(projectId),
  })

  // 同步步骤状态
  useEffect(() => {
    if (stepParam) {
      const index = steps.findIndex(s => s.id === stepParam)
      if (index !== -1) setCurrentStepIndex(index)
    }
  }, [stepParam])

  const handleStepComplete = async (stepId: string, data: any) => {
    try {
      // 1. 保存当前步骤数据 (已经在各 Generator 组件中完成)
      
      // 2. 更新向导状态
      await wizardApi.updateStatus(projectId, 'IN_PROGRESS', currentStepIndex + 1)
      
      // 3. 进入下一步
      if (currentStepIndex < steps.length - 1) {
        const nextStep = steps[currentStepIndex + 1]
        router.push(`/project/${projectId}/wizard?step=${nextStep.id}`)
      } else {
        // 完成所有步骤，跳转到项目详情
        toast.success('创作向导完成！')
        router.push(`/project/${projectId}`)
      }
    } catch (error) {
      toast.error('保存状态失败')
    }
  }

  // 计算步骤显示状态
  const displaySteps = steps.map((step, index) => ({
    ...step,
    status: index < currentStepIndex ? 'completed' : index === currentStepIndex ? 'current' : 'pending',
  })) as WizardStep[]

  return (
    <div className="container max-w-5xl mx-auto py-8 space-y-8">
      {/* 顶部导航 */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link href={`/project/${projectId}`}>
            <ArrowLeft className="h-4 w-4" />
          </Link>
        </Button>
        <div>
          <h1 className="text-2xl font-bold">创作向导</h1>
          <p className="text-muted-foreground text-sm">
            {project?.title || '未命名项目'}
          </p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-4 gap-8">
        {/* 左侧步骤条 */}
        <div className="md:col-span-1">
          <WizardSteps steps={displaySteps} currentStep={currentStepIndex} />
        </div>

        {/* 右侧内容区 */}
        <div className="md:col-span-3">
            {steps[currentStepIndex].id === 'world' && (
                <WorldGenerator 
                    projectId={projectId} 
                    onComplete={(data) => handleStepComplete('world', data)}
                />
            )}
            {steps[currentStepIndex].id === 'characters' && (
                <div className="text-center py-12 text-muted-foreground border rounded-lg">
                    角色生成器开发中... <br/>
                    <Button 
                        variant="outline" 
                        className="mt-4"
                        onClick={() => handleStepComplete('characters', {})}
                    >
                        跳过 (模拟完成)
                    </Button>
                </div>
            )}
             {steps[currentStepIndex].id === 'outlines' && (
                <div className="text-center py-12 text-muted-foreground border rounded-lg">
                    大纲生成器开发中... <br/>
                     <Button 
                        variant="outline" 
                        className="mt-4"
                        onClick={() => handleStepComplete('outlines', {})}
                    >
                        跳过 (模拟完成)
                    </Button>
                </div>
            )}
        </div>
      </div>
    </div>
  )
}
