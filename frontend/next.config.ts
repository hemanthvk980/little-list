import type { NextConfig } from 'next';

const config: NextConfig = {
  async rewrites() {
    const backend = process.env.BACKEND_URL || 'http://127.0.0.1:8080';
    return ['/api/:path*', '/oauth2/:path*', '/login/oauth2/:path*'].map(source => ({
      source, destination: backend + source
    }));
  },
  async headers() {
    return [{source: '/admin/:path*', headers: [{key:'Cache-Control',value:'no-store'},{key:'X-Robots-Tag',value:'noindex, nofollow'}]}, { source: '/:path*', headers: [
      { key: 'Referrer-Policy', value: 'no-referrer' },
      { key: 'X-Content-Type-Options', value: 'nosniff' }
    ] }];
  }
};
export default config;
