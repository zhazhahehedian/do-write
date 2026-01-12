'use client'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Accordion,
  AccordionContent,
  AccordionItem,
  AccordionTrigger,
} from '@/components/ui/accordion'
import {
  BookOpen,
  Sparkles,
  Users,
  FileText,
  Settings,
  Lightbulb,
  MessageSquare,
  Zap,
} from 'lucide-react'
import Link from 'next/link'

const quickStartSteps = [
  {
    step: 1,
    title: '配置 AI API',
    description: '在设置中添加您的 AI 服务配置（支持 OpenAI、Azure、Ollama 本地模型、第三方中转等）',
    icon: Settings,
    link: '/settings/api-config',
  },
  {
    step: 2,
    title: '创建项目',
    description: '点击"创建项目"，填写小说标题、类型等基本信息',
    icon: BookOpen,
    link: '/projects',
  },
  {
    step: 3,
    title: '使用创作向导',
    description: 'AI 将帮助您生成世界观、角色和大纲',
    icon: Sparkles,
    link: null,
  },
  {
    step: 4,
    title: '生成章节',
    description: '基于大纲，选择写作风格，让 AI 生成精彩的章节内容',
    icon: FileText,
    link: null,
  },
]

const faqItems = [
  {
    question: '如何获取 AI API 密钥？',
    answer: '根据您选择的提供商：1) OpenAI - 在 platform.openai.com 注册并生成 API Key；2) Azure OpenAI - 在 Azure 门户创建资源后获取；3) Ollama - 本地部署无需密钥，填写任意值即可；4) 第三方中转 - 从对应服务商（如硅基流动、DeepSeek 等）获取。',
  },
  {
    question: '支持哪些 AI 模型？',
    answer: '支持四种类型：1) OpenAI 官方（GPT-4、GPT-4o 等）；2) Azure OpenAI；3) Ollama 本地模型（如 Llama、Qwen 等）；4) OpenAI 兼容的第三方服务（DeepSeek、硅基流动、各种中转站等），只需填写对应的 Base URL 即可。',
  },
  {
    question: '创作向导的三个步骤是什么？',
    answer: '创作向导包含：1) 世界观生成 - 设定时代背景、地点、氛围和规则；2) 角色生成 - 批量创建主角、配角、反派和组织；3) 大纲生成 - 规划故事结构和章节大纲。',
  },
  {
    question: '什么是写作风格？',
    answer: '系统预设了6种写作风格：自然沉浸、古典雅致、冷硬现代、意识流、白描速写、感官特写。每种风格会影响 AI 的叙事方式和语言特点。您可以在生成章节时选择使用。',
  },
  {
    question: '章节生成使用了什么技术？',
    answer: '章节生成采用 RAG（检索增强生成）技术，会自动检索相关的故事记忆和前文内容，确保情节连贯、人物一致。',
  },
  {
    question: '我可以编辑 AI 生成的内容吗？',
    answer: '当然可以！AI 生成的世界观、角色、大纲和章节都支持手动编辑。AI 是您的创作助手，最终的创作决定权在您手中。',
  },
]

const features = [
  {
    title: '创作向导',
    description: '三步引导式创作流程，帮助您快速构建小说世界',
    icon: Sparkles,
    items: ['世界观生成', '批量角色创建', '大纲规划'],
  },
  {
    title: '智能章节生成',
    description: '基于 RAG 技术的章节生成，保持情节连贯',
    icon: Zap,
    items: ['流式生成', '上下文记忆', '风格定制'],
  },
  {
    title: '角色管理',
    description: '管理小说中的所有角色，包括主角、配角、反派',
    icon: Users,
    items: ['角色档案', '性格设定', '背景故事'],
  },
  {
    title: '多样写作风格',
    description: '6种预设风格，满足不同类型小说的需求',
    icon: MessageSquare,
    items: ['自然沉浸', '古典雅致', '冷硬现代'],
  },
]

export default function HelpPage() {
  return (
    <div className="space-y-8">
      {/* 页面标题 */}
      <div>
        <h1 className="text-2xl font-bold tracking-tight">帮助文档</h1>
        <p className="text-muted-foreground">
          了解如何使用 do-write 进行 AI 辅助小说创作
        </p>
      </div>

      {/* 快速开始 */}
      <Card>
        <CardHeader>
          <CardTitle className="flex items-center gap-2">
            <Lightbulb className="h-5 w-5 text-amber-500" />
            快速开始
          </CardTitle>
          <CardDescription>
            按照以下步骤，几分钟内开始您的创作之旅
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
            {quickStartSteps.map((item) => (
              <div
                key={item.step}
                className="relative flex flex-col gap-2 rounded-lg border p-4 hover:bg-muted/50 transition-colors"
              >
                <div className="flex items-center gap-3">
                  <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary text-primary-foreground text-sm font-medium">
                    {item.step}
                  </div>
                  <item.icon className="h-5 w-5 text-muted-foreground" />
                </div>
                <h3 className="font-medium">{item.title}</h3>
                <p className="text-sm text-muted-foreground">{item.description}</p>
                {item.link && (
                  <Link
                    href={item.link}
                    className="text-sm text-primary hover:underline mt-auto"
                  >
                    前往设置 →
                  </Link>
                )}
              </div>
            ))}
          </div>
        </CardContent>
      </Card>

      {/* 核心功能 */}
      <div>
        <h2 className="text-xl font-semibold mb-4">核心功能</h2>
        <div className="grid gap-4 md:grid-cols-2">
          {features.map((feature) => (
            <Card key={feature.title}>
              <CardHeader className="pb-3">
                <CardTitle className="text-base flex items-center gap-2">
                  <feature.icon className="h-5 w-5 text-primary" />
                  {feature.title}
                </CardTitle>
                <CardDescription>{feature.description}</CardDescription>
              </CardHeader>
              <CardContent>
                <div className="flex flex-wrap gap-2">
                  {feature.items.map((item) => (
                    <Badge key={item} variant="secondary">
                      {item}
                    </Badge>
                  ))}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      </div>

      {/* 常见问题 */}
      <div>
        <h2 className="text-xl font-semibold mb-4">常见问题</h2>
        <Card>
          <CardContent className="pt-6">
            <Accordion type="single" collapsible className="w-full">
              {faqItems.map((item, index) => (
                <AccordionItem key={index} value={`item-${index}`}>
                  <AccordionTrigger className="text-left">
                    {item.question}
                  </AccordionTrigger>
                  <AccordionContent className="text-muted-foreground">
                    {item.answer}
                  </AccordionContent>
                </AccordionItem>
              ))}
            </Accordion>
          </CardContent>
        </Card>
      </div>

      {/* 后续规划 */}
      <Card className="border-dashed">
        <CardHeader>
          <CardTitle className="text-base">即将推出</CardTitle>
          <CardDescription>
            以下功能正在开发中，敬请期待
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap gap-3">
            <Badge variant="outline" className="text-muted-foreground">
              自定义提示词模板
            </Badge>
            <Badge variant="outline" className="text-muted-foreground">
              MCP 插件支持
            </Badge>
            <Badge variant="outline" className="text-muted-foreground">
              角色关系图谱
            </Badge>
          </div>
        </CardContent>
      </Card>

      {/* 联系方式 */}
      <div className="rounded-lg border bg-muted/50 p-4 text-sm text-muted-foreground">
        <p>
          <strong>反馈与建议：</strong>如果您在使用过程中遇到问题或有任何建议，欢迎通过 GitHub Issues 反馈。
        </p>
      </div>
    </div>
  )
}
