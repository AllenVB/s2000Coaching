import { useMemo, useState } from 'react'
import { Link } from 'react-router-dom'

import { useMembers } from '@/api/adminMembersApi'
import { useAssignNutritionPlan, useMemberNutritionPlans, useNutritionTemplates } from '@/api/nutritionApi'
import { useAssignTrainingPlan, useMemberPlans, usePlanTemplates } from '@/api/trainingApi'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { inputClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatMemberNumber } from '@/types/member'

type Kind = 'antrenman' | 'beslenme'

/**
 * "Toplu Atama": one programme, many students. The source is a template or another student's programme;
 * each chosen student gets their own independent active copy, so editing one later never touches the rest.
 */
export function BulkAssignPage() {
  const [kind, setKind] = useState<Kind>('antrenman')
  const [sourceMemberId, setSourceMemberId] = useState('')
  const [planId, setPlanId] = useState('')
  const [chosen, setChosen] = useState<Set<string>>(new Set())
  const [search, setSearch] = useState('')
  const [confirming, setConfirming] = useState(false)
  const [done, setDone] = useState<number | null>(null)

  const { data: members, error } = useMembers()
  useSignOutOn401(error)
  const trainingTemplates = usePlanTemplates()
  const nutritionTemplates = useNutritionTemplates()
  const trainingOfMember = useMemberPlans(sourceMemberId)
  const nutritionOfMember = useMemberNutritionPlans(sourceMemberId)
  const assignTraining = useAssignTrainingPlan()
  const assignNutrition = useAssignNutritionPlan()
  const assign = kind === 'antrenman' ? assignTraining : assignNutrition

  const templates = (kind === 'antrenman' ? trainingTemplates.data : nutritionTemplates.data) ?? []
  const memberPlans = sourceMemberId ? ((kind === 'antrenman' ? trainingOfMember.data : nutritionOfMember.data) ?? []) : []
  const options = [
    ...templates.map((plan) => ({ id: plan.id, label: `Şablon · ${plan.name}` })),
    ...memberPlans.map((plan) => ({ id: plan.id, label: `${plan.name}${plan.active ? ' (aktif)' : ''}` })),
  ]
  const selected = options.find((option) => option.id === planId)

  const visible = useMemo(() => {
    const term = search.trim().toLocaleLowerCase('tr').replace(/^#/, '')
    return (members ?? []).filter(
      (member) =>
        member.id !== sourceMemberId &&
        (term === '' ||
          member.fullName.toLocaleLowerCase('tr').includes(term) ||
          member.email.toLocaleLowerCase('tr').includes(term) ||
          String(member.memberNumber).includes(term)),
    )
  }, [members, search, sourceMemberId])

  const toggle = (id: string) =>
    setChosen((current) => {
      const next = new Set(current)
      if (!next.delete(id)) next.add(id)
      return next
    })
  const allVisibleChosen = visible.length > 0 && visible.every((member) => chosen.has(member.id))
  const toggleAllVisible = () =>
    setChosen((current) => {
      const next = new Set(current)
      visible.forEach((member) => (allVisibleChosen ? next.delete(member.id) : next.add(member.id)))
      return next
    })

  const switchKind = (next: Kind) => {
    setKind(next)
    setPlanId('')
    setDone(null)
  }

  return (
    <>
      <PageHeading
        title="Toplu Atama"
        description="Bir programı birden çok öğrenciye tek seferde verin. Her öğrenci kendi bağımsız kopyasını alır ve bu kopya onun aktif programı olur; önceki aktif programı pasife düşer."
        action={
          <Link to="/yonetim/programlar" className="text-xs font-semibold uppercase tracking-wide text-primary hover:underline">
            ← Programlar
          </Link>
        }
      />

      <div className="space-y-6">
        <section className="rounded-2xl border border-border bg-surface p-5">
          <nav aria-label="Program türü" className="mb-4 inline-flex gap-1 rounded-full border border-border bg-canvas p-1">
            {(['antrenman', 'beslenme'] as const).map((value) => (
              <button
                key={value}
                type="button"
                onClick={() => switchKind(value)}
                className={`rounded-full px-5 py-2 text-xs font-semibold uppercase tracking-wide ${kind === value ? 'bg-primary text-canvas' : 'text-text-secondary hover:text-text-primary'}`}
              >
                {value === 'antrenman' ? 'Antrenman' : 'Beslenme'}
              </button>
            ))}
          </nav>

          <h2 className="mb-3 text-sm font-bold uppercase tracking-wider text-text-primary">1. Hangi program?</h2>
          <div className="grid gap-4 sm:grid-cols-2">
            <label className="text-xs font-medium text-text-secondary">
              Başka bir öğrencinin programından almak için öğrenci seçin (isteğe bağlı)
              <select
                value={sourceMemberId}
                onChange={(event) => {
                  setSourceMemberId(event.target.value)
                  setPlanId('')
                  setChosen((current) => {
                    const next = new Set(current)
                    next.delete(event.target.value)
                    return next
                  })
                }}
                className={`${inputClasses} mt-1`}
              >
                <option value="">— yalnızca şablonlar —</option>
                {(members ?? []).map((member) => (
                  <option key={member.id} value={member.id}>
                    {member.fullName} {formatMemberNumber(member.memberNumber)}
                  </option>
                ))}
              </select>
            </label>
            <label className="text-xs font-medium text-text-secondary">
              Program
              <select value={planId} onChange={(event) => { setPlanId(event.target.value); setDone(null) }} className={`${inputClasses} mt-1`}>
                <option value="">Seçin</option>
                {options.map((option) => (
                  <option key={option.id} value={option.id}>
                    {option.label}
                  </option>
                ))}
              </select>
            </label>
          </div>
        </section>

        <section className="rounded-2xl border border-border bg-surface p-5">
          <div className="mb-3 flex flex-wrap items-center justify-between gap-3">
            <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">2. Kimlere? ({chosen.size} seçili)</h2>
            <button type="button" onClick={toggleAllVisible} className="text-xs font-semibold uppercase tracking-wide text-primary hover:underline">
              {allVisibleChosen ? 'Görünenlerin seçimini kaldır' : 'Görünenlerin hepsini seç'}
            </button>
          </div>
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            type="search"
            placeholder="Öğrenci ara: ad, e-posta, #numara…"
            aria-label="Öğrenci ara"
            className={inputClasses}
          />
          <ul className="mt-3 grid max-h-[50vh] gap-1.5 overflow-y-auto sm:grid-cols-2">
            {visible.map((member) => (
              <li key={member.id}>
                <label className={`flex cursor-pointer items-center gap-2.5 rounded-lg border px-3 py-2 text-sm ${chosen.has(member.id) ? 'border-primary bg-primary/10' : 'border-border'}`}>
                  <input type="checkbox" checked={chosen.has(member.id)} onChange={() => toggle(member.id)} />
                  <span className="min-w-0 truncate text-text-primary">{member.fullName}</span>
                  <span className="ml-auto text-[11px] text-text-muted">{formatMemberNumber(member.memberNumber)}</span>
                </label>
              </li>
            ))}
          </ul>
        </section>

        {done !== null ? (
          <p role="status" className="rounded-xl border border-success/40 bg-success/10 p-3.5 text-sm text-success">
            {done} öğrenciye program atandı.
          </p>
        ) : null}
        {assign.isError ? <p role="alert" className="text-xs text-error">{assign.error.message}</p> : null}

        <button
          type="button"
          disabled={!planId || chosen.size === 0 || assign.isPending}
          onClick={() => setConfirming(true)}
          className="h-12 rounded-xl bg-primary px-6 text-sm font-bold uppercase tracking-wide text-canvas disabled:opacity-40"
        >
          Programı Ata
        </button>
      </div>

      {confirming ? (
        <ConfirmDialog
          title="Program atansın mı?"
          description={`“${selected?.label.replace('Şablon · ', '') ?? ''}” programı ${chosen.size} öğrenciye ayrı kopyalar olarak verilecek ve her birinin aktif ${kind} programı olacak. Önceki aktif programları pasife düşer.`}
          confirmLabel="Evet, Ata"
          isPending={assign.isPending}
          error={null}
          onCancel={() => setConfirming(false)}
          onConfirm={() =>
            assign.mutate(
              { planId, memberIds: [...chosen] },
              {
                onSuccess: (result) => {
                  setDone(result.assigned)
                  setChosen(new Set())
                },
                onSettled: () => setConfirming(false),
              },
            )
          }
        />
      ) : null}
    </>
  )
}
