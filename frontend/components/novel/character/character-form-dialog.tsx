'use client'

import { useState, useEffect } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Textarea } from '@/components/ui/textarea'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { characterApi } from '@/lib/api/character'
import type { Character, RoleType } from '@/lib/types/character'
import { Loader2 } from 'lucide-react'
import { toast } from 'sonner'

interface CharacterFormDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  projectId: string
  character?: Character | null
  onSuccess?: () => void
}

const roleTypeOptions: { value: RoleType; label: string }[] = [
  { value: 'protagonist', label: '主角' },
  { value: 'supporting', label: '配角' },
  { value: 'antagonist', label: '反派' },
]

const genderOptions = [
  { value: '男', label: '男' },
  { value: '女', label: '女' },
  { value: '未知', label: '未知' },
]

interface CharacterFormData {
  name: string
  isOrganization: number
  roleType: RoleType
  age: string
  gender: string
  appearance: string
  personality: string
  background: string
  organizationType: string
  organizationPurpose: string
}

const initialFormData: CharacterFormData = {
  name: '',
  isOrganization: 0,
  roleType: 'supporting',
  age: '',
  gender: '',
  appearance: '',
  personality: '',
  background: '',
  organizationType: '',
  organizationPurpose: '',
}

export function CharacterFormDialog({
  open,
  onOpenChange,
  projectId,
  character,
  onSuccess,
}: CharacterFormDialogProps) {
  const queryClient = useQueryClient()
  const isEditing = !!character
  const [formData, setFormData] = useState<CharacterFormData>(initialFormData)
  const [activeTab, setActiveTab] = useState<'character' | 'organization'>('character')

  // Reset form when dialog opens/closes or character changes
  useEffect(() => {
    if (open) {
      if (character) {
        setFormData({
          name: character.name || '',
          isOrganization: character.isOrganization || 0,
          roleType: character.roleType || 'supporting',
          age: character.age?.toString() || '',
          gender: character.gender || '',
          appearance: character.appearance || '',
          personality: character.personality || '',
          background: character.background || '',
          organizationType: character.organizationType || '',
          organizationPurpose: character.organizationPurpose || '',
        })
        setActiveTab(character.isOrganization === 1 ? 'organization' : 'character')
      } else {
        setFormData(initialFormData)
        setActiveTab('character')
      }
    }
  }, [open, character])

  const createMutation = useMutation({
    mutationFn: (data: Partial<Character>) => characterApi.create(data),
    onSuccess: () => {
      toast.success('角色创建成功')
      queryClient.invalidateQueries({ queryKey: ['characters', projectId] })
      onOpenChange(false)
      onSuccess?.()
    },
    onError: (error: any) => {
      toast.error(error.message || '创建失败')
    },
  })

  const updateMutation = useMutation({
    mutationFn: (data: Partial<Character>) =>
      characterApi.update(character!.id.toString(), data),
    onSuccess: () => {
      toast.success('角色更新成功')
      queryClient.invalidateQueries({ queryKey: ['characters', projectId] })
      onOpenChange(false)
      onSuccess?.()
    },
    onError: (error: any) => {
      toast.error(error.message || '更新失败')
    },
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()

    if (!formData.name.trim()) {
      toast.error('请输入名称')
      return
    }

    const isOrg = activeTab === 'organization'
    const data: Partial<Character> = {
      projectId: Number(projectId),
      name: formData.name.trim(),
      isOrganization: isOrg ? 1 : 0,
    }

    if (isOrg) {
      data.organizationType = formData.organizationType || undefined
      data.organizationPurpose = formData.organizationPurpose || undefined
    } else {
      data.roleType = formData.roleType
      data.age = formData.age ? Number(formData.age) : undefined
      data.gender = formData.gender || undefined
      data.appearance = formData.appearance || undefined
      data.personality = formData.personality || undefined
      data.background = formData.background || undefined
    }

    if (isEditing) {
      updateMutation.mutate(data)
    } else {
      createMutation.mutate(data)
    }
  }

  const isPending = createMutation.isPending || updateMutation.isPending

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="sm:max-w-[600px] max-h-[85vh] overflow-y-auto">
        <DialogHeader>
          <DialogTitle>{isEditing ? '编辑角色' : '创建角色'}</DialogTitle>
          <DialogDescription>
            {isEditing ? '修改角色或组织的信息' : '创建新的角色或组织'}
          </DialogDescription>
        </DialogHeader>

        <form onSubmit={handleSubmit}>
          <Tabs value={activeTab} onValueChange={(v) => setActiveTab(v as 'character' | 'organization')}>
            <TabsList className="grid w-full grid-cols-2 mb-4">
              <TabsTrigger value="character">角色</TabsTrigger>
              <TabsTrigger value="organization">组织</TabsTrigger>
            </TabsList>

            <TabsContent value="character" className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="name">姓名 *</Label>
                  <Input
                    id="name"
                    value={formData.name}
                    onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                    placeholder="输入角色姓名"
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="roleType">角色类型</Label>
                  <Select
                    value={formData.roleType}
                    onValueChange={(value: RoleType) => setFormData(prev => ({ ...prev, roleType: value }))}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="选择类型" />
                    </SelectTrigger>
                    <SelectContent>
                      {roleTypeOptions.map(opt => (
                        <SelectItem key={opt.value} value={opt.value}>
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="age">年龄</Label>
                  <Input
                    id="age"
                    type="number"
                    value={formData.age}
                    onChange={(e) => setFormData(prev => ({ ...prev, age: e.target.value }))}
                    placeholder="输入年龄"
                    min={0}
                    max={999}
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="gender">性别</Label>
                  <Select
                    value={formData.gender}
                    onValueChange={(value) => setFormData(prev => ({ ...prev, gender: value }))}
                  >
                    <SelectTrigger>
                      <SelectValue placeholder="选择性别" />
                    </SelectTrigger>
                    <SelectContent>
                      {genderOptions.map(opt => (
                        <SelectItem key={opt.value} value={opt.value}>
                          {opt.label}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="appearance">外貌描述</Label>
                <Textarea
                  id="appearance"
                  value={formData.appearance}
                  onChange={(e) => setFormData(prev => ({ ...prev, appearance: e.target.value }))}
                  placeholder="描述角色的外貌特征"
                  rows={2}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="personality">性格特点</Label>
                <Textarea
                  id="personality"
                  value={formData.personality}
                  onChange={(e) => setFormData(prev => ({ ...prev, personality: e.target.value }))}
                  placeholder="描述角色的性格特点"
                  rows={2}
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="background">背景故事</Label>
                <Textarea
                  id="background"
                  value={formData.background}
                  onChange={(e) => setFormData(prev => ({ ...prev, background: e.target.value }))}
                  placeholder="描述角色的背景故事"
                  rows={3}
                />
              </div>
            </TabsContent>

            <TabsContent value="organization" className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="orgName">组织名称 *</Label>
                <Input
                  id="orgName"
                  value={formData.name}
                  onChange={(e) => setFormData(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="输入组织名称"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="organizationType">组织类型</Label>
                <Input
                  id="organizationType"
                  value={formData.organizationType}
                  onChange={(e) => setFormData(prev => ({ ...prev, organizationType: e.target.value }))}
                  placeholder="如：门派、公司、帮派等"
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="organizationPurpose">组织目的</Label>
                <Textarea
                  id="organizationPurpose"
                  value={formData.organizationPurpose}
                  onChange={(e) => setFormData(prev => ({ ...prev, organizationPurpose: e.target.value }))}
                  placeholder="描述组织的目标和宗旨"
                  rows={4}
                />
              </div>
            </TabsContent>
          </Tabs>

          <DialogFooter className="mt-6">
            <Button
              type="button"
              variant="outline"
              onClick={() => onOpenChange(false)}
              disabled={isPending}
            >
              取消
            </Button>
            <Button type="submit" disabled={isPending}>
              {isPending && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
              {isEditing ? '保存' : '创建'}
            </Button>
          </DialogFooter>
        </form>
      </DialogContent>
    </Dialog>
  )
}
