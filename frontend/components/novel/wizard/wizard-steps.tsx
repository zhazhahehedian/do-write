'use client'

import { cn } from '@/lib/utils/cn'
import { Check, Loader2 } from 'lucide-react'

export interface WizardStep {
  id: string
  title: string
  description: string
  status: 'pending' | 'current' | 'completed' | 'generating'
}

interface WizardStepsProps {
  steps: WizardStep[]
  currentStep: number
  maxStepIndex?: number
  onStepClick?: (stepId: string, stepIndex: number) => void
}

export function WizardSteps({
  steps,
  currentStep,
  maxStepIndex,
  onStepClick,
}: WizardStepsProps) {
  return (
    <div className="space-y-4">
      {steps.map((step, index) => {
        const isCompleted = step.status === 'completed'
        const isCurrent = step.status === 'current'
        const isGenerating = step.status === 'generating'
        const isSelected = index === currentStep

        const canClick = !!onStepClick
          && maxStepIndex !== undefined
          && index <= maxStepIndex
          && index !== currentStep

        return (
          <div key={step.id}>
            <div
              role={canClick ? 'button' : undefined}
              tabIndex={canClick ? 0 : undefined}
              onClick={() => {
                if (!canClick) return
                onStepClick(step.id, index)
              }}
              onKeyDown={(e) => {
                if (!canClick) return
                if (e.key !== 'Enter' && e.key !== ' ') return
                e.preventDefault()
                onStepClick(step.id, index)
              }}
              className={cn(
                'flex items-start gap-4 p-4 rounded-lg border transition-colors',
                isCurrent && 'border-primary bg-primary/5',
                isCompleted && 'border-green-500/50 bg-green-500/5',
                isGenerating && 'border-primary bg-primary/5 animate-pulse',
                isSelected && 'ring-2 ring-primary/40',
                canClick && 'cursor-pointer hover:border-primary/60'
              )}
              aria-label={canClick ? `回看：${step.title}` : undefined}
            >
            {/* 步骤指示器 */}
            <div
              className={cn(
                'flex items-center justify-center w-8 h-8 rounded-full border-2 shrink-0',
                isCompleted && 'border-green-500 bg-green-500 text-white',
                isCurrent && 'border-primary text-primary',
                isGenerating && 'border-primary text-primary',
                !isCompleted && !isCurrent && !isGenerating && 'border-muted-foreground/30'
              )}
            >
              {isCompleted ? (
                <Check className="h-4 w-4" />
              ) : isGenerating ? (
                <Loader2 className="h-4 w-4 animate-spin" />
              ) : (
                <span className="text-sm font-medium">{index + 1}</span>
              )}
            </div>

            {/* 步骤信息 */}
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2">
                <h3
                  className={cn(
                    'font-medium',
                    (isCurrent || isGenerating) && 'text-primary'
                  )}
                >
                  {step.title}
                </h3>
                {isCompleted && !isSelected && (
                  <span className="text-xs text-muted-foreground">
                    可回看
                  </span>
                )}
                {isCompleted && isSelected && (
                  <span className="text-xs text-primary">
                    回看中
                  </span>
                )}
              </div>
              <p className="text-sm text-muted-foreground mt-0.5">
                {step.description}
              </p>
            </div>
            </div>
          </div>
        )
      })}
    </div>
  )
}
