'use client'

import * as React from "react"
import Link from "next/link"
import { usePathname, useRouter } from "next/navigation"
import { motion } from "motion/react"
import { toast } from "sonner"
import { Button } from "@/components/ui/button"
import { Spinner } from "@/components/ui/spinner"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
} from "@/components/ui/dropdown-menu"
import {
  Sidebar,
  SidebarContent,
  SidebarGroup,
  SidebarGroupContent,
  SidebarGroupLabel,
  SidebarHeader,
  SidebarMenu,
  SidebarMenuButton,
  SidebarMenuItem,
  useSidebar,
} from "@/components/ui/sidebar"
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from "@/components/ui/alert-dialog"
import {
  Home,
  FolderOpen,
  Palette,
  KeyRound,
  LogOut,
  ChevronRight,
  ChevronLeft,
  ChevronDown,
  FileText,
  HelpCircle,
} from "lucide-react"

import { useAuthStore } from "@/lib/store/auth-store"
import { authApi } from "@/lib/api/auth"

/* 导航数据 */
const data = {
  navMain: [
    { title: "首页", url: "/home", icon: Home },
    { title: "我的项目", url: "/projects", icon: FolderOpen },
  ],
  tools: [
    { title: "写作风格", url: "/styles", icon: FileText },
    { title: "帮助文档", url: "/help", icon: HelpCircle },
  ],
}

/**
 * 应用侧边栏组件
 * 显示应用侧边栏
 */
