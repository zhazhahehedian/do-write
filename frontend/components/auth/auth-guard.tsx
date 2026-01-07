'use client'

import { useEffect, useState } from 'react'
import { useRouter, usePathname } from 'next/navigation'
import { useAuthStore } from '@/lib/store/auth-store'

export function AuthGuard({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const pathname = usePathname()
  const { isAuthenticated, token } = useAuthStore()
  const [isMounted, setIsMounted] = useState(false)

  useEffect(() => {
    setIsMounted(true)
  }, [])

  useEffect(() => {
    if (isMounted) {
      if (!isAuthenticated || !token) {
         router.push('/login')
      }
    }
  }, [isAuthenticated, token, router, isMounted])

  // Avoid hydration mismatch by rendering null until mounted
  if (!isMounted) {
    return null
  }

  // If not authenticated, render null (redirecting)
  if (!isAuthenticated || !token) {
    return null
  }

  return <>{children}</>
}
