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
}

export function WizardSteps({ steps, currentStep }: WizardStepsProps) {
  return (
    <div className="space-y-4">
      {steps.map((step, index) => {
        const isCompleted = step.status === 'completed'
        const isCurrent = step.status === 'current'
        const isGenerating = step.status === 'generating'

        return (
          <div
            key={step.id}
            className={cn(
              'flex items-start gap-4 p-4 rounded-lg border transition-colors',
              isCurrent && 'border-primary bg-primary/5',
              isCompleted && 'border-green-500/50 bg-green-500/5',
              isGenerating && 'border-primary bg-primary/5 animate-pulse'
            )}
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
              <h3
                className={cn(
                  'font-medium',
                  (isCurrent || isGenerating) && 'text-primary'
                )}
              >
                {step.title}
              </h3>
              <p className="text-sm text-muted-foreground mt-0.5">
                {step.description}
              </p>
            </div>
          </div>
        )
      })}
    </div>
  )
}
