import Link from "next/link"
import { Card, CardTitle, CardDescription, CardContent } from "@/components/ui/card"
import { Settings, Palette } from "lucide-react"

/* 设置项 */
const settingsItems = [
  {
    title: "AI 配置",
    description: "配置大模型 API 密钥和参数",
    icon: Settings,
    href: "/settings/api-config",
    category: "系统设置",
  },
  {
    title: "外观设置",
    description: "自定义界面主题和显示",
    icon: Palette,
    href: "/settings/appearance",
    category: "个人设置",
  },
]

export default function SettingsPage() {
  const groupedSettings = settingsItems.reduce((acc, item) => {
    if (!acc[item.category]) {
      acc[item.category] = []
    }
    acc[item.category].push(item)
    return acc
  }, {} as Record<string, typeof settingsItems>)

  return (
    <div className="space-y-6 py-6 container mx-auto px-4 md:px-8">
      <div className="font-semibold text-2xl">设置</div>

      {Object.entries(groupedSettings).map(([category, items]) => (
        <div key={category} className="space-y-4">
          <div className="font-medium text-sm text-muted-foreground">{category}</div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {items.map((item) => (
              <Link key={item.href} href={item.href}>
                <Card className="py-2 border hover:bg-muted/50 transition-colors cursor-pointer h-full">
                  <CardContent className="pt-6">
                    <div className="flex items-center gap-4">
                      <div className="p-2 bg-primary/10 rounded-lg">
                        <item.icon className="size-5 text-primary" />
                      </div>
                      <div>
                        <CardTitle className="mb-1 text-base">{item.title}</CardTitle>
                        <CardDescription className="text-xs">
                          {item.description}
                        </CardDescription>
                      </div>
                    </div>
                  </CardContent>
                </Card>
              </Link>
            ))}
          </div>
        </div>
      ))}
    </div>
  )
}
