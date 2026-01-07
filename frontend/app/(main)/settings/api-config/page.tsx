'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { userConfigApi } from '@/lib/api/user-config'
import { useAuthStore } from '@/lib/store/auth-store'
import type { UserApiConfig } from '@/lib/types/user-config'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from '@/components/ui/form'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog'
import { Switch } from '@/components/ui/switch'
import { Badge } from '@/components/ui/badge'
import { Loader2, Plus, Trash2, Pencil, Check } from 'lucide-react'
import { toast } from 'sonner'

const configSchema = z.object({
  configName: z.string().min(1, '请输入配置名称'),
  apiType: z.string().min(1, '请选择提供商'),
  baseUrl: z.string().url('请输入有效的 URL').optional().or(z.literal('')),
  apiKey: z.string().min(1, '请输入 API Key'),
  modelName: z.string().min(1, '请输入模型名称'),
})

type ConfigFormValues = z.infer<typeof configSchema>

export default function ApiConfigPage() {
  const queryClient = useQueryClient()
  const { user } = useAuthStore()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingConfig, setEditingConfig] = useState<UserApiConfig | null>(null)

  // 查询配置列表
  const { data: configs, isLoading } = useQuery({
    queryKey: ['api-configs', user?.id],
    queryFn: () => userConfigApi.list(user?.id || ''),
    enabled: !!user?.id
  })

  // 创建/更新配置
  const saveMutation = useMutation<any, Error, ConfigFormValues & { id?: number }>({
    mutationFn: (data: ConfigFormValues & { id?: number }) => {
      if (data.id) {
        return userConfigApi.update({ ...data, userId: Number(user?.id) } as any)
      } else {
        return userConfigApi.create({ ...data, userId: Number(user?.id) } as any)
      }
    },
    onSuccess: () => {
      toast.success(editingConfig ? '配置已更新' : '配置已创建')
      setDialogOpen(false)
      queryClient.invalidateQueries({ queryKey: ['api-configs'] })
    },
    onError: (error: any) => {
      toast.error(error.message || '保存失败')
    },
  })

  // 删除配置
  const deleteMutation = useMutation({
    mutationFn: (id: number) => userConfigApi.delete(user?.id || '', id.toString()),
    onSuccess: () => {
      toast.success('配置已删除')
      queryClient.invalidateQueries({ queryKey: ['api-configs'] })
    },
  })

  // 设置激活
  const activeMutation = useMutation({
    mutationFn: (id: number) => userConfigApi.setActive(user?.id || '', id.toString()),
    onSuccess: () => {
      toast.success('已设为默认配置')
      queryClient.invalidateQueries({ queryKey: ['api-configs'] })
    },
  })

  const form = useForm<ConfigFormValues>({
    resolver: zodResolver(configSchema),
    defaultValues: {
      configName: '',
      apiType: 'OPENAI',
      baseUrl: '',
      apiKey: '',
      modelName: '',
    },
  })

  const handleEdit = (config: UserApiConfig) => {
    setEditingConfig(config)
    form.reset({
      configName: config.configName,
      apiType: config.apiType,
      baseUrl: config.baseUrl || '',
      apiKey: '', // Don't pre-fill masked key
      modelName: config.modelName || '',
    })
    setDialogOpen(true)
  }

  const handleCreate = () => {
    setEditingConfig(null)
    form.reset({
        configName: '',
        apiType: 'OPENAI',
        baseUrl: '',
        apiKey: '',
        modelName: '',
    })
    setDialogOpen(true)
  }

  const onSubmit = (data: ConfigFormValues) => {
    saveMutation.mutate({ ...data, id: editingConfig?.id })
  }

  return (
    <div className="container max-w-4xl mx-auto py-8 space-y-8">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold">AI 模型配置</h1>
          <p className="text-muted-foreground">
            管理您的大模型 API 配置，用于内容生成。
          </p>
        </div>
        <Button onClick={handleCreate}>
          <Plus className="mr-2 h-4 w-4" />
          添加配置
        </Button>
      </div>

      {isLoading ? (
        <div className="flex justify-center py-8">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      ) : (
        <div className="grid gap-4">
          {configs?.map((config) => (
            <Card key={config.id} className={config.isDefault === 1 ? 'border-primary' : ''}>
              <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
                <div className="space-y-1">
                  <CardTitle className="text-base font-medium flex items-center gap-2">
                    {config.configName}
                    {config.isDefault === 1 && (
                      <Badge variant="default" className="text-xs">
                        使用中
                      </Badge>
                    )}
                  </CardTitle>
                  <CardDescription>
                    {config.apiType} · {config.modelName}
                  </CardDescription>
                </div>
                <div className="flex items-center gap-2">
                  {config.isDefault !== 1 && (
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => activeMutation.mutate(config.id)}
                    >
                      <Check className="mr-2 h-4 w-4" />
                      设为默认
                    </Button>
                  )}
                  <Button
                    variant="ghost"
                    size="icon"
                    onClick={() => handleEdit(config)}
                  >
                    <Pencil className="h-4 w-4" />
                  </Button>
                  <Button
                    variant="ghost"
                    size="icon"
                    className="text-destructive hover:text-destructive"
                    onClick={() => {
                        if(confirm('确定要删除此配置吗？')) {
                            deleteMutation.mutate(config.id)
                        }
                    }}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </CardHeader>
            </Card>
          ))}

          {configs?.length === 0 && (
            <div className="text-center py-12 text-muted-foreground border border-dashed rounded-lg">
              暂无配置，请添加一个 API 配置以开始使用。
            </div>
          )}
        </div>
      )}

      <Dialog open={dialogOpen} onOpenChange={setDialogOpen}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>{editingConfig ? '编辑配置' : '添加配置'}</DialogTitle>
            <DialogDescription>
              配置您的 LLM API 连接信息。
            </DialogDescription>
          </DialogHeader>
          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
              <FormField
                control={form.control}
                name="configName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>配置名称</FormLabel>
                    <FormControl>
                      <Input placeholder="例如：我的 GPT-4" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="apiType"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>提供商</FormLabel>
                    <Select onValueChange={field.onChange} defaultValue={field.value}>
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="选择提供商" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="OPENAI">OpenAI</SelectItem>
                        <SelectItem value="AZURE_OPENAI">Azure OpenAI</SelectItem>
                        <SelectItem value="CUSTOM">Custom</SelectItem>
                      </SelectContent>
                    </Select>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="modelName"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>模型名称</FormLabel>
                    <FormControl>
                      <Input placeholder="例如：gpt-4-turbo" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="baseUrl"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>Base URL (选填)</FormLabel>
                    <FormControl>
                      <Input placeholder="例如：https://api.openai.com/v1" {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="apiKey"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>API Key</FormLabel>
                    <FormControl>
                      <Input type="password" placeholder="sk-..." {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <DialogFooter>
                <Button type="submit" disabled={saveMutation.isPending}>
                  {saveMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  保存
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>
    </div>
  )
}
