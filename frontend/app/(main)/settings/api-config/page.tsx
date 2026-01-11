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
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import {
  Form,
  FormControl,
  FormDescription,
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
} from '@/components/ui/dialog'
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogHeader,
  AlertDialogTitle,
} from '@/components/ui/alert-dialog'
import { Badge } from '@/components/ui/badge'
import { Loader2, Plus, Trash2, Pencil, Check, Zap } from 'lucide-react'
import { toast } from 'sonner'

const apiTypeValues = ['OPENAI', 'AZURE_OPENAI', 'OLLAMA', 'CUSTOM'] as const
type ApiType = (typeof apiTypeValues)[number]

const optionalUrl = z.preprocess(
  (value) => (typeof value === 'string' && value.trim() === '' ? undefined : value),
  z.string().trim().url('请输入有效的 URL').optional()
)

const configSchema = z.object({
  configName: z.string().trim().min(1, '请输入配置名称'),
  apiType: z.enum(apiTypeValues, { required_error: '请选择提供商' }),
  baseUrl: optionalUrl,
  apiKey: z.string().trim().min(1, '请输入 API Key'),
  modelName: z.string().trim().min(1, '请输入模型名称'),
}).superRefine((values, ctx) => {
  const baseUrlRequired = values.apiType === 'AZURE_OPENAI' || values.apiType === 'CUSTOM'
  if (baseUrlRequired && !values.baseUrl) {
    ctx.addIssue({
      code: z.ZodIssueCode.custom,
      path: ['baseUrl'],
      message: '该提供商需要填写 Base URL',
    })
  }
})

type ConfigFormValues = z.infer<typeof configSchema>

