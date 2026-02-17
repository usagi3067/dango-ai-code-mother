'use client'

import { useSearchParams } from 'next/navigation'
import { Suspense } from 'react'
import SupabaseManagerDialog from '@/components/supabase-manager'

function ManagerContent() {
  const searchParams = useSearchParams()
  const projectRef = searchParams.get('ref') || ''

  return (
    <div style={{ width: '100%', height: '100vh' }}>
      <SupabaseManagerDialog
        projectRef={projectRef}
        open={true}
        onOpenChange={() => {}}
        isMobile={false}
      />
    </div>
  )
}

export default function Page() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <ManagerContent />
    </Suspense>
  )
}
