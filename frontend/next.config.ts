import type { NextConfig } from 'next'

const nextConfig: NextConfig = {
  // API 请求代理到后端
  async rewrites() {
    return [
      {
        source: '/api/:path*',
        destination: `${process.env.BACKEND_URL || 'http://localhost:8080'}/api/:path*`,
      },
    ]
  },

  // React Compiler 优化
  reactCompiler: true,
}

export default nextConfig