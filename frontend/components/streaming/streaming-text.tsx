'use client'

import { useEffect, useRef } from 'react'
import { cn } from '@/lib/utils/cn'

interface StreamingTextProps {
  content: string
  isStreaming: boolean
  className?: string
}

export function StreamingText({
  content,
  isStreaming,
  className,
}: StreamingTextProps) {
  const containerRef = useRef<HTMLDivElement>(null)

  // 自动滚动到底部
  useEffect(() => {
    if (containerRef.current && isStreaming) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight
    }
  }, [content, isStreaming])

  return (
    <div
      ref={containerRef}
      className={cn(
        'prose prose-sm max-w-none overflow-auto',
        'dark:prose-invert',
        className
      )}
    >
      <div className="whitespace-pre-wrap">
        {content}
        {isStreaming && (
          <span className="inline-block w-2 h-4 ml-1 bg-primary animate-pulse" />
        )}
      </div>
    </div>
  )
}
