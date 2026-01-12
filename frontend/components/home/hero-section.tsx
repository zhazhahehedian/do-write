'use client'

import * as React from "react"
import Link from "next/link"
import { motion } from "motion/react"
import { ArrowRight, PenTool, Sparkles, BookOpen, Brain, Zap } from "lucide-react"
import { cn } from "@/lib/utils"
import { Button } from "@/components/ui/button"

export interface HeroSectionProps {
  className?: string;
}

// 逐字显示动画组件（单次播放，用于段落）
const StreamingText = React.memo(function StreamingText({
  text,
  speed = 40,
  delay = 0,
  className
}: {
  text: string;
  speed?: number;
  delay?: number;
  className?: string;
}) {
  const [displayText, setDisplayText] = React.useState('')
  const [started, setStarted] = React.useState(false)

  React.useEffect(() => {
    const delayTimer = setTimeout(() => setStarted(true), delay)
    return () => clearTimeout(delayTimer)
  }, [delay])

  React.useEffect(() => {
    if (!started) return

    let currentIndex = 0;
    const interval = setInterval(() => {
      if (currentIndex < text.length) {
        setDisplayText(text.substring(0, currentIndex + 1))
        currentIndex++
      } else {
        clearInterval(interval)
      }
    }, speed)

    return () => clearInterval(interval)
  }, [text, speed, started])

  return (
    <span className={className}>
      {displayText}
      {started && displayText.length < text.length && (
        <span className="inline-block w-0.5 h-3 bg-primary/60 ml-0.5 animate-pulse" />
      )}
    </span>
  )
})

const NarrativeLoop = () => {
  const [key, setKey] = React.useState(0)

  // 时间计算:
  // 第一段: 500ms delay + 42字 × 45ms ≈ 2400ms (2.4秒完成)
  // AI续写提示: 2800ms 出现 (第一段完成后0.4秒)
  // 第二段: 3300ms delay + 47字 × 40ms ≈ 5200ms (5.2秒完成)
  // 建议卡片: 5500ms 出现
  // 建议内容: 6000ms delay + 31字 × 35ms ≈ 7100ms (7.1秒完成)
  // 停留展示: 3秒
  // 总循环: 约10秒
  React.useEffect(() => {
    const timer = setTimeout(() => {
      setKey(prev => prev + 1)
    }, 10000)
    return () => clearTimeout(timer)
  }, [key])

  return (
    <div key={key} className="flex flex-col h-full">
      <div className="p-4 border-b border-white/10 flex items-center gap-2 bg-muted/30">
        <div className="flex gap-1.5">
          <div className="w-3 h-3 rounded-full bg-red-500/50" />
          <div className="w-3 h-3 rounded-full bg-yellow-500/50" />
          <div className="w-3 h-3 rounded-full bg-green-500/50" />
        </div>
        <div className="ml-4 text-xs text-muted-foreground font-mono">
          chapter-1.txt
        </div>
      </div>
      <div className="p-6 font-serif text-muted-foreground/80 leading-relaxed text-sm space-y-4 flex-1">
        <p>
          <StreamingText
            text="夜色如墨，霓虹灯在雨后的街道上投下斑驳的光影。李明站在巨大的落地窗前，俯瞰着这座不夜城。"
            speed={45}
            delay={300}
          />
        </p>

        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 2.5, duration: 0.3 }}
        >
          <span className="text-primary font-medium flex items-center gap-1 mb-2">
            <Sparkles className="w-3 h-3 animate-pulse" />
            AI 续写中...
          </span>
          <StreamingText
            text="他手中的红酒杯轻轻摇晃，映照出他深邃的眼眸。这一刻，他等待了整整十年。系统的倒计时在他视网膜上跳动——"
            speed={40}
            delay={2800}
          />
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 10 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 5.5, duration: 0.4 }}
          className="p-3 bg-primary/5 rounded-lg border border-primary/10 mt-4"
        >
          <div className="flex items-center gap-2 text-xs text-primary mb-1">
            <Sparkles className="w-3 h-3" />
            <span>建议走向</span>
          </div>
          <p className="text-xs">
            <StreamingText
              text="突发事件打破寂静，神秘访客敲响房门，带来关于「源计划」的惊人秘密。"
              speed={35}
              delay={5900}
            />
          </p>
        </motion.div>
      </div>
    </div>
  )
}

