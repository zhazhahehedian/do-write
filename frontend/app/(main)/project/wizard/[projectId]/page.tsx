'use client'

import { useState, useEffect, useRef } from 'react'
import { useParams, useRouter, useSearchParams } from 'next/navigation'
import { useQuery } from '@tanstack/react-query'
import { WizardSteps, type WizardStep } from '@/components/novel/wizard/wizard-steps'
import { WorldGenerator } from '@/components/novel/wizard/world-generator'
import { CharacterGenerator } from '@/components/novel/wizard/character-generator'
import { OutlineGenerator } from '@/components/novel/wizard/outline-generator'
import { Button } from '@/components/ui/button'
import { ArrowLeft, Loader2 } from 'lucide-react'
import Link from 'next/link'
import { projectApi } from '@/lib/api/project'
import { wizardApi } from '@/lib/api/wizard'
import { useApiConfigCheck } from '@/hooks/use-api-config-check'
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

  const { hasDefaultConfig, isLoading: configLoading } = useApiConfigCheck()
  const [currentStepIndex, setCurrentStepIndex] = useState(0)

  // 获取项目信息
  const { data: project, isLoading: projectLoading } = useQuery({
    queryKey: ['project', projectId],
    queryFn: () => projectApi.getById(projectId),
    enabled: !!projectId,
  })

  // 获取向导进度
  const { data: progress } = useQuery({
    queryKey: ['wizard-progress', projectId],
    queryFn: () => wizardApi.getProgress(projectId),
    enabled: !!projectId,
  })

  // 检查 API 配置（使用 ref 防止 React Strict Mode 下重复弹出 toast）
  const toastShownRef = useRef(false)
  useEffect(() => {
    if (!configLoading && !hasDefaultConfig && !toastShownRef.current) {
      toastShownRef.current = true
      toast.error('请先配置 AI 模型')
      router.push('/settings/api-config')
    }
  }, [configLoading, hasDefaultConfig, router])

  // 根据后端进度同步步骤
  useEffect(() => {
    if (progress) {
      // 如果向导已完成，跳转到章节页面
      if (progress.status === 'COMPLETED' || progress.currentStep >= 3) {
        router.push(`/project/${projectId}/chapters`)
        return
      }
      // 根据后端返回的 wizardStep 设置当前步骤
      setCurrentStepIndex(progress.currentStep)
    }
  }, [progress, projectId, router])

  // 根据 URL 参数同步步骤
  useEffect(() => {
    if (stepParam) {
      const index = steps.findIndex(s => s.id === stepParam)
      if (index !== -1) setCurrentStepIndex(index)
    }
  }, [stepParam])

  const handleStepComplete = async (_stepId: string, _data: unknown) => {
    try {
      // 更新向导状态
      await wizardApi.updateStatus(projectId, 'IN_PROGRESS', currentStepIndex + 1)

      // 进入下一步
      if (currentStepIndex < steps.length - 1) {
        const nextStep = steps[currentStepIndex + 1]
        setCurrentStepIndex(currentStepIndex + 1)
        router.push(`/project/wizard/${projectId}?step=${nextStep.id}`)
      } else {
        // 完成所有步骤，跳转到章节页面
        toast.success('创作向导完成!')
        router.push(`/project/${projectId}/chapters`)
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

  if (configLoading || projectLoading) {
    return (
      <div className="flex h-[400px] items-center justify-center">
        <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
      </div>
    )
  }

  return (
    <div className="container max-w-5xl mx-auto py-8 space-y-8">
      {/* 顶部导航 */}
      <div className="flex items-center gap-4">
        <Button variant="ghost" size="icon" asChild>
          <Link href="/projects">
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
              initialData={project ? {
                timePeriod: project.worldTimePeriod,
                location: project.worldLocation,
                atmosphere: project.worldAtmosphere,
                rules: project.worldRules,
              } : undefined}
              onComplete={(data) => handleStepComplete('world', data)}
            />
          )}

          {steps[currentStepIndex].id === 'characters' && (
            <CharacterGenerator
              projectId={projectId}
              onComplete={(data) => handleStepComplete('characters', data)}
            />
          )}

          {steps[currentStepIndex].id === 'outlines' && (
            <OutlineGenerator
              projectId={projectId}
              onComplete={(data) => handleStepComplete('outlines', data)}
            />
          )}
        </div>
      </div>
    </div>
  )
}
