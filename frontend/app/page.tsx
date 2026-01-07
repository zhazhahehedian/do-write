'use client'

import { HeroSection } from "@/components/home/hero-section"
import { AuroraBackground } from "@/components/ui/aurora-background"

export default function LandingPage() {
  return (
    <AuroraBackground showRadialGradient={true}>
      <div className="relative w-full min-h-screen text-foreground font-sans overflow-x-hidden selection:bg-primary selection:text-primary-foreground z-10 flex flex-col">
        <HeroSection />
        
        <footer className="w-full py-6 text-center text-sm text-muted-foreground border-t border-border/10 bg-background/20 backdrop-blur-sm relative z-20 mt-auto">
          <div className="container mx-auto">
            &copy; {new Date().getFullYear()} do-write. All rights reserved.
          </div>
        </footer>
      </div>
    </AuroraBackground>
  )
}
