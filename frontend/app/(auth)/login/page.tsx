'use client'

import { motion } from 'motion/react'
import { AuroraBackground } from '@/components/ui/aurora-background'
import { LoginForm } from '@/components/auth/login-form'

export default function LoginPage() {
  return (
    <AuroraBackground>
      <motion.div
        initial={{ opacity: 0, y: 40 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true }}
        transition={{
          delay: 0.3,
          duration: 0.8,
          ease: "easeInOut",
        }}
        className="relative z-10 w-full max-w-sm px-4"
      >
        <div className="text-center mb-10 space-y-2">
          <h1 className="text-3xl font-bold tracking-tight text-foreground">
            do-write <span className="font-serif italic text-primary">Novel</span>
          </h1>
          <p className="text-sm text-muted-foreground font-light">
            AI 驱动的小说创作平台
          </p>
        </div>

        <div className="w-full">
            <LoginForm />
        </div>

        <div className="mt-8 text-center text-xs text-muted-foreground">
          &copy; {new Date().getFullYear()} do-write. All rights reserved.
        </div>
      </motion.div>
    </AuroraBackground>
  )
}