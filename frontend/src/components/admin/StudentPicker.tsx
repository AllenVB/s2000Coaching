import { useMemo, useState } from 'react'

import { useMembers } from '@/api/adminMembersApi'
import { inputClasses } from '@/components/ui/FormField'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatMemberNumber } from '@/types/member'

/** A searchable list of students (name, e-mail, phone or #number) for the pages that work on one at a time. */
export function StudentPicker({ selectedId, onSelect }: { selectedId: string | undefined; onSelect: (id: string) => void }) {
  const { data: members, isLoading, error } = useMembers()
  useSignOutOn401(error)
  const [search, setSearch] = useState('')

  const visible = useMemo(() => {
    const term = search.trim().toLocaleLowerCase('tr').replace(/^#/, '')
    return (members ?? []).filter(
      (candidate) =>
        term === '' ||
        candidate.fullName.toLocaleLowerCase('tr').includes(term) ||
        candidate.email.toLocaleLowerCase('tr').includes(term) ||
        candidate.phone.includes(term) ||
        String(candidate.memberNumber).includes(term),
    )
  }, [members, search])

  return (
    <aside className="rounded-2xl border border-border bg-surface p-4 lg:sticky lg:top-24 lg:self-start">
      <input
        value={search}
        onChange={(event) => setSearch(event.target.value)}
        type="search"
        placeholder="Öğrenci ara: ad, e-posta, #numara…"
        aria-label="Öğrenci ara"
        className={inputClasses}
      />
      <ul className="mt-3 max-h-[60vh] space-y-1 overflow-y-auto">
        {isLoading ? <li className="p-2 text-xs text-text-muted">Yükleniyor…</li> : null}
        {visible.map((candidate) => (
          <li key={candidate.id}>
            <button
              type="button"
              onClick={() => onSelect(candidate.id)}
              aria-pressed={candidate.id === selectedId}
              className={`w-full rounded-xl px-3 py-2.5 text-left transition-colors ${
                candidate.id === selectedId ? 'bg-primary text-canvas' : 'hover:bg-white/5'
              }`}
            >
              <span className="block text-sm font-semibold">{candidate.fullName}</span>
              <span className={`block text-[11px] ${candidate.id === selectedId ? 'text-canvas/80' : 'text-text-muted'}`}>
                {formatMemberNumber(candidate.memberNumber)} · {candidate.email}
              </span>
            </button>
          </li>
        ))}
        {!isLoading && visible.length === 0 ? <li className="p-2 text-xs italic text-text-muted">Eşleşen öğrenci yok.</li> : null}
      </ul>
    </aside>
  )
}
