'use client'

import { useEffect, useMemo, useRef, useState } from 'react'
import { cn } from '@/lib/utils/cn'

interface StreamingTextProps {
  content: string
  isStreaming: boolean
  mode?: 'direct' | 'typewriter'
  className?: string
}

export function StreamingText({
  content,
  isStreaming,
  mode = 'direct',
  className,
}: StreamingTextProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const rafRef = useRef<number | null>(null)
  const renderedLengthRef = useRef(0)
  const [renderedLength, setRenderedLength] = useState(0)

  useEffect(() => {
    if (mode !== 'typewriter') return

    const tick = () => {
      const target = content.length
      const current = renderedLengthRef.current
      if (current === target) {
        rafRef.current = null
        return
      }

      setRenderedLength((prev) => {
        if (prev > target) {
          renderedLengthRef.current = target
          return target
        }

        const remaining = target - prev
        const step = Math.min(Math.max(Math.ceil(remaining / 60), 1), 80)
        const next = Math.min(prev + step, target)
        renderedLengthRef.current = next
        return next
      })

      if (renderedLengthRef.current !== target) {
        rafRef.current = requestAnimationFrame(tick)
      } else {
        rafRef.current = null
      }
    }

    rafRef.current = requestAnimationFrame(tick)
    return () => {
      if (rafRef.current !== null) {
        cancelAnimationFrame(rafRef.current)
        rafRef.current = null
      }
    }
  }, [content, mode])

  const displayContent = useMemo(() => {
    if (mode !== 'typewriter') return content
    return content.slice(0, renderedLength)
  }, [content, mode, renderedLength])

  const isTyping = mode === 'typewriter' && renderedLength < content.length

  // 自动滚动到底部
  useEffect(() => {
    if (containerRef.current && isStreaming) {
      containerRef.current.scrollTop = containerRef.current.scrollHeight
    }
  }, [isStreaming, displayContent])

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
        {displayContent}
        {(isStreaming || isTyping) && (
          <span className="inline-block w-2 h-4 ml-1 bg-primary animate-pulse" />
        )}
      </div>
    </div>
  )
}
