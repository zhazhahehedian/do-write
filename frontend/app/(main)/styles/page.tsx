'use client'

import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card'
import { Badge } from '@/components/ui/badge'
import {
  Leaf,
  BookOpen,
  Zap,
  Waves,
  Pencil,
  Eye,
} from 'lucide-react'

/**
 * 预设写作风格数据
 * 与后端 WritingStyle 枚举保持一致
 */
const writingStyles = [
  {
    code: 'natural',
    name: '自然沉浸',
    subtitle: 'Natural & Immersive',
    description: '祛除翻译腔，强调生活质感，像呼吸一样自然的叙事',
    icon: Leaf,
    color: 'bg-green-500',
    features: [
      '拒绝翻译腔与书面化，多用短句和流水句',
      '生活化的颗粒度，聚焦微小的细节',
      '用动作和名词展示，而非抽象形容词',
    ],
  },
  {
    code: 'classical',
    name: '古典雅致',
    subtitle: 'Classical & Elegant',
    description: '白话文与古典韵味的结合，强调留白与炼字',
    icon: BookOpen,
    color: 'bg-amber-600',
    features: [
      '注重声调韵律，使用双音节词或四字短语',
      '克制的修辞，意在言外，留三分余地',
      '避免现代科技词汇和网络用语',
    ],
  },
  {
    code: 'modern',
    name: '冷硬现代',
    subtitle: 'Modern & Hard-boiled',
    description: '海明威式的冰山理论，节奏极快，零度情感',
    icon: Zap,
    color: 'bg-slate-700',
    features: [
      '只写动作和对话，剔除心理描写',
      '电影蒙太奇节奏，句子短、脆、硬',
      '高信息密度，删除所有废话',
    ],
  },
  {
    code: 'poetic',
    name: '意识流',
    subtitle: 'Stream of Consciousness',
    description: '注重感官通感与内心独白，打破现实与幻想的边界',
    icon: Waves,
    color: 'bg-purple-600',
    features: [
      '通感与陌生化，打通五感',
      '情绪的具象化，寻找客观对应物',
      '流动的句式，允许思维的非线性跳跃',
    ],
  },
  {
    code: 'concise',
    name: '白描速写',
    subtitle: 'Sketch & Concise',
    description: '只有骨架的叙事，强调绝对的精准和功能性',
    icon: Pencil,
    color: 'bg-gray-500',
    features: [
      '功能性第一，每句话必须推动情节',
      '简单的主谓宾结构，减少修饰语',
      '对话直接进入主题，去除寒暄',
    ],
  },
  {
    code: 'vivid',
    name: '感官特写',
    subtitle: 'Sensory & Vivid',
    description: '高分辨率的描写，强调材质、光影和微观细节',
    icon: Eye,
    color: 'bg-rose-500',
    features: [
      '反套路细节，关注物体的质感',
      '动态捕捉，让读者产生生理性反应',
      '用具体的动词带动感官描写',
    ],
  },
]

export default function StylesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold tracking-tight">写作风格</h1>
        <p className="text-muted-foreground">
          系统预设了 6 种写作风格，在生成章节时可以选择使用。每种风格都有独特的叙事特点和语言规则。
        </p>
      </div>

      <div className="grid gap-6 md:grid-cols-2 lg:grid-cols-3">
        {writingStyles.map((style) => (
          <Card key={style.code} className="group hover:shadow-md transition-shadow">
            <CardHeader className="pb-3">
              <div className="flex items-start gap-3">
                <div className={`rounded-lg p-2 ${style.color} text-white`}>
                  <style.icon className="h-5 w-5" />
                </div>
                <div className="flex-1 space-y-1">
                  <CardTitle className="text-lg flex items-center gap-2">
                    {style.name}
                    <Badge variant="outline" className="text-xs font-normal">
                      {style.code}
                    </Badge>
                  </CardTitle>
                  <p className="text-xs text-muted-foreground">{style.subtitle}</p>
                </div>
              </div>
              <CardDescription className="pt-2">
                {style.description}
              </CardDescription>
            </CardHeader>
            <CardContent>
              <ul className="space-y-2 text-sm text-muted-foreground">
                {style.features.map((feature, index) => (
                  <li key={index} className="flex items-start gap-2">
                    <span className="text-primary mt-1">•</span>
                    <span>{feature}</span>
                  </li>
                ))}
              </ul>
            </CardContent>
          </Card>
        ))}
      </div>

      <div className="rounded-lg border bg-muted/50 p-4 text-sm text-muted-foreground">
        <p>
          <strong>提示：</strong>写作风格在生成章节时生效。进入项目后，在章节生成对话框中可以选择想要的风格。
          不同风格会影响 AI 的叙事方式、句式节奏和用词偏好。
        </p>
      </div>
    </div>
  )
}