export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const { toggleSidebar, state, isMobile, setOpenMobile } = useSidebar()
  const { user, logout } = useAuthStore()
  const [showLogoutDialog, setShowLogoutDialog] = React.useState(false)
  const [isLoggingOut, setIsLoggingOut] = React.useState(false)
  const pathname = usePathname()
  const router = useRouter()

  const [userMenuOpen, setUserMenuOpen] = React.useState(false)

  const handleCloseSidebar = React.useCallback(() => {
    if (isMobile) {
      setOpenMobile(false)
    }
  }, [isMobile, setOpenMobile])

  const handleLogout = async () => {
    setIsLoggingOut(true)
    try {
      await authApi.logout()
      logout()
      setShowLogoutDialog(false)
      router.push('/login')
    } catch (error) {
      toast.error("登出失败", {
        description: error instanceof Error ? error.message : "登出时发生错误，请重试"
      })
      setIsLoggingOut(false)
    }
  }

  return (
    <>
      <Sidebar collapsible="icon" {...props} className="px-2 relative border-r border-border/40 group-data-[collapsible=icon]">
        <Button
          onClick={toggleSidebar}
          variant="ghost"
          size="icon"
          className="absolute top-1/2 -right-6 w-2 h-4 text-muted-foreground hover:bg-background hidden md:flex"
        >
          <motion.span
            key={state}
            className="inline-flex"
            initial={{
              opacity: 0,
              x: state === "expanded" ? 4 : -4,
            }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ duration: 0.15, ease: "easeOut" }}
            whileHover={{ x: state === "expanded" ? -2 : 2 }}
            whileTap={{ scale: 0.9 }}
          >
            {state === "expanded" ? (
              <ChevronLeft className="size-6" />
            ) : (
              <ChevronRight className="size-6" />
            )}
          </motion.span>
        </Button>

        <SidebarHeader className="py-4 md:-ml-2">
          <DropdownMenu open={userMenuOpen} onOpenChange={setUserMenuOpen}>
            <DropdownMenuTrigger asChild>
              <Button
                variant="ghost"
                className="flex h-12 hover:bg-sidebar-accent hover:text-sidebar-accent-foreground group-data-[collapsible=icon]:ml-2"
                suppressHydrationWarning
              >
                <div className="relative overflow-visible">
                  <Avatar className="size-8 rounded relative z-10">
                    <AvatarImage
                      src={user?.avatar}
                      alt={user?.username}
                    />
                    <AvatarFallback className="rounded bg-primary/10 text-primary text-sm font-medium">
                      {user?.username?.charAt(0)?.toUpperCase() || "U"}
                    </AvatarFallback>
                  </Avatar>
                </div>
                <div className="flex flex-col items-start flex-1 text-left group-data-[collapsible=icon]:hidden ml-2 min-w-0">
                  <span className="text-sm font-medium truncate w-full text-left">
                    {user?.username || "Guest User"}
                  </span>
                  <span className="text-[11px] font-medium text-muted-foreground truncate w-full text-left">
                    {user?.userType === 'ADMIN' ? '管理员' : '普通用户'}
                  </span>
                </div>
                <ChevronDown className="size-4 text-muted-foreground ml-auto group-data-[collapsible=icon]:hidden" />
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent
              className={isMobile || state === "collapsed" ? "ml-1 w-68 z-[200]" : "w-64 z-[200]"}
              align="start"
              side={isMobile || state === "collapsed" ? "bottom" : "right"}
              sideOffset={4}
            >
              <DropdownMenuLabel>
                <div className="flex flex-col items-center mb-4 gap-1 pt-4">
                  <Avatar className="size-16 rounded-full mb-2">
                    <AvatarImage
                      src={user?.avatar}
                      alt={user?.username}
                    />
                    <AvatarFallback className="rounded-full bg-primary/10 text-primary text-xl">
                      {user?.username?.charAt(0)?.toUpperCase() || "U"}
                    </AvatarFallback>
                  </Avatar>
                  <span className="text-base font-semibold truncate mt-2">
                    {user?.username || "Guest User"}
                  </span>
                  <span className="text-xs font-base text-muted-foreground max-w-[200px] truncate">
                    {user?.email
                      ? (user.email.length > 25
                          ? `${user.email.substring(0, 12)}...${user.email.substring(user.email.lastIndexOf('@'))}`
                          : user.email)
                      : "No Email"}
                  </span>
                </div>
              </DropdownMenuLabel>
              <DropdownMenuItem onClick={() => {
                router.push("/settings/appearance")
                handleCloseSidebar()
              }}>
                <Palette className="mr-2 size-4" />
                <span>外观设置</span>
              </DropdownMenuItem>
              <DropdownMenuItem onClick={() => {
                router.push("/settings/api-config")
                handleCloseSidebar()
              }}>
                <KeyRound className="mr-2 size-4" />
                <span>API 配置</span>
              </DropdownMenuItem>
              <DropdownMenuSeparator className="my-2" />
              <DropdownMenuItem className="text-destructive hover:bg-destructive/50" onClick={() => setShowLogoutDialog(true)}>
                <LogOut className="mr-2 size-4 text-destructive" />
                <span>退出登录</span>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </SidebarHeader>

        <SidebarContent className="group-data-[collapsible=icon]">
          <SidebarGroup className="py-0">
            <SidebarGroupContent className="py-1">
              <SidebarMenu className="gap-1">
                {data.navMain.map((item) => (
                  <SidebarMenuItem key={item.title}>
                    <SidebarMenuButton
                      tooltip={item.title}
                      isActive={pathname === item.url}
                      asChild
                    >
                      <Link href={item.url} onClick={handleCloseSidebar}>
                        {item.icon && <item.icon />}
                        <span>{item.title}</span>
                      </Link>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>

          <SidebarGroup className="py-0 pt-4">
            <SidebarGroupLabel className="text-xs font-normal text-muted-foreground">
              工具
            </SidebarGroupLabel>
            <SidebarGroupContent className="py-1">
              <SidebarMenu className="gap-1">
                {data.tools.map((item) => (
                  <SidebarMenuItem key={item.title}>
                    <SidebarMenuButton
                      tooltip={item.title}
                      isActive={pathname === item.url}
                      asChild
                    >
                      <Link href={item.url} onClick={handleCloseSidebar}>
                        {item.icon && <item.icon />}
                        <span>{item.title}</span>
                      </Link>
                    </SidebarMenuButton>
                  </SidebarMenuItem>
                ))}
              </SidebarMenu>
            </SidebarGroupContent>
          </SidebarGroup>

        </SidebarContent>
      </Sidebar>

      <AlertDialog open={showLogoutDialog} onOpenChange={(open) => !isLoggingOut && setShowLogoutDialog(open)}>
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认登出</AlertDialogTitle>
            <AlertDialogDescription>
              {isLoggingOut ? '正在登出，请稍候...' : '您确定要登出当前账户吗？'}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={isLoggingOut}>取消</AlertDialogCancel>
            <AlertDialogAction onClick={handleLogout} disabled={isLoggingOut}>
              {isLoggingOut && <Spinner className="mr-2" />}
              {isLoggingOut ? '登出中...' : '确认登出'}
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </>
  )
}
