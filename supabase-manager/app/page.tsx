'use client'

import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useSearchParams } from 'next/navigation'
import { Suspense } from 'react'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { DatabaseManager } from '@/components/supabase-manager/database'
import {
  SheetNavigationProvider,
  useSheetNavigation,
} from '@/contexts/SheetNavigationContext'

const queryClient = new QueryClient()

function DatabaseContent({ projectRef, schema }: { projectRef: string; schema?: string }) {
  const { stack, popTo } = useSheetNavigation()
  const currentView = stack[stack.length - 1]

  return (
    <div className="flex flex-col h-screen overflow-hidden">
      {/* Breadcrumb header */}
      {stack.length > 1 && (
        <div className="flex items-center h-12 shrink-0 px-4 border-b">
          <Button
            variant="outline"
            size="icon"
            className="h-8 w-8"
            onClick={() => popTo(stack.length - 2)}
          >
            <ChevronLeft className="h-4 w-4" />
          </Button>
          <div className="ml-4 flex items-center gap-1.5 text-sm text-muted-foreground">
            {stack.map((item: { title: string }, index: number) => (
              <div key={`${item.title}-${index}`} className="flex items-center gap-1.5">
                {index > 0 && <ChevronRight className="h-3 w-3" />}
                {index === stack.length - 1 ? (
                  <span className="font-semibold text-foreground">{item.title}</span>
                ) : (
                  <button onClick={() => popTo(index)} className="hover:underline">
                    {item.title}
                  </button>
                )}
              </div>
            ))}
          </div>
        </div>
      )}
      <div className="grow overflow-y-auto">
        {currentView ? currentView.component : null}
      </div>
    </div>
  )
}

function ManagerContent() {
  const searchParams = useSearchParams()
  const projectRef = searchParams.get('ref') || ''
  const schema = searchParams.get('schema') || undefined

  return (
    <QueryClientProvider client={queryClient}>
      <SheetNavigationProvider
        onStackEmpty={() => {}}
        initialStack={[
          {
            title: 'Database',
            component: <DatabaseManager projectRef={projectRef} schemas={schema ? [schema] : undefined} />,
          },
        ]}
      >
        <DatabaseContent projectRef={projectRef} schema={schema} />
      </SheetNavigationProvider>
    </QueryClientProvider>
  )
}

export default function Page() {
  return (
    <Suspense fallback={<div className="flex items-center justify-center h-screen text-muted-foreground">Loading...</div>}>
      <ManagerContent />
    </Suspense>
  )
}
