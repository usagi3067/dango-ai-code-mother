'use client'

import { cn } from '@/lib/utils'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'

interface ResultsTableProps {
  data: any[]
  onRowClick?: (row: any) => void
  selectable?: boolean
  selectedIndices?: Set<number>
  onSelectionChange?: (indices: Set<number>) => void
}

export function ResultsTable({
  data,
  onRowClick,
  selectable,
  selectedIndices,
  onSelectionChange,
}: ResultsTableProps) {
  if (!data || data.length === 0) {
    return <p className="p-4 text-center">The query returned no results.</p>
  }

  const headers = Object.keys(data[0])
  const allSelected = selectedIndices?.size === data.length

  const toggleAll = () => {
    if (!onSelectionChange) return
    if (allSelected) {
      onSelectionChange(new Set())
    } else {
      onSelectionChange(new Set(data.map((_, i) => i)))
    }
  }

  const toggleRow = (index: number) => {
    if (!onSelectionChange || !selectedIndices) return
    const next = new Set(selectedIndices)
    if (next.has(index)) {
      next.delete(index)
    } else {
      next.add(index)
    }
    onSelectionChange(next)
  }

  return (
    <div className="overflow-auto">
      <Table>
        <TableHeader>
          <TableRow className="hover:bg-transparent">
            {selectable && (
              <TableHead className="w-10 pl-6">
                <input
                  type="checkbox"
                  checked={allSelected}
                  onChange={toggleAll}
                  className="rounded border-gray-300"
                />
              </TableHead>
            )}
            {headers.map((header) => (
              <TableHead
                className={cn(!selectable && 'first:pl-6 lg:first:pl-8', 'last:pr-6 lg:last:pr-8')}
                key={header}
              >
                {header}
              </TableHead>
            ))}
          </TableRow>
        </TableHeader>
        <TableBody>
          {data.map((row, rowIndex) => (
            <TableRow
              key={rowIndex}
              onClick={() => onRowClick?.(row)}
              className={cn(
                onRowClick && 'cursor-pointer hover:bg-muted/50 group',
                selectable && selectedIndices?.has(rowIndex) && 'bg-muted/30'
              )}
            >
              {selectable && (
                <TableCell className="w-10 pl-6">
                  <input
                    type="checkbox"
                    checked={selectedIndices?.has(rowIndex) || false}
                    onChange={(e) => {
                      e.stopPropagation()
                      toggleRow(rowIndex)
                    }}
                    onClick={(e) => e.stopPropagation()}
                    className="rounded border-gray-300"
                  />
                </TableCell>
              )}
              {headers.map((header) => (
                <TableCell
                  className={cn(
                    !selectable && 'first:pl-6 lg:first:pl-8',
                    'last:pr-6 lg:last:pr-8 text-xs text-muted-foreground group-hover:text-foreground min-w-[8rem]'
                  )}
                  key={`${rowIndex}-${header}`}
                >
                  <div className="text-xs font-mono w-fit max-w-96 truncate">
                    {JSON.stringify(row[header]).replace(/^"|"$/g, '')}
                  </div>
                </TableCell>
              ))}
            </TableRow>
          ))}
        </TableBody>
      </Table>
    </div>
  )
}
