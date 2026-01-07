"use client"

import { useState, useEffect } from "react"
import { Button } from "@/components/ui/button"
import { Bell, Settings, Search, Moon, Sun, Maximize2, Minimize2 } from "lucide-react"
import { useAuthStore } from "@/lib/store/auth-store"
import { SidebarTrigger } from "@/components/ui/sidebar"
import { useTheme } from "next-themes"
import { useRouter } from "next/navigation"
import { SearchDialog } from "@/components/layout/search-dialog"

export function Header({ isFullWidth = false, onToggleFullWidth }: { isFullWidth?: boolean, onToggleFullWidth?: (value: boolean) => void }) {
  const { user } = useAuthStore()
  const { theme, setTheme } = useTheme()
  const router = useRouter()
  const [mounted, setMounted] = useState(false)
  const [searchOpen, setSearchOpen] = useState(false)

  useEffect(() => {
    setMounted(true)
  }, [])

  return (
    <div className="flex flex-col w-full">
      <header className="flex h-16 shrink-0 items-center bg-background px-4 md:px-0 border-b md:border-none">
        <div className="flex w-full items-center justify-between md:hidden">
          <div className="flex items-center gap-2">
            <SidebarTrigger />
            <span className="text-sm font-medium truncate max-w-[120px]">
              {user?.username || "Guest"}
            </span>
          </div>
          <div className="flex items-center gap-1">
            <Button variant="ghost" size="icon" className="size-9 text-muted-foreground hover:text-foreground" onClick={() => setSearchOpen(true)}>
              <Search className="size-[18px]" />
              <span className="sr-only">搜索</span>
            </Button>
            <Button variant="ghost" size="icon" className="size-9 text-muted-foreground hover:text-foreground" onClick={() => router.push('/settings/appearance')}>
              <Settings className="size-[18px]" />
              <span className="sr-only">设置</span>
            </Button>
          </div>
        </div>

        <div className={`hidden md:flex w-full items-center gap-4 ${!isFullWidth ? "max-w-[1320px]" : ""} mx-auto px-6`}>
          <div className="relative w-64 cursor-pointer" onClick={() => setSearchOpen(true)}>
            <Search className="absolute left-3 top-1/2 size-4 -translate-y-1/2 text-muted-foreground" />
            <div className="h-9 border bg-muted/50 hover:bg-muted/80 transition-colors pl-10 pr-3 text-sm rounded-md flex items-center text-muted-foreground">
              <span>搜索...</span>
              <kbd className="ml-auto pointer-events-none inline-flex h-5 select-none items-center gap-1 rounded border bg-background px-1.5 font-mono text-[10px] font-medium text-muted-foreground opacity-100">
                <span className="text-xs">⌘</span>K
              </kbd>
            </div>
          </div>

          <div className="ml-auto flex items-center gap-1">
            <Button variant="ghost" size="icon" className="size-9 text-muted-foreground hover:text-foreground">
              <Bell className="size-[18px]" />
              <span className="sr-only">通知</span>
            </Button>
            <Button variant="ghost" size="icon" className="size-9 text-muted-foreground hover:text-foreground" onClick={() => router.push('/settings/appearance')}>
              <Settings className="size-[18px]" />
              <span className="sr-only">设置</span>
            </Button>

            {onToggleFullWidth && (
              <Button variant="ghost" size="icon" className="size-9 text-muted-foreground hover:text-foreground" onClick={() => onToggleFullWidth(!isFullWidth)}>
                {isFullWidth ? <Minimize2 className="size-[18px]" /> : <Maximize2 className="size-[18px]" />}
                <span className="sr-only">切换全宽</span>
              </Button>
            )}

            <Button variant="ghost" size="icon" className="size-9 text-muted-foreground hover:text-foreground" onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}>
              {mounted ? (theme === 'dark' ? <Sun className="size-[18px]" /> : <Moon className="size-[18px]" />) : <Moon className="size-[18px]" />}
              <span className="sr-only">主题切换</span>
            </Button>
          </div>
        </div>

        {mounted && <SearchDialog open={searchOpen} onOpenChange={setSearchOpen} />}
      </header>
    </div>
  )
}
