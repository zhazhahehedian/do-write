'use client'

import { useState } from 'react'
import { SidebarProvider, SidebarInset } from '@/components/ui/sidebar'
import { AppSidebar } from '@/components/layout/app-sidebar'
import { Header } from '@/components/layout/header'
import { AuthGuard } from '@/components/auth/auth-guard'
import { motion } from 'motion/react'
import { usePathname } from 'next/navigation'

export default function MainLayout({
  children,
}: {
  children: React.ReactNode
}) {
  const pathname = usePathname()
  const [isFullWidth, setIsFullWidth] = useState(false)

  return (
    <AuthGuard>
      <SidebarProvider
        className="h-screen"
        style={
          {
            "--header-height": "64px",
          } as React.CSSProperties
        }
      >
        <AppSidebar />
        <SidebarInset className="flex flex-col min-w-0 h-screen">
          <Header isFullWidth={isFullWidth} onToggleFullWidth={setIsFullWidth} />
          <div className="flex flex-1 flex-col bg-background overflow-y-auto overflow-x-hidden min-w-0">
             <div className={`w-full mx-auto px-6 py-6 min-w-0 ${!isFullWidth ? "max-w-[1320px]" : ""}`}>
                <motion.div
                  key={pathname}
                  initial={{ opacity: 0, y: 10 }}
                  animate={{ opacity: 1, y: 0 }}
                  transition={{
                    duration: 0.4,
                    ease: "easeOut",
                  }}
                  className="w-full"
                >
                  {children}
                </motion.div>
             </div>
          </div>
        </SidebarInset>
      </SidebarProvider>
    </AuthGuard>
  )
}