export default function ApiConfigPage() {
  const queryClient = useQueryClient()
  const { user } = useAuthStore()
  const [dialogOpen, setDialogOpen] = useState(false)
  const [editingConfig, setEditingConfig] = useState<UserApiConfig | null>(null)
  const [pendingDeleteConfig, setPendingDeleteConfig] = useState<UserApiConfig | null>(null)
  const [selectedApiType, setSelectedApiType] = useState<ApiType>('OPENAI')

  // 查询配置列表
  const { data: configs, isLoading } = useQuery({
    queryKey: ['api-configs', user?.id],
    queryFn: () => userConfigApi.list(user?.id || ''),
    enabled: !!user?.id
  })

  // 创建/更新配置
  type SaveVariables = ConfigFormValues & { id?: number }
  type SaveResult = number | boolean

  const saveMutation = useMutation<SaveResult, Error, SaveVariables>({
    mutationFn: (data: SaveVariables) => {
      const userId = user?.id || ''
      if (data.id) {
        return userConfigApi.update({ ...data, userId, id: String(data.id) })
      }
      return userConfigApi.create({ ...data, userId })
    },
    onSuccess: () => {
      toast.success(editingConfig ? '配置已更新' : '配置已创建')
      setDialogOpen(false)
      queryClient.invalidateQueries({ queryKey: ['api-configs'] })
    },
    onError: (error) => {
      toast.error(error.message || '保存失败')
    },
  })

  // 删除配置
  const deleteMutation = useMutation({
    mutationFn: (id: number) => userConfigApi.delete(user?.id || '', id.toString()),
    onSuccess: () => {
      toast.success('配置已删除')
      setPendingDeleteConfig(null)
      queryClient.invalidateQueries({ queryKey: ['api-configs'] })
    },
    onError: (error: Error) => {
      toast.error(error.message || '删除失败')
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

  // 测试连接
  const validateMutation = useMutation({
    mutationFn: (data: ConfigFormValues) => {
      const userId = user?.id || ''
      return userConfigApi.validate({ ...data, userId })
    },
    onSuccess: () => {
      toast.success('连接测试成功')
    },
    onError: (error: Error) => {
      toast.error(error.message || '连接测试失败，请检查配置')
    },
  })

  const handleTestConnection = async () => {
    const isValid = await form.trigger()
    if (!isValid) {
      toast.error('请先填写完整的配置信息')
      return
    }
    const values = form.getValues()
    validateMutation.mutate(values)
  }

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

  const baseUrlRequired = selectedApiType === 'AZURE_OPENAI' || selectedApiType === 'CUSTOM'
  const baseUrlMeta = (() => {
    switch (selectedApiType) {
      case 'OLLAMA':
        return {
          placeholder: '例如：http://localhost:11434',
          description: '不填则默认使用 http://localhost:11434。',
        }
      case 'AZURE_OPENAI':
        return {
          placeholder: '例如：https://{resource}.openai.azure.com',
          description: 'Azure OpenAI 需要填写资源端点地址（通常不包含 /v1）。',
        }
      case 'CUSTOM':
        return {
          placeholder: '例如：https://xxx.com/v1',
          description: '第三方/中转端点一般以 /v1 结尾，请以服务商文档为准。',
        }
      case 'OPENAI':
      default:
        return {
          placeholder: '例如：https://api.openai.com/v1',
          description: '不填则使用 OpenAI 官方默认端点；使用代理/中转时再填写。',
        }
    }
  })()

  const handleEdit = (config: UserApiConfig) => {
    setEditingConfig(config)
    setSelectedApiType(config.apiType as ApiType)
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
    setSelectedApiType('OPENAI')
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
                    onClick={() => setPendingDeleteConfig(config)}
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
                    <FormLabel>配置名称 <span className="text-destructive">*</span></FormLabel>
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
                    <FormLabel>提供商 <span className="text-destructive">*</span></FormLabel>
                    <Select
                      onValueChange={(value) => {
                        field.onChange(value)
                        setSelectedApiType(value as ApiType)
                      }}
                      defaultValue={field.value}
                    >
                      <FormControl>
                        <SelectTrigger>
                          <SelectValue placeholder="选择提供商" />
                        </SelectTrigger>
                      </FormControl>
                      <SelectContent>
                        <SelectItem value="OPENAI">OpenAI</SelectItem>
                        <SelectItem value="AZURE_OPENAI">Azure OpenAI</SelectItem>
                        <SelectItem value="OLLAMA">Ollama (本地模型)</SelectItem>
                        <SelectItem value="CUSTOM">OpenAI 兼容 (第三方/中转)</SelectItem>
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
                    <FormLabel>模型名称 <span className="text-destructive">*</span></FormLabel>
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
                    <FormLabel>
                      Base URL {baseUrlRequired ? <span className="text-destructive">*</span> : '（选填）'}
                    </FormLabel>
                    <FormControl>
                      <Input placeholder={baseUrlMeta.placeholder} {...field} />
                    </FormControl>
                    <FormDescription>{baseUrlMeta.description}</FormDescription>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <FormField
                control={form.control}
                name="apiKey"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel>API Key <span className="text-destructive">*</span></FormLabel>
                    <FormControl>
                      <Input type="password" placeholder="sk-..." {...field} />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
              <DialogFooter className="gap-2 sm:gap-2">
                <Button
                  type="button"
                  variant="outline"
                  disabled={validateMutation.isPending || saveMutation.isPending}
                  onClick={handleTestConnection}
                >
                  {validateMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  <Zap className="mr-2 h-4 w-4" />
                  测试连接
                </Button>
                <Button type="submit" disabled={saveMutation.isPending || validateMutation.isPending}>
                  {saveMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
                  保存
                </Button>
              </DialogFooter>
            </form>
          </Form>
        </DialogContent>
      </Dialog>

      <AlertDialog
        open={pendingDeleteConfig !== null}
        onOpenChange={(open) => {
          if (deleteMutation.isPending) return
          if (!open) setPendingDeleteConfig(null)
        }}
      >
        <AlertDialogContent>
          <AlertDialogHeader>
            <AlertDialogTitle>确认删除配置？</AlertDialogTitle>
            <AlertDialogDescription>
              {pendingDeleteConfig
                ? `删除后将无法恢复，且会永久删除「${pendingDeleteConfig.configName}」。`
                : '删除后将无法恢复，且会永久删除该配置。'}
            </AlertDialogDescription>
          </AlertDialogHeader>
          <AlertDialogFooter>
            <AlertDialogCancel disabled={deleteMutation.isPending}>取消</AlertDialogCancel>
            <AlertDialogAction
              className="bg-destructive text-destructive-foreground hover:bg-destructive/90"
              disabled={deleteMutation.isPending}
              onClick={(event) => {
                event.preventDefault()
                if (!pendingDeleteConfig) return
                deleteMutation.mutate(pendingDeleteConfig.id)
              }}
            >
              {deleteMutation.isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              删除
            </AlertDialogAction>
          </AlertDialogFooter>
        </AlertDialogContent>
      </AlertDialog>
    </div>
  )
}
