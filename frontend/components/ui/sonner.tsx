"use client"

import {
  CircleCheckIcon,
  InfoIcon,
  Loader2Icon,
  OctagonXIcon,
  TriangleAlertIcon,
} from "lucide-react"
import { useTheme } from "next-themes"
import { Toaster as Sonner, type ToasterProps } from "sonner"

const Toaster = ({ toastOptions, richColors, style, ...props }: ToasterProps) => {
  const { theme = "system" } = useTheme()

  const mergedToastOptions: ToasterProps["toastOptions"] = {
    ...toastOptions,
    classNames: {
      toast:
        "group toast group-[.toaster]:border group-[.toaster]:shadow-lg group-[.toaster]:rounded-lg",
      title: "text-sm font-medium",
      description: "text-sm opacity-90",
      success:
        "group-[.toast]:bg-emerald-600 group-[.toast]:text-white group-[.toast]:border-emerald-700/50",
      info: "group-[.toast]:bg-blue-600 group-[.toast]:text-white group-[.toast]:border-blue-700/50",
      warning:
        "group-[.toast]:bg-amber-500 group-[.toast]:text-black group-[.toast]:border-amber-600/60",
      error:
        "group-[.toast]:bg-destructive group-[.toast]:text-white group-[.toast]:border-destructive/60",
      ...toastOptions?.classNames,
    },
  }

  return (
    <Sonner
      theme={theme as ToasterProps["theme"]}
      richColors={richColors ?? true}
      className="toaster group"
      icons={{
        success: <CircleCheckIcon className="size-4" />,
        info: <InfoIcon className="size-4" />,
        warning: <TriangleAlertIcon className="size-4" />,
        error: <OctagonXIcon className="size-4" />,
        loading: <Loader2Icon className="size-4 animate-spin" />,
      }}
      toastOptions={mergedToastOptions}
      style={
        {
          "--normal-bg": "var(--popover)",
          "--normal-text": "var(--popover-foreground)",
          "--normal-border": "var(--border)",
          "--border-radius": "var(--radius)",
          ...style,
        } as React.CSSProperties
      }
      {...props}
    />
  )
}

export { Toaster }