export const HeroSection = React.memo(function HeroSection({ className }: HeroSectionProps) {
  return (
    <section className={cn("w-full relative z-10 flex-1", className)}>
      <div className="container mx-auto max-w-7xl grid lg:grid-cols-2 gap-12 lg:gap-20 items-center min-h-0 h-full px-6">

        <div className="max-w-3xl pt-20 lg:pt-0">
          <motion.h1
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: 0.1, ease: [0.16, 1, 0.3, 1] }}
            className="text-4xl md:text-5xl lg:text-6xl font-extrabold tracking-tight leading-[1.1] mb-6 text-foreground"
          >
            Do Write <br />
            <span className="bg-clip-text text-transparent bg-gradient-to-r from-primary to-purple-500">
              AI 驱动的沉浸式创作
            </span>
          </motion.h1>

          <motion.p
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: 0.2, ease: [0.16, 1, 0.3, 1] }}
            className="text-sm md:text-base text-muted-foreground max-w-xl leading-relaxed mb-10"
          >
            从世界观构建到章节生成，do-write 无论是大纲设计还是细节润色，
            都能为您提供全方位的 AI 辅助，让您的创意无限流淌。
          </motion.p>

          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6, delay: 0.3, ease: [0.16, 1, 0.3, 1] }}
            className="flex flex-col sm:flex-row items-center gap-4"
          >
            <Link href="/login" className="w-full sm:w-auto">
              <Button
                size="lg"
                className="w-full sm:w-auto rounded-full bg-primary hover:bg-primary/90 font-medium transition-all active:scale-95 shadow-lg shadow-primary/25"
              >
                立即开始
                <ArrowRight className="size-4 ml-2" />
              </Button>
            </Link>

            <Button
              variant="secondary"
              size="lg"
              className="w-full sm:w-auto rounded-full font-medium active:scale-95 border bg-background/50 backdrop-blur-sm"
              onClick={() => window.open('https://github.com/zhazhahehedian', '_blank')}
            >
              了解更多
            </Button>
          </motion.div>

          <motion.div
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            transition={{ duration: 0.8, delay: 0.5 }}
            className="mt-16 flex flex-wrap gap-8 text-sm font-medium text-muted-foreground border-t border-border/50 pt-8"
          >
            <div className="flex items-center gap-2">
              <Brain className="w-5 h-5 text-indigo-500" />
              <span>智能构思</span>
            </div>
            <div className="flex items-center gap-2">
              <Zap className="w-5 h-5 text-yellow-500" />
              <span>极速生成</span>
            </div>
            <div className="flex items-center gap-2">
              <BookOpen className="w-5 h-5 text-green-500" />
              <span>完整上下文</span>
            </div>
          </motion.div>
        </div>

        <div className="hidden lg:block relative h-full max-h-[600px] w-full flex items-center justify-center">
          <motion.div
            initial={{ opacity: 0, scale: 0.9, rotate: -5 }}
            whileInView={{ opacity: 1, scale: 1, rotate: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 1, delay: 0.2, ease: "easeOut" }}
            className="relative w-full max-w-md aspect-[3/4]"
          >
            <div className="absolute top-0 right-0 w-72 h-72 bg-purple-500/20 rounded-full blur-3xl animate-pulse" />
            <div className="absolute bottom-0 left-0 w-72 h-72 bg-blue-500/20 rounded-full blur-3xl animate-pulse delay-75" />

            <div className="relative z-10 w-full h-full bg-background/40 backdrop-blur-xl border border-white/10 rounded-2xl shadow-2xl overflow-hidden flex flex-col">
              <NarrativeLoop />
            </div>

            <motion.div
              animate={{ y: [0, -15, 0] }}
              transition={{ duration: 5, repeat: Infinity, ease: "easeInOut" }}
              className="absolute -bottom-6 -right-6 z-20 bg-background/80 backdrop-blur-xl border border-white/20 p-4 rounded-xl shadow-xl max-w-[200px]"
            >
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 rounded-full bg-blue-500/10 flex items-center justify-center text-blue-500">
                  <PenTool className="size-5" />
                </div>
                <div>
                  <p className="text-xs text-muted-foreground">今日产出</p>
                  <div className="text-lg font-bold">4,520 字</div>
                </div>
              </div>
            </motion.div>

          </motion.div>
        </div>
      </div>
    </section>
  )
})
